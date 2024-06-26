package org.orienteering.wmoc.resulthelperui;

import com.helger.commons.mutable.MutableInt;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.grid.ColumnPathRenderer;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.Route;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.orienteering.datastandard._3.EntryList;
import org.orienteering.datastandard._3.Iof3ResultList;
import org.orienteering.datastandard._3.PersonEntry;
import org.orienteering.wmoc.services.StartTimeService;
import org.vaadin.firitin.components.DynamicFileDownloader;
import org.vaadin.firitin.components.grid.VGrid;
import org.vaadin.firitin.components.orderedlayout.VHorizontalLayout;
import org.vaadin.firitin.components.upload.UploadFileHandler;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Route(layout = TopLayout.class)
public class SymmetricDifferenceView extends AbstractCalculatorView {

    private final Unmarshaller unmarshaller;
    private final Marshaller marshaller;
    private final StartTimeService startTimeService;
    private EntryList el;
    List<PersonEntry> entries = new ArrayList<>();
    VGrid<PersonEntry> entryGrid = new VGrid<>();

    public SymmetricDifferenceView(StartTimeService startTimeService) {
        this.startTimeService = startTimeService;
        add("Symmetric disjoint of entries (e.g. to find only those that have not paid). Uploading another file -> only uniques are left visible.");
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
            try {
                var xml = unmarshaller.unmarshal(content);

                if (xml instanceof EntryList el) {
                    this.el = el;
                }
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }

            return () -> {
                showReport();
                notifyErrors();
            };
        }).allowMultiple();



        add(new VHorizontalLayout(
                uploadFileHandler)
                .space()
                .withComponents(
                        new DynamicFileDownloader(out -> gridToCsv(out))
                )
        );
        addAndExpand(entryGrid);
        entryGrid.setMultiSort(true);
    }

    private void showReport() {
        if (el != null) {

            List<PersonEntry> personEntry = el.getPersonEntry();
            if(entries == null) {
                entries = personEntry;
            } else {
                personEntry.forEach(pe -> {
                    var existing = entries.stream().filter(current ->
                            current.getPerson().getId().get(0).getValue().equals(pe.getPerson().getId().get(0).getValue())
                    ).findFirst();
                    if(!existing.isPresent()) {
                        entries.add(pe);
                    } else {
                        entries.remove(existing.get());
                    }
                });
            }


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
            entryGrid.withColumnSelector();
            entryGrid.setItems(entries);
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

    private static final String DELIM = ";";

    private PrintWriter writer;

    private void cell(Object o) {
        writer.print(o);
        writer.print(DELIM);
    }

    private void nl() {
        writer.println();
    }



}
