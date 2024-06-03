package org.orienteering.wmoc.resulthelperui;

import com.helger.commons.mutable.MutableInt;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.ColumnPathRenderer;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.Route;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbookType;
import org.orienteering.datastandard._3.ControlCard;
import org.orienteering.datastandard._3.EntryList;
import org.orienteering.datastandard._3.Iof3ResultList;
import org.orienteering.datastandard._3.PersonEntry;
import org.orienteering.datastandard._3.PersonServiceRequest;
import org.orienteering.datastandard._3.Service;
import org.orienteering.datastandard._3.ServiceRequest;
import org.orienteering.datastandard._3.ServiceRequestList;
import org.orienteering.wmoc.services.StartTimeService;
import org.vaadin.firitin.components.DynamicFileDownloader;
import org.vaadin.firitin.components.checkbox.VCheckBox;
import org.vaadin.firitin.components.grid.VGrid;
import org.vaadin.firitin.components.listbox.VMultiSelectListBox;
import org.vaadin.firitin.components.orderedlayout.VHorizontalLayout;
import org.vaadin.firitin.components.orderedlayout.VVerticalLayout;
import org.vaadin.firitin.components.textfield.VTextField;
import org.vaadin.firitin.components.upload.UploadFileHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Route(layout = TopLayout.class)
public class RegistrationVieverView extends AbstractCalculatorView {

    static Collection<String> groups = Arrays.asList(
            "X Nordahl",
            "Jens Kristian Kopland",
            "Leif Morkfors",
            "Aller Travel",
            "Robert Zdrahal",
            "Urmas Sulaoja",
            "Ulrich Aeschlimann",
            "Laia Santamaria Ortega",
            "Thorolf Trolsrud",
            "Joli Wehrli",
            "Jozef Pollak",
            "Margus Sarap"
    );

    private final Unmarshaller unmarshaller;
    private final Marshaller marshaller;
    private final StartTimeService startTimeService;
    VGrid<PersonEntry> entryGrid = new VGrid<>();
    Map<PersonEntry, EmitReservation> emitResCache = new HashMap<>();
    Map<PersonEntry, Optional<PersonServiceRequest>> personServiceRequestMap = new HashMap<>();
    Div count = new Div();
    MultiSelectListBox<Service> services = new VMultiSelectListBox();
    Checkbox sprint = new VCheckBox("Sprint").withValueChangeListener(e -> filterReport());
    private TextField textFilter = new VTextField()
            .withWidth("250px")
            .withClearButtonVisible(true)
            .withPlaceholder("Freetext filters, ; for AND")
            .withValueChangeListener(e -> filterReport());
    Checkbox forest = new VCheckBox("Forest").withValueChangeListener(e -> filterReport());
    private ServiceRequestList srl;
    record ServiceOrder(int amout, double balance) {}
    private Map<String,Map<String,ServiceOrder>> iofIdToServiceToOrder= new LinkedHashMap<>();
    private Map<String,String> iofIdToTour= new HashMap<>();

    private EntryList el;
    public RegistrationVieverView(StartTimeService startTimeService) {
        this.startTimeService = startTimeService;
        add("View & export entries & services in IOF XML format, upload at least entries, more data with services xml. Start times available for %s races".formatted(startTimeService.getRaceCount()));
        JAXBContext jaxbContext = null;
        try {
            jaxbContext = JAXBContext.newInstance(Iof3ResultList.class, EntryList.class);
            unmarshaller = jaxbContext.createUnmarshaller();

            marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }

        UploadFileHandler uploadFileHandler = new UploadFileHandler((content, m) -> {
            String mimeType = m.mimeType();

            if("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equals(mimeType)) {
                iofIdToServiceToOrder.clear();
                Workbook wb = new XSSFWorkbook(content);
                // First collect special "Group" column if signed up by a tour operator
                Sheet entries = wb.getSheet("Entries");
                int row = 1;
                int lastRowNum = entries.getLastRowNum();
                while(row < lastRowNum) {
                    Row entriesRow = entries.getRow(row);
                    int iofID = (int) entriesRow.getCell(0).getNumericCellValue();
                    String enteredBy = entriesRow.getCell(33).getStringCellValue();
                    if(groups.contains(enteredBy)) {
                        iofIdToTour.put(""+iofID, enteredBy);
                    }
                    row++;
                }
                // Then actual service orders
                entries = wb.getSheet("Service orders");
                row = 1;
                lastRowNum = entries.getLastRowNum();
                while(row < lastRowNum) {
                    Row entriesRow = entries.getRow(row);
                    int iofID = (int) entriesRow.getCell(0).getNumericCellValue();
                    double balance = 0;
                    if(iofID != 0) {
                        String status = entriesRow.getCell(18).getStringCellValue();
                        String serviceName = entriesRow.getCell(14).getStringCellValue();
                        int orders = (int) entriesRow.getCell(15).getNumericCellValue();
                        if("Not paid".equals(status)) {
                            double amount = entriesRow.getCell(16).getNumericCellValue();
                            double paid = entriesRow.getCell(19).getNumericCellValue();
                            balance = paid - amount;
                        }
                        if(orders > 0) {
                            iofIdToServiceToOrder.computeIfAbsent(""+iofID, id -> new HashMap<>())
                                    .put(serviceName, new ServiceOrder(orders, balance));
                        }
                    }
                    row++;
                }

                return () -> {
                    showReport();
                    notifyErrors();
                    Notification.show("Loaded entries from Eventor Excel file");
                };
            }

            try {
                var xml = unmarshaller.unmarshal(content);

                if (xml instanceof EntryList el) {
                    this.el = el;
                } else if (xml instanceof ServiceRequestList srl) {
                    this.srl = srl;
                    emitResCache.clear();
                    personServiceRequestMap.clear();
                }
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }

            return () -> {
                showReport();
                notifyErrors();
            };
        }).allowMultiple();

        services.setHeight("100px");
        services.setItemLabelGenerator(s -> s.getName().get(0).getValue());

        var excell = new DynamicFileDownloader(out -> gridToXls(out))
                .withContentTypeGenerator((DynamicFileDownloader.ContentTypeGenerator) () ->
                        XSSFWorkbookType.XLSX.getContentType())
                .withFileNameGenerator((DynamicFileDownloader.FileNameGenerator) request -> "bibprinting."+ XSSFWorkbookType.XLSX.getExtension());
        excell.asButton().getButton().setIcon(VaadinIcon.DOWNLOAD_ALT.create());
        excell.getButton().setTooltipText("Excel file for bibs & envelopes, needs all data to succeed!");

        add(new VHorizontalLayout(
                uploadFileHandler,
                new VVerticalLayout(textFilter, sprint, forest).withSpacing(false).withPadding(false).withSizeUndefined(),
                services)
                .space()
                .withComponents(
                        new DynamicFileDownloader(out -> gridToCsv(out)),
                        excell
                )
        );
        addAndExpand(entryGrid);
        add(count);

        entryGrid.setMultiSort(true);
        entryGrid.asSingleSelect().addValueChangeListener(e -> {
            PersonEntry personEntry = e.getValue();
            if (personEntry == null) {
                return;
            }
            // TODO figure out a good way to show
            Dialog dialog = new Dialog("Details for " + personEntry.getId().getValue());

            StringWriter stringWriter = new StringWriter();
            try {
                marshaller.marshal(personEntry, stringWriter);

                srl.getPersonServiceRequest().stream()
                        .filter(psr -> psr.getPerson().idEquals(personEntry.getPerson()))
                        .forEach(psr -> {
                            try {
                                marshaller.marshal(psr, stringWriter);
                            } catch (JAXBException ex) {
                                throw new RuntimeException(ex);
                            }
                        });

            } catch (JAXBException ex) {
                throw new RuntimeException(ex);
            }
            Pre pre = new Pre();
            pre.setText(stringWriter.toString());
            dialog.add(pre);

            dialog.open();
        });

        services.addValueChangeListener(e -> {
            if (e.isFromClient()) {
                filterReport();
            }
        });

    }

    private void addServiceColumns() {
        if (serviceOrdersAvailable()) {
            LinkedHashMap<Integer, Service> idToService = new LinkedHashMap<>();
            List<Service> services = el.getEvent().getService();
            // collect all services...
            for(Service s : services) {
                idToService.put(Integer.parseInt(s.getId().getValue()), s);
            }

            if(!iofIdToServiceToOrder.isEmpty()) {
                entryGrid.addColumn(pe -> {
                    String iofId = pe.getPerson().getId().get(0).getValue();
                    return iofIdToTour.getOrDefault(iofId, "");
                }).setHeader("Group");
                entryGrid.addColumn(pe -> {
                    Map<String, ServiceOrder> orderMap = iofIdToServiceToOrder.get(pe.getPerson().getId().get(0).getValue());
                    if(orderMap != null) {
                        return orderMap.entrySet().stream()
                                .map(e -> e.getValue().amout() + " " + e.getKey())
                                .collect(Collectors.joining("|"));
                    }
                    return "";
                }).setHeader("Service listing");
                entryGrid.addColumn(pe -> {
                    Map<String, ServiceOrder> orderMap = iofIdToServiceToOrder.get(pe.getPerson().getId().get(0).getValue());
                    if(orderMap != null) {
                        double sum = orderMap.values().stream().mapToDouble(so -> so.balance()).sum();
                        if(sum != 0) {
                            return sum;
                        } else {
                            return "";
                        }
                    }
                    return "";
                }).setHeader("Service balance");

            }

            for (Service s : idToService.values()) {
                if(!iofIdToServiceToOrder.isEmpty()) {
                    entryGrid.addColumn(pe -> {
                        Map<String, ServiceOrder> orderMap = iofIdToServiceToOrder.get(pe.getPerson().getId().get(0).getValue());
                        if(orderMap != null) {
                            ServiceOrder serviceOrder = orderMap.get(s.getName().get(0).getValue());
                            if(serviceOrder != null) {
                                return serviceOrder.amout();
                            }
                        }
                        return "";
                    }).setHeader(s.getName().get(0).getValue());
                } else {
                    entryGrid.addColumn(pe -> {
                        Optional<PersonServiceRequest> personServiceRequest = getPersonServiceRequest(pe);

                        if (personServiceRequest.isPresent()) {
                            double counted = personServiceRequest.get().getServiceRequest().stream().filter(sr -> sr.getService().getId().getValue().equals(s.getId().getValue()))
                                    .mapToInt(sr -> (int) sr.getRequestedQuantity()).sum();
                            if(counted == 0) {
                                return "";
                            }
                            return counted;
                        }
                        return "";
                    }).setHeader(s.getName().get(0).getValue());
                }
            }

            this.services.setItems(idToService.values());
        }
    }

    private EmitReservation emitReservation(PersonEntry personEntry) {
        if (srl != null) {
            return emitResCache.computeIfAbsent(personEntry, pe -> {
                Optional<PersonServiceRequest> services = getPersonServiceRequest(pe);
                if (services.isPresent()) {
                    Optional<ServiceRequest> rental = services.get().getServiceRequest().stream()
                            .filter(sr -> sr.getService().getId().getValue().equals("893") || sr.getService().getId().getValue().equals("1078"))
                            .findFirst();
                    if (rental.isPresent()) {
                        return EmitReservation.RENTAL;
                    }
                    // TODO Do we now know those who plan to purchase 🤔
                }
                return EmitReservation.UNKNOWN;
            });
        }
        if(!iofIdToServiceToOrder.isEmpty()) {
            String iofId = personEntry.getPerson().getId().get(0).getValue();
            Map<String, ServiceOrder> orderMap = iofIdToServiceToOrder.get(iofId);
            if(orderMap != null) {
                return orderMap.containsKey("Emit card (rental)") ? EmitReservation.RENTAL : EmitReservation.UNKNOWN;
            }
        }
        return EmitReservation.UNKNOWN;
    }

    private Optional<PersonServiceRequest> getPersonServiceRequest(PersonEntry pe) {
        return personServiceRequestMap.computeIfAbsent(pe, p -> srl.getPersonServiceRequest().stream()
                .filter(psr -> {
                    return p.getPerson().idEquals(psr.getPerson());
                })
                .findFirst());
    }

    private void showReport() {
        if (el != null) {
            entryGrid.removeAllColumns();
            entryGrid.addColumn(pe -> pe.getPerson().getId().get(0).getValue()).setHeader("IOF Id");
            if(startTimeService.getRaceCount() > 0) {
                // expects bibs calculated if start times available
                entryGrid.addColumn(pe -> pe.getId().getValue()).setHeader("Bib");
                // Sorry for further maintainers, this section is hardcoded, might not match in the future
                final int SQ = 1;
                final int FQ = 3;
                entryGrid.addColumn(pe -> {
                    String iofId = pe.getPerson().getId().get(0).getValue();
                    return startTimeService.getStartTime(SQ, iofId);
                }).setHeader("Start SQ");
                entryGrid.addColumn(pe -> {
                    String iofId = pe.getPerson().getId().get(0).getValue();
                    return startTimeService.getStartTime(FQ, iofId);
                }).setHeader("Start FQ");
            }
            entryGrid.addColumn(pe -> {
                return pe.getClazz().get(0).getName();
            }).setHeader("Class").setSortable(true);
            entryGrid.addColumn(pe -> {
                return pe.getPerson().getName().getGiven() + " " + pe.getPerson().getName().getFamily();
            }).setHeader("Name");
            entryGrid.addColumn(pe -> {
                if (pe.getControlCard().isEmpty()) {
                    if (emitReservation(pe) == EmitReservation.RENTAL) {
                        return -1;
                    }
                    return -2;
                }
                ControlCard controlCard = pe.getControlCard().get(0);
                return Integer.parseInt(controlCard.getValue());
            }).setRenderer(new ComponentRenderer<>(pe -> {
                Span span = new Span("");
                EmitReservation emitReservation = emitReservation(pe);
                if (pe.getControlCard().isEmpty()) {
                    // TODO check for rented/reserved from other XML file
                    if (emitReservation == EmitReservation.RENTAL) {
                        // span.setText("rental");
                    } else {
                        span.setText("");
                        //span.getStyle().setColor("darkred");
                    }
                } else {
                    if(emitReservation == EmitReservation.RENTAL) {
                        // Assume emit rented, but some old/wrong value given
                    } else {
                        int parsed = Integer.parseInt(pe.getControlCard().get(0).getValue());
                        if (!(parsed > 10000 && parsed < 600000)) {
                            span.getStyle().setColor("red");
                        }
                        span.setText(parsed + "");
                    }
                }
                return span;
            })).setHeader("ControlCard").setSortable(true);
            entryGrid.addColumn(pe -> {
                return pe.getRaceNumber().contains(1) ? "1" : "";
            }).setHeader("Sprint").setSortable(true);
            Grid.Column<PersonEntry> forest = entryGrid.addColumn(pe -> {
                return pe.getRaceNumber().contains(3) ? "1" : "";
            }).setHeader("Forest").setSortable(true);
            // Switched to code, for some reason, some long names are in Swedish, thank you eventor...
            entryGrid.addColumn(pe -> pe.getPerson().getNationality().getCode())
                    .setHeader("Nationality");

            entryGrid.getColumns().forEach(c -> c.setAutoWidth(true));

            if (serviceOrdersAvailable()) {
                addServiceColumns();
            }
            filterReport();
            entryGrid.withColumnSelector();

        }

    }

    private boolean serviceOrdersAvailable() {
        return srl != null || !iofIdToServiceToOrder.isEmpty();
    }

    private void filterReport() {
        if (el != null) {
            List<PersonEntry> personEntry = new ArrayList<>(el.getPersonEntry());
            if (!textFilter.isEmpty()) {
                String[] filters = textFilter.getValue().split(";");
                personEntry = personEntry.stream().filter(pe -> {
                    for(int i = 0; i < filters.length; i++) {
                        String f = filters[i];
                        if (pe.getPerson().getId().get(0).getValue().contains(f)) {
                            continue;
                        }
                        if (pe.getPerson().getName().getFamily().contains(f)) {
                            continue;
                        }
                        if (pe.getPerson().getName().getGiven().contains(f)) {
                            continue;
                        }
                        if (pe.getClazz().get(0).getName().contains(f)) {
                            continue;
                        }
                        if (pe.getPerson().getNationality().getValue().contains(f)) {
                            continue;
                        }
                        String cCard = "not known";
                        if (!pe.getControlCard().isEmpty()) {
                            cCard = pe.getControlCard().get(0).getValue();
                        }
                        if (cCard.contains(f)) {
                            continue;
                        }
                        return false;
                    }
                    return true;
                }).toList();
            }
            if (!services.getValue().isEmpty()) {
                for (Service s : services.getValue()) {
                    personEntry = personEntry.stream().filter(pe -> {
                        Optional<PersonServiceRequest> psr = getPersonServiceRequest(pe);
                        if (psr.isPresent()) {
                            return psr.get().findByService(s).isPresent();
                        }
                        return false;
                    }).toList();
                }
            }
            if(sprint.getValue()) {
                personEntry = personEntry.stream().filter(pe -> {
                    return pe.getRaceNumber().contains(1);
                }).toList();
            }
            if(forest.getValue()) {
                personEntry = personEntry.stream().filter(pe -> {
                    return pe.getRaceNumber().contains(3);
                }).toList();
            }
            entryGrid.setItems(personEntry);
            count.setText("" + personEntry.size() + " rows");
        }
    }

    private void gridToCsv(OutputStream outputStream) {
        final String DELIM = ";";
        PrintStream out = new PrintStream(outputStream);

        MutableInt idx = new MutableInt(0);
        entryGrid.getColumns().forEach(c -> {
            if (c.isVisible()) {
                String headerText = c.getHeaderText();
                if (headerText != null) {
                    out.print(headerText);
                } else if (c.getKey() != null) {
                    out.print(c.getKey());
                } else {
                    out.print(idx.intValue());
                }
                out.print(DELIM);
            }
        });
        out.println();
        entryGrid.getGenericDataView().getItems().forEach(
                i -> {
                    entryGrid.getColumns().forEach(c -> {
                        if (c.isVisible()) {
                            Renderer<PersonEntry> renderer = c.getRenderer();
                            if (renderer instanceof ColumnPathRenderer<PersonEntry> cpr) {
                                try {
                                    Field field = ColumnPathRenderer.class.getDeclaredField("provider");
                                    field.setAccessible(true);
                                    ValueProvider vp = (ValueProvider) field.get(cpr);
                                    Object object = vp.apply(i);
                                    out.print(object.toString());
                                } catch (NoSuchFieldException e) {
                                    throw new RuntimeException(e);
                                } catch (IllegalAccessException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            if (renderer instanceof ComponentRenderer cr) {
                                Component component = cr.createComponent(i);
                                if (component instanceof HasText ht) {
                                    out.print(ht.getText());
                                } else {
                                    out.print(component.toString());
                                }
                            }
                            out.print(DELIM);
                        }

                    });
                    out.println();
                }
        );

    }

    private void gridToXls(OutputStream outputStream) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFCellStyle multiline = workbook.createCellStyle();
        multiline.setWrapText(true);
        XSSFSheet sheet = workbook.createSheet();
        XSSFRow header = sheet.createRow(0);
        header.createCell(0).setCellValue("IOF Id");
        header.createCell(1).setCellValue("Bib");
        header.createCell(2).setCellValue("SQ");
        header.createCell(3).setCellValue("Start SQ");
        header.createCell(4).setCellValue("FQ");
        header.createCell(5).setCellValue("Start FQ");
        header.createCell(6).setCellValue("Class");
        header.createCell(7).setCellValue("Name");
        header.createCell(8).setCellValue("ControlCard");
        header.createCell(9).setCellValue("Nationality");
        header.createCell(10).setCellValue("Group");
        header.createCell(11).setCellValue("Service listing");
        header.createCell(12).setCellValue("Service balance");

        MutableInt idx = new MutableInt(1);
        entryGrid.getGenericDataView().getItems().forEach(
                pe -> {
                    XSSFRow row = sheet.createRow(idx.intValue());
                    String iofId = pe.getPerson().getId().get(0).getValue();
                    row.createCell(0).setCellValue(iofId);
                    row.createCell(1).setCellValue(pe.getId().getValue());
                    // Sorry for further maintainers, this section is hardcoded, might not match in the future
                    final int SQ = 1;
                    final int FQ = 3;
                    StartTimeService.StartInfo ss = startTimeService.getStartTime(SQ, iofId);
                    row.createCell(2).setCellValue(ss == null ? "" : ss.clazz());
                    row.createCell(3).setCellValue(ss == null ? "" : ss.time().toString());
                    StartTimeService.StartInfo sf = startTimeService.getStartTime(FQ, iofId);
                    row.createCell(4).setCellValue(sf == null ? "" : sf.clazz());
                    row.createCell(5).setCellValue(sf == null ? "" : sf.time().toString());
                    row.createCell(6).setCellValue(pe.getClazz().get(0).getName());
                    row.createCell(7).setCellValue(pe.getPerson().getName().getGiven() + " " + pe.getPerson().getName().getFamily());
                    String controlCard = "";
                    if (!pe.getControlCard().isEmpty()) {
                        EmitReservation emitReservation = emitReservation(pe);
                        if(emitReservation == EmitReservation.RENTAL) {
                            // Assume emit rented, but some old/wrong value given
                        } else {
                            int parsed = Integer.parseInt(pe.getControlCard().get(0).getValue());
                            controlCard = "" + parsed;
                        }
                    }
                    row.createCell(8).setCellValue(controlCard);
                    row.createCell(9).setCellValue(pe.getPerson().getNationality().getCode());
                    row.createCell(10).setCellValue(iofIdToTour.getOrDefault(iofId, ""));
                    String services = "";
                    Map<String, ServiceOrder> orderMap = iofIdToServiceToOrder.get(pe.getPerson().getId().get(0).getValue());
                    if(orderMap != null) {
                        services = orderMap.entrySet().stream()
                                .map(e -> e.getValue().amout() + " " + e.getKey())
                                .collect(Collectors.joining("\n"));
                        row.setHeightInPoints(orderMap.size()*sheet.getDefaultRowHeightInPoints());
                    }
                    XSSFCell cell = row.createCell(11);
                    cell.setCellStyle(multiline);
                    cell.setCellValue(services);
                    String balance = "";
                    if(orderMap != null) {
                        double sum = orderMap.values().stream().mapToDouble(so -> so.balance()).sum();
                        if(sum != 0) {
                            balance = "" + sum + " €";
                        }
                    }
                    row.createCell(12).setCellValue(balance);
                    idx.inc();
                }
        );

        for (int i = 0; i < 13; i++) {
            sheet.autoSizeColumn(i);
        }

        try {
            workbook.write(outputStream);
            outputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    enum EmitReservation {
        UNKNOWN, RENTAL, WMOCPURCHACE
    }



}
