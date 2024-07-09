package org.orienteering.wmoc.domain;

import java.io.PrintStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public record FinalClazz(String clazzName, List<FinalRunner> runners) {
    static final String DELIM = ";";

    public static void printCsvHeader(PrintStream out) {
        out.print("Class");
        out.print(DELIM);
        out.print("Starttime");
        out.print(DELIM);
        out.print("IOF ID");
        out.print(DELIM);
        out.print("BIB");
        out.print(DELIM);
        out.print("Given");
        out.print(DELIM);
        out.print("Family");
        out.print(DELIM);
        out.print("Nationality");
        out.print(DELIM);
        out.print("Organization");
        out.print(DELIM);
        out.print("Class/pos in qualification");
        out.println(DELIM);
    }

    // LocalDate formatter for format like 13:33:00
    private static final DateTimeFormatter HHmmss = DateTimeFormatter.ofPattern("H:mm:ss");

    public LocalTime printCsv(PrintStream out, LocalTime ns, int startInterval) {

        for (FinalRunner r : runners) {
            out.print(clazzName);
            out.print(DELIM);
            out.print(ns.format(HHmmss));
            out.print(DELIM);
            out.print(r.iofId());
            out.print(DELIM);
            out.print(r.bib());
            out.print(DELIM);
            out.print(r.given());
            out.print(DELIM);
            out.print(r.family());
            out.print(DELIM);
            out.print(r.nationality());
            out.print(DELIM);
            out.print(r.club());
            out.print(DELIM);
            out.print(r.qualClazz());
            out.println(DELIM);
            ns = ns.plusSeconds(startInterval);
        }
        return ns;
    }
}
