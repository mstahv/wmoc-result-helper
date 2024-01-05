import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.orienteering.wmoc.domain.FinalClazz;
import org.orienteering.wmoc.domain.FinalRunner;
import org.orienteering.wmoc.services.NormalFinalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orienteering.datastandard._3.EntryList;
import org.orienteering.datastandard._3.Iof3ResultList;

import java.io.File;
import java.util.List;

public class NormalFinalStartListCompositionTest {

    private Unmarshaller unmarshaller;

    @BeforeEach
    public void setUp() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(Iof3ResultList.class, EntryList.class);
        unmarshaller = jaxbContext.createUnmarshaller();
    }

    @Test
    public void composeFinalStartLists() throws JAXBException {

        File qrf = new File("src/test/resources/wmoc_2022_sq_result_splittimes.xml");

        Iof3ResultList qualificationResults = (Iof3ResultList) unmarshaller.unmarshal(qrf);

        List<FinalClazz> finals = NormalFinalService.getFinalClazzes(qualificationResults);

        for (FinalClazz fc : finals) {
            System.out.println("### " + fc.clazzName() + ", " + fc.runners().size() + " runners");
        }

        for (FinalClazz fc : finals) {
            fc.printCsv(System.out);
        }

    }

    private void csvPrint(List<FinalRunner> a) {
        var o = System.out;
        for (FinalRunner r : a) {
            o.print(r.iofId());
            o.print(";");
            o.print(r.name());
            o.print(";");
            o.println(r.qualClazz());
        }
    }
}
