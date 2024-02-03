package org.orienteering.wmoc.resulthelperui;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.orienteering.datastandard._3.ControlCard;
import org.orienteering.datastandard._3.EntryList;
import org.orienteering.datastandard._3.Iof3ResultList;
import org.orienteering.datastandard._3.PersonEntry;
import org.orienteering.datastandard._3.PersonServiceRequest;
import org.orienteering.datastandard._3.ServiceRequest;
import org.orienteering.datastandard._3.ServiceRequestList;
import org.vaadin.firitin.components.DynamicFileDownloader;
import org.vaadin.firitin.components.upload.UploadFileHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.HashMap;
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
    Grid<PersonEntry> entryGrid = new Grid<>();
    Map<PersonEntry, EmitReservation> emitResCache = new HashMap<>();
    private ServiceRequestList srl;
    private EntryList el;

    public RegistrationVieverView() {
        add("View & export entries & services in IOF XML format, upload at least entries, more data with services xml.");
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
                }
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }

            return () -> {
                showReport();
                notifyErrors();
            };
        }).allowMultiple();

        add(uploadFileHandler);

        entryGrid.setMultiSort(true);

        entryGrid.addColumn(pe -> pe.getPerson().getId().get(0).getValue()).setHeader("IOF Id");
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
                if ((parsed > 10000 && parsed < 300000)) {
                    span.getStyle().setColor("darkcyan");
                } else if (!(parsed > 300000 && parsed < 600000)) {
                    span.getStyle().setColor("red");
                }
                span.setText(parsed + "");
            }
            return span;
        })).setHeader("ControlCard").setSortable(true);
        entryGrid.addColumn(pe -> {
            return String.join(",", pe.getRaceNumber().stream().map(i -> i.toString()).toList());
        }).setHeader("Races").setSortable(true);
        entryGrid.addColumn(pe -> {
            return "";
        }).setHeader("");
        entryGrid.addColumn(pe -> {
            return "";
        }).setHeader("");
        addAndExpand(entryGrid);

        entryGrid.asSingleSelect().addValueChangeListener(e -> {
            PersonEntry personEntry = e.getValue();
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

    }

    private EmitReservation emitReservation(PersonEntry personEntry) {
        if (srl != null) {
            return emitResCache.computeIfAbsent(personEntry, pe -> {
                Optional<PersonServiceRequest> services = srl.getPersonServiceRequest().stream()
                        .filter(psr -> {
                            return pe.getPerson().idEquals(psr.getPerson());
                        })
                        .findFirst();
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

    private void showReport() {
        if (el != null) {
            List<PersonEntry> personEntry = el.getPersonEntry();
            entryGrid.setItems(personEntry);
        }

    }

    enum EmitReservation {
        UNKNOWN, RENTAL, WMOCPURCHACE
    }

}
