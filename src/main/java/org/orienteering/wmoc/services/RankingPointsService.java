package org.orienteering.wmoc.services;

import org.orienteering.datastandard._3.EntryList;
import org.orienteering.datastandard._3.Iof3ResultList;
import org.orienteering.wmoc.domain.QualificationCompetitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class RankingPointsService {

    public static Map<String, Integer> calculatePoints(List<Iof3ResultList> previousYearResults) {
        Map<String, Integer> points = new HashMap<>();
        previousYearResults.forEach(iof3ResultList -> {
            iof3ResultList.getClassResult().forEach(classResult -> {
                classResult.getPersonResult().forEach(personResult -> {
                    String iofId = personResult.getPerson().getId().get(0).getValue();
                    Integer position = personResult.getResult().get(0).getPosition();
                    if(position != null) {
                        int pos = position.intValue();
                        // first one gets 20 points, second 19, etc.
                        int pointsForPosition = 21 - pos;
                        if(pointsForPosition > 0) {
                            points.put(iofId, points.getOrDefault(iofId, 0) + pointsForPosition);
                        };
                    }
                });
            });
        });
        return points;
    }

    public static Map<String, List<QualificationCompetitor>> calculatePoints(ArrayList<Iof3ResultList> previousYearResults, EntryList entryList, Integer raceId) {
        Map<String, Integer> iofIdToPoints = calculatePoints(previousYearResults);

        Map<String, List<QualificationCompetitor>> classToCompetitor = new TreeMap<>();
        Map<String, List<QualificationCompetitor>> heatToCompetitor = new TreeMap<>();

        entryList.getPersonEntry().forEach(personEntry -> {
            if(personEntry.getRaceNumber().contains(Integer.valueOf(raceId))) {
                String iofId = personEntry.getPerson().getId().get(0).getValue();
                String clazz = personEntry.getClazz().get(0).getName();
                String emit = "";
                if(!personEntry.getControlCard().isEmpty()) {
                    emit = personEntry.getControlCard().get(0).getValue() +"";
                }
                String club = "";
                if(personEntry.getOrganisation() != null) {
                    club = personEntry.getOrganisation().getName();
                }
                classToCompetitor.computeIfAbsent(clazz, k -> new ArrayList<>()).add(

                        new QualificationCompetitor(
                                iofId,
                                personEntry.getId().getValue(),
                                personEntry.getPerson().getName(),
                                clazz,
                                personEntry.getPerson().getNationality().getCode(),
                                iofIdToPoints.getOrDefault(iofId,0),
                                emit,
                                club
                        )
                );
            }
        });

        classToCompetitor.forEach((clazz, competitors) -> {
            Collections.shuffle(competitors);
            competitors.sort((o1, o2) -> {
                int comparePoint = Integer.compare(o2.points(), o1.points());
                if(comparePoint != 0) {
                    return comparePoint;
                };
                return o1.nationality().compareTo(o2.nationality());
            });
            System.out.println("Class: " + clazz);
            competitors.forEach(competitor -> {
                System.out.println(competitor.name().getGiven() + " " + competitor.name().getFamily() + " " + competitor.points() + " " + competitor.nationality());
            });

            List<List<QualificationCompetitor>> heats = new ArrayList<>();
            int numHeats = (int) Math.ceil(competitors.size() / 80.0); // max 80  users per heat
            for(int i = 0; i < numHeats; i++) {
                heats.add(new ArrayList<>());
            }
            boolean raising = true;
            // distribute ranked runners to heats with "ABBA" principle
            // then continue with ABAB or BABA whichever direction was last in use
            for(int i = 0 ; i < competitors.size(); i++) {
                QualificationCompetitor competitor = competitors.get(i);
                int heatIndex;
                heatIndex = i % numHeats;
                // ABBA direction changes only if still withing ranked runners
                if(competitor.points() > 0 ) {
                    raising = (i /numHeats) % 2 == 0;
                }
                if(!raising) {
                    heatIndex = -heatIndex + numHeats -1;
                }
                heats.get(heatIndex).add(competitor);
            }
            for (int i = 0; i < heats.size(); i++) {
                List<QualificationCompetitor> competitorList = heats.get(i);
                // Not randomising here so analysing the correct order is easier,
                // Rudely the view shuffles if start times for classes chosen
                //Collections.shuffle(competitorList);
                heatToCompetitor.put(clazz + "-" + (i+1), competitorList);
            }
        });

        return heatToCompetitor;
    }

}
