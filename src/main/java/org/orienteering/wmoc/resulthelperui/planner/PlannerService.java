package org.orienteering.wmoc.resulthelperui.planner;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import org.eclipse.serializer.Serializer;
import org.eclipse.serializer.SerializerFoundation;
import org.eclipse.serializer.persistence.types.PersistenceLegacyTypeMappingResultor;
import org.orienteering.wmoc.domain.planner.AllPlans;
import org.orienteering.wmoc.domain.planner.Clazz;
import org.orienteering.wmoc.domain.planner.Start;
import org.orienteering.wmoc.domain.planner.StartTimePlan;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Base64;
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
}
