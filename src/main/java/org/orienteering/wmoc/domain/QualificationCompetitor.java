package org.orienteering.wmoc.domain;

import org.orienteering.datastandard._3.PersonName;

public record QualificationCompetitor(String iofId, PersonName name, String clazz, String nationality, int points, String emit, String club) {
}