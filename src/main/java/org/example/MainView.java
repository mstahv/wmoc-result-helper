package org.example;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.orienteering.datastandard._3.Iof3ResultList;
import org.vaadin.firitin.components.DynamicFileDownloader;
import org.vaadin.firitin.components.upload.UploadFileHandler;

import java.io.IOException;
import java.io.OutputStream;

@Route
public class MainView extends VerticalLayout {


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
    private Iof3ResultList qualResults;
    private Iof3ResultList middleResults;

    public MainView() {
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
            String startlist = ClassSplitterService.splitToFinals(qualResults, middleResults);
            preview.setText(startlist);
            download.setEnabled(true);
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
        });

        UploadFileHandler uploadQual = new UploadFileHandler((content, fileName, mimeType) -> {
            try {
                qualResults = (Iof3ResultList) unmarshaller.unmarshal(content);
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
            getUI().ifPresent(ui -> ui.access(() -> {
                checkButtonValidity();
            }));
        });

        controls.add(
                new H3("Middle results (IOF3):"),
                uploadMiddle,
                new H3("Qualification results (IOF3):"),
                uploadQual,
                button
        );

        add(new H1("WMOC Forest final class splitter"),
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
