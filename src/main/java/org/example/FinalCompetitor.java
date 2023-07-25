package org.example;

import org.orienteering.datastandard._3.Person;

import java.math.BigInteger;
import java.security.Principal;

public class FinalCompetitor {
    private final Person person;
    private int position = Integer.MAX_VALUE;
    private int time = Integer.MAX_VALUE;
    private String reason;

    public FinalCompetitor(Person person, BigInteger position, Double time) {
        this.person = person;
        if(time != null) {
            this.time = (int) (time.doubleValue()*100); // 100ths of a second
        }
        if(position != null) {
            this.position = position.intValue();
        }
    }

    public int getPosition() {
        return position;
    }

    public Person getPerson() {
        return person;
    }

    public int getTime() {
        return time;
    }

    public void setReason(String s) {
        this.reason = s;
    }

    public String getReason() {
        return reason;
    }
}
