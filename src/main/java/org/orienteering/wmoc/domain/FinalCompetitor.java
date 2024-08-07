package org.orienteering.wmoc.domain;

import org.orienteering.datastandard._3.Person;

public class FinalCompetitor {
    private final Person person;
    private final String bib;
    private final String nationality;
    private final String organization;
    boolean startedInFinal;
    private int position = Integer.MAX_VALUE;
    private int time = Integer.MAX_VALUE;
    private String reason;

    private ClazzQualifier middleFinalClass;

    public FinalCompetitor(Person person, String bib, String nationality, String organization, Integer position, Double time, ClazzQualifier middleFinalClass, boolean startedInFinal) {
        this.person = person;
        this.bib = bib;
        this.nationality = nationality;
        this.organization = organization;
        this.startedInFinal = startedInFinal;
        if(time != null) {
            this.time = (int) (time.doubleValue()*100); // 100ths of a second
        }
        if(position != null) {
            this.position = position.intValue();
        }
        if(middleFinalClass != null) {
            this.middleFinalClass = middleFinalClass;
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

    public ClazzQualifier getMiddleFinalClass() {
        return middleFinalClass;
    }

    public String getBib() {
        return bib;
    }

    public String getNationality() {
        return nationality;
    }

    public String getOrganization() {
        return organization;
    }

    public boolean isStartedInFinal() {
        return startedInFinal;
    }
}
