package org.example;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class AgeClass {

    private final String name;

    private Map<ClazzQualifier,StartList> startLists = new LinkedHashMap<>();

    public AgeClass(String name) {
        this.name = name;
    }

    public StartList getStartList(ClazzQualifier clazzQualifier) {
        StartList startList = startLists.get(clazzQualifier);
        if (startList == null) {
            startList = new StartList(name, clazzQualifier);
            startLists.put(clazzQualifier, startList);
        }
        return startList;
    }

    public Map<ClazzQualifier, StartList> getStartLists() {
        return startLists;
    }

    public String getName() {
        return name;
    }
}
