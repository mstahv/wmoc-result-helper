package org.example;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.Route;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.orienteering.datastandard._3.EntryList;
import org.orienteering.datastandard._3.Iof3ResultList;
import org.orienteering.datastandard._3.Race;
import org.vaadin.firitin.components.DynamicFileDownloader;
import org.vaadin.firitin.components.RichText;
import org.vaadin.firitin.components.upload.UploadFileHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Route(layout = TopLayout.class)
public class QualificationRankingPointsView extends VerticalLayout {

    private final Pre preview = new Pre();
    private final DynamicFileDownloader download = new DynamicFileDownloader("Download CSV", "qualification_heats.csv", (OutputStream stream) -> {
        try {
            stream.write(preview.getText().getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    });
    private final Unmarshaller unmarshaller;
    private final Button calculatePoints;
    private EntryList entryList;
    private ArrayList<Iof3ResultList> previousYearResults = new ArrayList<>();

    Integer raceId;

    public QualificationRankingPointsView() {
        JAXBContext jaxbContext = null;
        try {
            jaxbContext = JAXBContext.newInstance(Iof3ResultList.class, EntryList.class);
            unmarshaller = jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }

        H3 preveartitle = new H3("Upload previous year's results (0)");
        add(preveartitle);

        UploadFileHandler prevYearResults = new UploadFileHandler((content, fileName, mimeType) -> {
            try {
                Iof3ResultList middleResults = (Iof3ResultList) unmarshaller.unmarshal(content);
                previousYearResults.add(middleResults);
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
            getUI().ifPresent(ui -> ui.access(() -> {
                preveartitle.setText("Upload previous year's results (" + previousYearResults.size() + ")");
                checkButtonValidity();
            }));
        }).allowMultiple();

        add(prevYearResults);

        add(new H3("Upload this year's entries"));

        UploadFileHandler startList = new UploadFileHandler((content, fileName, mimeType) -> {
            try {
                entryList = (EntryList) unmarshaller.unmarshal(content);
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
            getUI().ifPresent(ui -> ui.access(() -> {
                Select<Race> raceSelect = new Select<>();
                raceSelect.setItems(entryList.getEvent().getRace());
                raceSelect.setItemLabelGenerator(r -> r.getName());
                Dialog dialog = new Dialog(new H3("Choose race:"), raceSelect);
                raceSelect.addValueChangeListener(e -> {
                    raceId = e.getValue().getRaceNumber().intValue();
                    checkButtonValidity();
                    dialog.close();
                });
                dialog.open();
            }));
        });

        add(startList);

        calculatePoints = new Button("Calculate Points", e -> {
            Map<String, List<QualificationCompetitor>> stringListMap = RankingPointsService.calculatePoints(previousYearResults, entryList, raceId);
            StringBuilder sb = new StringBuilder();
            stringListMap.forEach((clazz, cList) -> {
                cList.forEach(c -> {
                    sb.append(clazz).append(";")
                            .append(c.iofId()).append(";")
                            .append(c.name().getGiven())
                            .append(" ")
                            .append(c.name().getFamily())
                            .append(";")
                            .append(c.points())
                            .append(";")
                            .append(c.nationality())
                            .append("\n");
                });
            });

            add(new H3("Start list:"));
            preview.setText(sb.toString());
            preview.setVisible(true);
            add(download,preview);

        });

        calculatePoints.setEnabled(false);
        add(calculatePoints);

    }

    private void checkButtonValidity() {
        if(raceId != null && previousYearResults.size() > 0) {
            calculatePoints.setEnabled(true);
        } else {
            calculatePoints.setEnabled(false);
        }
    }
}
