package org.example;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Receiver;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.Route;
import org.vaadin.firitin.components.DynamicFileDownloader;
import org.vaadin.firitin.components.upload.UploadFileHandler;

import java.io.IOException;
import java.io.OutputStream;

@Route
public class MainView extends VerticalLayout {


    private final Pre preview = new Pre();
    private final DynamicFileDownloader download = new DynamicFileDownloader("Download CSV", "finals.csv" , (OutputStream stream) -> {
        try {
            stream.write(preview.getText().getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    });
    public MainView() {

        download.setEnabled(false);

        HorizontalLayout controls = new HorizontalLayout();
        controls.setAlignItems(Alignment.END);

        TextField url = new TextField("URL");
        url.setValue("https://online4.tulospalvelu.fi/tulokset/results_2023_smkeski_1_iof.xml");

        Button button = new Button("Split to finals", e -> {
                    preview.setText(ClassSplitterService.splitToFinals(url.getValue()));
                    download.setEnabled(true);
                });

        controls.add(url, button,new H3("... or upload a file to split"));

        UploadFileHandler upload = new UploadFileHandler((content, fileName, mimeType) -> {
            String startlist = ClassSplitterService.splitToFinals(content);
            getUI().get().access(() -> {
                preview.setText(startlist);
                download.setEnabled(true);
            });
        });
        controls.add(upload);

        add(new H1("WMOC Forest final class splitter"),
                controls,
                download,
                new H3("Preview:"),
                preview);

    }
}
