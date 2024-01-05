package org.orienteering.wmoc.resulthelperui;

import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.orienteering.datastandard._3.EntryList;
import org.orienteering.datastandard._3.Iof3ResultList;
import org.orienteering.wmoc.domain.FinalClazz;
import org.orienteering.wmoc.services.NormalFinalService;
import org.vaadin.firitin.components.DynamicFileDownloader;
import org.vaadin.firitin.components.upload.UploadFileHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

@Route(layout = TopLayout.class)
public class NormalFinalView extends VerticalLayout {

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

    public NormalFinalView() {
        add("This view suits for normal finals, such as Sprint & Middle.");
        JAXBContext jaxbContext = null;
        try {
            jaxbContext = JAXBContext.newInstance(Iof3ResultList.class, EntryList.class);
            unmarshaller = jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }

        add(new H3("Qualification results"));

        UploadFileHandler qru = new UploadFileHandler((content, fileName, mimeType) -> {
            try {
                qualResults = (Iof3ResultList) unmarshaller.unmarshal(content);
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
            List<FinalClazz> finalClazzes = NormalFinalService.getFinalClazzes(qualResults);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintStream out = new PrintStream(outputStream);

            for (FinalClazz fc : finalClazzes) {
                fc.printCsv(out);
            }

            getUI().ifPresent(ui -> ui.access(() -> {
                preview.setText(new String(outputStream.toByteArray()));
                download.setEnabled(true);
                add(download, preview);
            }));
        });

        add(qru);

    }
}
