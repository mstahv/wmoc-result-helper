package org.orienteering.wmoc.resulthelperui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.orienteering.datastandard._3.Iof3ResultList;
import org.orienteering.wmoc.resulthelperui.planner.PlanSelect;
import org.orienteering.wmoc.services.ClassSplitterService;
import org.vaadin.firitin.components.DynamicFileDownloader;
import org.vaadin.firitin.components.upload.UploadFileHandler;

import java.io.IOException;
import java.io.OutputStream;

public class ForestFinalView extends AbstractCalculatorView {

    private final Pre preview = new Pre();
    private final DynamicFileDownloader download = new DynamicFileDownloader("Download CSV", "finals.csv", (OutputStream stream) -> {
        try {
            stream.write(preview.getText().getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    });
    private final Unmarshaller unmarshaller;
    private final Button button;
    private final PlanSelect planSelect;
    private Iof3ResultList qualResults;
    private Iof3ResultList middleResults;

    public ForestFinalView(PlanSelect planSelect) {
        this.planSelect = planSelect;
        add("This view makes the magical promotions/relegations for forest final");
        add(planSelect);
        JAXBContext jaxbContext = null;
        try {
            jaxbContext = JAXBContext.newInstance(Iof3ResultList.class);
            unmarshaller = jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }

        download.setEnabled(false);

        VerticalLayout controls = new VerticalLayout();
        button = new Button("Split to finals", e -> {
            String startlist = ClassSplitterService.splitToFinals(qualResults, middleResults, planSelect.getValue(), getErrors());
            preview.setText(startlist);
            download.setEnabled(true);
            notifyErrors();
        });
        button.setEnabled(false);

        UploadFileHandler uploadMiddle = new UploadFileHandler((content, fileName, mimeType) -> {
            try {
                middleResults = (Iof3ResultList) unmarshaller.unmarshal(content);
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
            getUI().ifPresent(ui -> ui.access(() -> {
                checkButtonValidity();
            }));
        }).withClearAutomatically(false);

        UploadFileHandler uploadQual = new UploadFileHandler((content, fileName, mimeType) -> {
            try {
                qualResults = (Iof3ResultList) unmarshaller.unmarshal(content);
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
            getUI().ifPresent(ui -> ui.access(() -> {
                checkButtonValidity();
            }));
        }).withClearAutomatically(false);

        controls.add(
                new H3("Middle results (IOF3):"),
                uploadMiddle,
                new H3("Qualification results (IOF3):"),
                uploadQual,
                button
        );

        add(
                controls,
                download,
                new H3("Preview:"),
                preview);

    }

    private void checkButtonValidity() {
        if (middleResults != null && qualResults != null) {
            button.setEnabled(true);
        }
    }
}
