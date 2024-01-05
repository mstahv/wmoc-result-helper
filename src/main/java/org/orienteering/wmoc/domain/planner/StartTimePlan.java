package org.orienteering.wmoc.domain.planner;

import java.util.ArrayList;
import java.util.List;

public class StartTimePlan {

    private String planName;

    private List<Start> starts = new ArrayList<>();


    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public List<Start> getStarts() {
        return starts;
    }

    public void setStarts(List<Start> starts) {
        this.starts = starts;
    }
}
