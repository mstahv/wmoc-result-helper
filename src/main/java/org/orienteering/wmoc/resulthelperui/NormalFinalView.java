package org.orienteering.wmoc.resulthelperui;

import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Pre;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.orienteering.datastandard._3.EntryList;
import org.orienteering.datastandard._3.Iof3ResultList;
import org.orienteering.wmoc.domain.FinalClazz;
import org.orienteering.wmoc.domain.planner.Clazz;
import org.orienteering.wmoc.domain.planner.Start;
import org.orienteering.wmoc.domain.planner.StartTimePlan;
import org.orienteering.wmoc.resulthelperui.planner.PlanSelect;
import org.orienteering.wmoc.services.NormalFinalService;
import org.vaadin.firitin.components.DynamicFileDownloader;
import org.vaadin.firitin.components.upload.UploadFileHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public class NormalFinalView extends AbstractCalculatorView {

    private final Pre preview = new Pre();
    private final DynamicFileDownloader download = new DynamicFileDownloader("Download CSV", "finals.csv", (OutputStream stream) -> {
        try {
            stream.write(preview.getText().getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    });
    private final Unmarshaller unmarshaller;
    private Iof3ResultList qualResults;

    public NormalFinalView(PlanSelect planSelect) {

        add("This view suits for normal finals, such as Sprint & Middle.");
        add(planSelect);
        JAXBContext jaxbContext = null;
        try {
            jaxbContext = JAXBContext.newInstance(Iof3ResultList.class, EntryList.class);
            unmarshaller = jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }

        add(new H3("Qualification results"));

        UploadFileHandler qru = new UploadFileHandler((content, m) -> {
            try {
                qualResults = (Iof3ResultList) unmarshaller.unmarshal(content);
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
            List<FinalClazz> finalClazzes = NormalFinalService.getFinalClazzes(qualResults);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintStream out = new PrintStream(outputStream);
            FinalClazz.printCsvHeader(out);

            StartTimePlan plan = planSelect.getValue();
            if(plan != null) {
                for(Start s : plan.getStarts()) {
                    for(Clazz c : s.getStartQueues()) {
                        LocalTime ns = c.getFirstStart();
                        int startInterval = c.getStartInterval();
                        while(c != null) {
                            Clazz c2 = c;
                            Optional<FinalClazz> matchingClass = finalClazzes.stream().filter(f -> f.clazzName().equals(c2.getName())).findFirst();
                            if(!matchingClass.isPresent()) {
                                collectPossibleError(c.getName() + " found in plans, but no class in input file!");
                             } else {
                                FinalClazz fc = matchingClass.get();
                                // remove so we can check if some were not handled
                                finalClazzes.remove(fc);
                                ns = fc.printCsv(out, ns, startInterval);
                            }
                            c = c.getNextClazz();
                        }
                    }
                }
                if(!finalClazzes.isEmpty()) {
                    collectPossibleError("These classes SKIPPED, no start times found from the plan:" + finalClazzes.stream().map(f -> f.clazzName()).toList());
                }
            } else {
                for (FinalClazz fc : finalClazzes) {
                    fc.printCsv(out, LocalTime.of(0,0), 60);
                }
            }

            return () -> {
                preview.setText(new String(outputStream.toByteArray()));
                preview.setWidthFull();
                download.setEnabled(true);
                add(download, preview);
                notifyErrors();
            };
        });

        add(qru);

    }

}
