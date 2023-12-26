package org.example;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

public record FinalClazz(String clazzName, List<FinalRunner> runners) {
    public void printCsv(PrintStream out) {
        final String DELIM = ";";
        for (FinalRunner r : runners) {
            out.print(clazzName);
            out.print(DELIM);
            out.print(r.iofId());
            out.print(DELIM);
            out.print(r.name());
            out.print(DELIM);
            out.print(r.qualClazz());
            out.println(DELIM);
        }
    }
}
