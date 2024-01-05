import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.orienteering.wmoc.services.RankingPointsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orienteering.datastandard._3.EntryList;
import org.orienteering.datastandard._3.Iof3ResultList;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

public class RankingPointsTest {

    private Unmarshaller unmarshaller;

    @BeforeEach
    public void setUp() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(Iof3ResultList.class, EntryList.class);
        unmarshaller = jaxbContext.createUnmarshaller();
    }

    @Test
    public void testCalculatePoints() throws JAXBException {
        File entryListFile = new File("src/test/resources/entries_World_Masters_Orienteering_Championships_2024.xml");

        if(!entryListFile.exists()) {
            // Skip, test data not available
            return;
        }


        EntryList entryList = (EntryList) unmarshaller.unmarshal(entryListFile);

        File middleResultsFile = new File("src/test/resources/wmoc_2022_lf_split_times.xml");
        Iof3ResultList middleResults = (Iof3ResultList) unmarshaller.unmarshal(middleResultsFile);
        ArrayList<Iof3ResultList> previousYearResults = new ArrayList<>();
        previousYearResults.add(middleResults);

        Map<String, Integer> iofIdToPoints = RankingPointsService.calculatePoints(previousYearResults);

        int raceId = 1; // 3 == forest qualification
        var heatToCompetitor = RankingPointsService.calculatePoints(previousYearResults, entryList, raceId);
        heatToCompetitor.forEach((h, competitors) -> {
            System.out.println("Heat: " + h);

            competitors.forEach(c -> {
                System.out.println(c.name().getGiven() + " " + c.name().getFamily() + " " + c.points() + " " + c.nationality());
            });

        });

    }
}
