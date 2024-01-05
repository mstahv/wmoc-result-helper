package org.orienteering.wmoc.domain.planner;

import java.time.LocalTime;
import java.util.ArrayList;

public class AllPlans {

    private ArrayList<StartTimePlan> plans = new ArrayList<>();

    public AllPlans() {
        StartTimePlan startTimePlan = new StartTimePlan();
        startTimePlan.setPlanName("Your first plan");
        Start start = new Start("Start 1");
        startTimePlan.getStarts().add(start);

        Clazz clazz = new Clazz("M35", start);
        clazz.setFirstStart(LocalTime.of(11,0));

        Clazz clazz2 = new Clazz("M40A", start);
        clazz2.setStartsAfter(clazz);

        Clazz clazz3 = new Clazz("M45A", start);
        clazz3.setStartsAfter(clazz2);

        clazz = new Clazz("D35A", start);
        clazz.setFirstStart(LocalTime.of(11,0));

        start = new Start("Start 2");

        var c4 = new Clazz("M50", start);
        c4.setFirstStart(LocalTime.of(11,30));

        var c5 = new Clazz("M55", start);
        c5.setStartsAfter(c4);

        startTimePlan.getStarts().add(start);

        plans.add(startTimePlan);
    }

    public ArrayList<StartTimePlan> getPlans() {
        return plans;
    }
}
