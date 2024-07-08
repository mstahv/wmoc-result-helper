package org.orienteering.wmoc.resulthelperui.planner;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.spring.annotation.UIScope;
import org.eclipse.serializer.Serializer;
import org.eclipse.serializer.SerializerFoundation;
import org.orienteering.wmoc.domain.planner.AllPlans;
import org.orienteering.wmoc.domain.planner.Clazz;
import org.orienteering.wmoc.domain.planner.Start;
import org.orienteering.wmoc.domain.planner.StartTimePlan;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

@Component
@UIScope
public class PlannerService {

    private final Serializer<byte[]> serializer;

    public PlannerService() {
        SerializerFoundation<?> foundation = SerializerFoundation.New();
        // Define the custom Java types that we want to serialize/deserialize
        // Without these hints, deserialisation will fail if it happens
        // in a different process than the one that serialized the data.
        // There is also TypedSerializer which is a bit more convenient,
        // and don't need these configs, but slower and less secure.
        foundation.registerEntityTypes(
                AllPlans.class,
                Clazz.class,
                StartTimePlan.class,
                Start.class
        );

        serializer = Serializer.Bytes(foundation);
    }

    private AllPlans allPlans;


    public Future<AllPlans> getAllPlans() {
        if(allPlans != null) {
            return CompletableFuture.completedFuture(allPlans);
        } else {
            CompletableFuture<AllPlans> allPlansCompletableFuture = new CompletableFuture<>();
            WebStorage.getItem("plans", (WebStorage.Callback) s -> {
                if(s != null) {
                    int b64l = s.length();
                    byte[] bytes = Base64.getDecoder().decode(s);
                    int length = bytes.length;
                    try {
                        allPlans = serializer.deserialize(bytes);
                    } catch (Exception e) {
                        // Incompatible deserialization issue, too large
                        // data model changes
                        Notification.show("Sorry your data was destroyed :-(");
                    }
                }
                if(allPlans == null) {
                    allPlans = new AllPlans();
                }
                allPlansCompletableFuture.complete(allPlans);
            });
            return allPlansCompletableFuture;
        }
    }

    public void saveToLocalStorage() {
        byte[] bytes = serializer.serialize(allPlans);
        int length = bytes.length;
        String s = Base64.getEncoder().encodeToString(bytes);
        int b64lenght = s.length();
        WebStorage.setItem("plans", s);
    }

    public byte[] backup(StartTimePlan value) {
        return serializer.serialize(value);
    }

    public StartTimePlan readBackup(byte[] bytes) {
        StartTimePlan plan = serializer.deserialize(bytes);
        // given special name if similarly named already exists
        allPlans.getPlans().stream().filter(p -> p.getPlanName().equals(plan.getPlanName()))
                .findFirst().ifPresent(p -> {
                    plan.setPlanName(plan.getPlanName() + ", uploaded " + LocalDateTime.now());
                });
        allPlans.getPlans().add(plan);
        return plan;
    }

    public void remove(StartTimePlan startTimePlan) {
        if(allPlans != null) {
            allPlans.getPlans().remove(startTimePlan);
        }
    }

    public void toCsv(StartTimePlan plan, OutputStream outputStream) {
        var writer = new PrintStream(outputStream);
        writer.print("Start");
        writer.print(DELIM);
        writer.print("Class");
        writer.print(DELIM);
        writer.print("First start");
        writer.print(DELIM);
        writer.print("Interval");
        writer.print(DELIM);
        writer.print("EstimatedRunners");
        writer.println(DELIM);
        List<Start> starts = plan.getStarts();
        for(Start s : starts) {
            for(Clazz c : s.getStartQueues()) {
                printCsvLine(s, c, writer);
                while(c.getNextClazz() != null) {
                    c = c.getNextClazz();
                    printCsvLine(s, c, writer);
                }
            }
        }
    }

    private static final String DELIM = ";";

    // LocalDate formatter for format like 13:33:00
    private static final DateTimeFormatter HHmmss = DateTimeFormatter.ofPattern("H:mm:ss");

    private void printCsvLine(Start s, Clazz c, PrintStream writer) {
        writer.print(s.getName());
        writer.print(DELIM);
        writer.print(c.getName());
        writer.print(DELIM);
        writer.print(c.getFirstStart().format(HHmmss));
        writer.print(DELIM);
        writer.print(c.getStartInterval());
        writer.print(DELIM);
        writer.print(c.getEstimatedRunners());
        writer.println(DELIM);
    }

    public StartTimePlan readCsv(InputStream is, String filename) throws IOException {
        var reader = new BufferedReader(new InputStreamReader(is));
        reader.readLine(); // skip header
        StartTimePlan plan = new StartTimePlan();
        plan.setPlanName(filename);
        Map<String,Start> nameToStart = new HashMap<>();

        String line = reader.readLine();
        while(line != null) {
            if(line.isBlank()) {
                line = reader.readLine();
                continue;
            }
            // Start 1;M35;11:00;60;80;
            String[] parts = line.split(";");
            String startName = parts[0];
            Start start = nameToStart.computeIfAbsent(startName, k -> {
                Start s = new Start(startName);
                plan.getStarts().add(s);
                return s;
            });
            Clazz clazz = new Clazz(parts[1], start);
            clazz.setFirstStart(LocalTime.parse(parts[2], HHmmss));
            clazz.setStartInterval(Integer.parseInt(parts[3]));
            clazz.setEstimatedRunners(Integer.parseInt(parts[4]));
            start.getStartQueues().add(clazz);

            line = reader.readLine();
        }
        Collections.sort(plan.getStarts(), Comparator.comparing(Start::getName));

        // given special name if similarly named already exists
        allPlans.getPlans().stream().filter(p -> p.getPlanName().equals(plan.getPlanName()))
                .findFirst().ifPresent(p -> {
                    plan.setPlanName(plan.getPlanName() + ", uploaded " + LocalDateTime.now());
                });
        allPlans.getPlans().add(plan);

        return plan;

    }
}
