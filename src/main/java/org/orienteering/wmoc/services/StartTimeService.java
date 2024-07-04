package org.orienteering.wmoc.services;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@SessionScope
@Component
public class StartTimeService {

    public record StartInfo(LocalTime time, String clazz, String start) {
        @Override
        public String toString() {
            return clazz + " " + time + " " + start;
        }
    }

    public StartTimeService() {
    }

    private Map<Integer, Map<String, StartInfo>> raceToIofIdToStartTime = new HashMap<>();

    public void saveStartTime(Integer raceId, String iofId, LocalTime startTime, String clazz, String start) {
        raceToIofIdToStartTime
                .computeIfAbsent(raceId, i -> new HashMap<>())
                .put(iofId, new StartInfo(startTime, clazz, start));
    }

    public StartInfo getStartTime(Integer raceId, String iofId) {
        Map<String, StartInfo> idToLocalTimeMap = raceToIofIdToStartTime.get(raceId);
        if(idToLocalTimeMap != null) {
            return idToLocalTimeMap.get(iofId);
        }
        return null;
    }

    public int getRaceCount() {
        return raceToIofIdToStartTime.size();
    }
}
