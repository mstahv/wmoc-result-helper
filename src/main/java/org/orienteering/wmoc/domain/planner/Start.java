package org.orienteering.wmoc.domain.planner;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Start {

    private String name;
    Set<Clazz> startQueues = new LinkedHashSet<>();

    public Start(String s) {
        name = s;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Clazz> getStartQueues() {
        return startQueues;
    }
}
