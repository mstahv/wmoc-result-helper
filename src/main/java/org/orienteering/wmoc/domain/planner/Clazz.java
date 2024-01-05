package org.orienteering.wmoc.domain.planner;

import java.time.LocalTime;
import java.util.Objects;

public class Clazz {
    private Start start;

    private String name;
    private Clazz startsAfter;
    private Clazz nextClazz;
    private LocalTime firstStart;
    private int estimatedRunners = 80;
    private int startInterval = 60;

    public Clazz(String name, Start start) {
        this.name = name;
        this.start = start;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Clazz getStartsAfter() {
        return startsAfter;
    }

    public void setStartsAfter(Clazz newStartsAfter) {
        if(this.startsAfter == newStartsAfter) {
            throw new IllegalStateException("Already starts after given class!");
        }
        if(this == newStartsAfter) {
            throw new IllegalStateException("Can't queue after itself!");
        }
        if(isQueueRoot() && nextClazz != null) {
            // Move the roots start time to the new root
            nextClazz.setFirstStart(getFirstStart());
        }
        if(this.startsAfter != null) {
            Clazz nextClazz1 = this.getNextClazz();
            if(nextClazz1 != null) {
                // Dropping between clazzes, shorten chain
                nextClazz1.startsAfter = startsAfter;
                nextClazz1.startsAfter.nextClazz = nextClazz1;
                this.nextClazz = null;
            } else {
                this.startsAfter.nextClazz = null;
            }
        }
        if(newStartsAfter.nextClazz != null) {
            newStartsAfter.nextClazz.startsAfter = this;
            this.nextClazz = newStartsAfter.nextClazz;
        }
        this.startsAfter = newStartsAfter;
        newStartsAfter.nextClazz = this;
        firstStart = null;
    }

    public Clazz getNextClazz() {
        return nextClazz;
    }

    public LocalTime getFirstStart() {
        if (firstStart != null) {
            return firstStart;
        } else {
            int secs = 0;
            Clazz p = getStartsAfter();
            while (p != null) {
                secs = secs + p.getStartInterval() * p.getEstimatedRunners();
                if (p.getStartsAfter() == null) {
                    LocalTime startTime = p.getFirstStart();
                    return startTime.plusSeconds(secs);
                }
                p = p.getStartsAfter();
            }
        }
        throw new IllegalStateException("Queue root not found!");
    }

    /**
     * Makes the class a root of a start queue with given first start time.
     * Clears possible previously set connection to start queue
     *
     * @param firstStart
     */
    public void setFirstStart(LocalTime firstStart) {
        Objects.requireNonNull(firstStart);
        boolean becomingRoot = this.firstStart == null;
        this.firstStart = firstStart;
        if(startsAfter != null) {
            startsAfter.nextClazz = null;
            startsAfter = null;
        }
        if(becomingRoot) {
            start.getStartQueues().add(this);
        }
    }

    public int getEstimatedRunners() {
        return estimatedRunners;
    }

    public void setEstimatedRunners(int estimatedRunners) {
        this.estimatedRunners = estimatedRunners;
    }

    public int getStartInterval() {
        return startInterval;
    }

    public void setStartInterval(int startInterval) {
        this.startInterval = startInterval;
    }

    public boolean isQueueRoot() {
        return firstStart != null;
    }

    public Start getStart() {
        return start;
    }

    public void setStart(Start start) {
        this.start = start;
    }
}
