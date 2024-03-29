package org.orienteering.wmoc.domain;

import java.io.PrintStream;
import java.time.LocalTime;
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
        out.print("Name");
        out.print(DELIM);
        out.print("Class/pos in qualification");
        out.println(DELIM);
    }

    public LocalTime printCsv(PrintStream out, LocalTime ns, int startInterval) {

        for (FinalRunner r : runners) {
            out.print(clazzName);
            out.print(DELIM);
            out.print(ns);
            out.print(DELIM);
            out.print(r.iofId());
            out.print(DELIM);
            out.print(r.name());
            out.print(DELIM);
            out.print(r.qualClazz());
            out.println(DELIM);
            ns = ns.plusSeconds(startInterval);
        }
        return ns;
    }
}
