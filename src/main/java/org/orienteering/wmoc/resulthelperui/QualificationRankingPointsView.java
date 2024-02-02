package org.orienteering.wmoc.resulthelperui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.Route;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.orienteering.datastandard._3.EntryList;
import org.orienteering.datastandard._3.Iof3ResultList;
import org.orienteering.datastandard._3.Race;
import org.orienteering.wmoc.domain.QualificationCompetitor;
import org.orienteering.wmoc.domain.planner.Clazz;
import org.orienteering.wmoc.domain.planner.Start;
import org.orienteering.wmoc.domain.planner.StartTimePlan;
import org.orienteering.wmoc.resulthelperui.planner.PlanSelect;
import org.orienteering.wmoc.services.RankingPointsService;
import org.vaadin.firitin.components.DynamicFileDownloader;
import org.vaadin.firitin.components.upload.UploadFileHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QualificationRankingPointsView extends AbstractCalculatorView {

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
    Integer raceId;
    private EntryList entryList;
    private ArrayList<Iof3ResultList> previousYearResults = new ArrayList<>();

    public QualificationRankingPointsView(PlanSelect planSelect) {
        add("This view calculates ranking points from n last year results, and generates even qualification heats based on them and countries.");

        add(planSelect);

        JAXBContext jaxbContext = null;
        try {
            jaxbContext = JAXBContext.newInstance(Iof3ResultList.class, EntryList.class);
            unmarshaller = jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }

        H3 preveartitle = new H3("Upload at least 1 previous year's results (Currently read: 0)");
        add(preveartitle);

        UploadFileHandler prevYearResults = new UploadFileHandler((content, fileName, mimeType) -> {
            try {
                Iof3ResultList middleResults = (Iof3ResultList) unmarshaller.unmarshal(content);
                previousYearResults.add(middleResults);
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
            getUI().ifPresent(ui -> ui.access(() -> {
                preveartitle.setText("Upload at least 1 previous year's results (Currently read:" + previousYearResults.size() + ")");
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
        }).withClearAutomatically(false);

        add(startList);

        calculatePoints = new Button("Calculate Points", e -> {
            Map<String, List<QualificationCompetitor>> classToRunners = RankingPointsService.calculatePoints(previousYearResults, entryList, raceId);
            StringBuilder sb = new StringBuilder();

            StartTimePlan plan = planSelect.getValue();
            if (plan == null) {
                Notification.show("No, plan selected, just showring runners");
                classToRunners.forEach((clazz, cList) -> {
                    cList.forEach(c -> {
                        sb.append(clazz)
                                .append(";")
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
            } else {
                sb.append("Starttime");
                sb.append(";");
                sb.append("Class");
                sb.append(";");
                sb.append("IOFID");
                sb.append(";");
                sb.append("Name");
                sb.append(";");
                sb.append("Points");
                sb.append(";\n");
                List<Start> starts = plan.getStarts();
                for (Start s : starts) {
                    for (Clazz clazz : s.getStartQueues()) {
                        LocalTime startTime = clazz.getFirstStart();
                        while (clazz != null) {
                            List<QualificationCompetitor> qualificationCompetitors = classToRunners.get(clazz.getName());
                            if (qualificationCompetitors != null) {

                                for (QualificationCompetitor c : qualificationCompetitors) {
                                    sb
                                            .append(startTime)
                                            .append(";")
                                            .append(clazz.getName())
                                            .append(";")
                                            .append(c.iofId())
                                            .append(";")
                                            .append(c.name().getGiven())
                                            .append(" ")
                                            .append(c.name().getFamily())
                                            .append(";")
                                            .append(c.points())
                                            .append("\n");
                                    startTime = startTime.plusSeconds(clazz.getStartInterval());
                                }
                            } else {
                                collectPossibleError("No sign ups for planned class" + clazz.getName());
                            }

                            clazz = clazz.getNextClazz();
                        }
                    }
                }
            }


            add(new H3("Start list:"));
            preview.setText(sb.toString());
            preview.setVisible(true);
            preview.setWidthFull();
            add(download, preview);
            notifyErrors();
        });

        calculatePoints.setEnabled(false);
        add(calculatePoints);

    }

    private void checkButtonValidity() {
        if (raceId != null && previousYearResults.size() > 0) {
            calculatePoints.setEnabled(true);
        } else {
            calculatePoints.setEnabled(false);
        }
    }
}
