package org.orienteering.wmoc.services;

import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@SessionScope
@Component
public class StartTimeService {

    public StartTimeService() {
    }

    private Map<Integer, Map<String, LocalTime>> raceToIofIdToStartTime = new HashMap<>();

    public void saveStartTime(Integer raceId, String iofId, LocalTime startTime) {
        raceToIofIdToStartTime
                .computeIfAbsent(raceId, i -> new HashMap<>())
                .put(iofId, startTime);
    }

    public LocalTime getStartTime(Integer raceId, String iofId) {
        Map<String, LocalTime> idToLocalTimeMap = raceToIofIdToStartTime.get(raceId);
        if(idToLocalTimeMap != null) {
            return idToLocalTimeMap.get(iofId);
        }
        return null;
    }

    public int getRaceCount() {
        return raceToIofIdToStartTime.size();
    }
}
