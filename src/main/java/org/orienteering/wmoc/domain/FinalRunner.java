package org.orienteering.wmoc.domain;

public record FinalRunner(
        String iofId,
        String bib,
        String given,
        String family,
        String nationality,
        String club,
        String qualClazz) {
}
