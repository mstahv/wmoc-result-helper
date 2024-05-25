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
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.Route;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.orienteering.datastandard._3.ControlCard;
import org.orienteering.datastandard._3.EntryList;
import org.orienteering.datastandard._3.Iof3ResultList;
import org.orienteering.datastandard._3.PersonEntry;
import org.orienteering.datastandard._3.PersonServiceRequest;
import org.orienteering.datastandard._3.Service;
import org.orienteering.datastandard._3.ServiceRequest;
import org.orienteering.datastandard._3.ServiceRequestList;
import org.vaadin.firitin.components.DynamicFileDownloader;
import org.vaadin.firitin.components.checkbox.VCheckBox;
import org.vaadin.firitin.components.grid.VGrid;
import org.vaadin.firitin.components.listbox.VMultiSelectListBox;
import org.vaadin.firitin.components.orderedlayout.VHorizontalLayout;
import org.vaadin.firitin.components.orderedlayout.VVerticalLayout;
import org.vaadin.firitin.components.textfield.VTextField;
import org.vaadin.firitin.components.upload.UploadFileHandler;

import java.io.File;
import java.io.FileInputStream;
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
public class CompetiotionNumberingView extends AbstractCalculatorView {

    private final DynamicFileDownloader download = new DynamicFileDownloader("Download CSV", "finals.csv", (OutputStream stream) -> {
        try {
            stream.write("TODO".getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    });
    private final Unmarshaller unmarshaller;
    private final Marshaller marshaller;
    private final File tmpFile;

    public CompetiotionNumberingView() {
        add("Replaces IOF PersonEntry IDs with generated competition numbers (running number in the order found in the file (=order of entries).");

        IntegerField firstNumber = new IntegerField("First number");
        firstNumber.setValue(1001);
        add(firstNumber);

        JAXBContext jaxbContext = null;
        try {
            jaxbContext = JAXBContext.newInstance(Iof3ResultList.class, EntryList.class);
            unmarshaller = jaxbContext.createUnmarshaller();

            marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            tmpFile = File.createTempFile("renumbered", "xml");

        } catch (JAXBException | IOException e) {
            throw new RuntimeException(e);
        }


        DynamicFileDownloader downdloadIofXml = new DynamicFileDownloader("Downdload IOF XML", "renumbered.iof.xml", out -> {
            FileUtils.copyFile(tmpFile, out);
        });
        downdloadIofXml.setVisible(false);

        UploadFileHandler uploadFileHandler = new UploadFileHandler((content, m) -> {
            try {
                EntryList xml = (EntryList) unmarshaller.unmarshal(content);

                int currentNumber = firstNumber.getValue();

                for(PersonEntry pe : xml.getPersonEntry()) {
                    pe.getId().setType("bib");
                    pe.getId().setValue(""+currentNumber);
                    currentNumber++;
                }
                marshaller.marshal(xml, tmpFile);
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }

            return () -> {
                notifyErrors();
                downdloadIofXml.setVisible(true);
            };
        });

        add(uploadFileHandler, downdloadIofXml);
    }

}
