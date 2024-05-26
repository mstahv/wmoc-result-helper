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
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Route(layout = TopLayout.class)
public class RegistrationVieverView extends AbstractCalculatorView {

    private final DynamicFileDownloader download = new DynamicFileDownloader("Download CSV", "finals.csv", (OutputStream stream) -> {
        try {
            stream.write("TODO".getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    });
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

        add(new VHorizontalLayout(
                uploadFileHandler,
                new VVerticalLayout(textFilter, sprint, forest).withSpacing(false).withPadding(false).withSizeUndefined(),
                services)
                .space()
                .withComponents(
                        new DynamicFileDownloader(out -> gridToCsv(out)))
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
        if (srl != null) {
            LinkedHashMap<Integer, Service> idToService = new LinkedHashMap<>();
            List<Service> services = el.getEvent().getService();
            // collect all services...
            for(Service s : services) {
                idToService.put(Integer.parseInt(s.getId().getValue()), s);
            }
            for (Service s : idToService.values()) {
                entryGrid.addColumn(pe -> {
                    Optional<PersonServiceRequest> personServiceRequest = getPersonServiceRequest(pe);
                    if (personServiceRequest.isPresent()) {
                        return personServiceRequest.get().getServiceRequest().stream().filter(sr -> sr.getService().getId().getValue().equals(s.getId().getValue()))
                                .findFirst().isPresent() ? "X" : "";
                    }
                    return "";
                }).setHeader(s.getName().get(0).getValue());
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
                if (pe.getControlCard().isEmpty()) {
                    // TODO check for rented/reserved from other XML file
                    if (emitReservation(pe) == EmitReservation.RENTAL) {
                        span.setText("rental");
                    } else {
                        span.setText("--not known--");
                        span.getStyle().setColor("darkred");
                    }
                } else {
                    int parsed = Integer.parseInt(pe.getControlCard().get(0).getValue());
                    if (!(parsed > 10000 && parsed < 600000)) {
                        span.getStyle().setColor("red");
                    }
                    span.setText(parsed + "");
                }
                return span;
            })).setHeader("ControlCard").setSortable(true);
            entryGrid.addColumn(pe -> {
                return pe.getRaceNumber().contains(1) ? "X" : "";
            }).setHeader("Sprint").setSortable(true);
            Grid.Column<PersonEntry> forest = entryGrid.addColumn(pe -> {
                return pe.getRaceNumber().contains(3) ? "X" : "";
            }).setHeader("Forest").setSortable(true);
            entryGrid.addColumn(pe -> pe.getPerson().getNationality().getValue())
                    .setHeader("Nationality");

            entryGrid.getColumns().forEach(c -> c.setAutoWidth(true));

            if (srl != null) {
                addServiceColumns();
            }
            filterReport();
            entryGrid.withColumnSelector();

        }

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
                        String cCard = "--not known--";
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

    enum EmitReservation {
        UNKNOWN, RENTAL, WMOCPURCHACE
    }



}
