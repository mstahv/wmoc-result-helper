package org.example;

import org.orienteering.datastandard._3.PersonName;

public record QualificationCompetitor(String iofId, PersonName name, String clazz, String nationality, int points) {
}