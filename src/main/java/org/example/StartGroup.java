package org.example;

import org.orienteering.datastandard._3.Person;

import java.util.ArrayList;

public class StartGroup extends ArrayList<FinalCompetitor> {
    public FinalCompetitor pick(Person person) {
        FinalCompetitor finalCompetitor = null;
        for (FinalCompetitor c : this) {
            if(c.getPerson().equals(person)) {
                finalCompetitor = c;
                break;
            }
        }
        remove(finalCompetitor);
        return finalCompetitor;
    }

    public boolean contains(Person o) {
        for (FinalCompetitor fc :this) {
            // TODO check IOFID
            if(fc.getPerson().equals(o)) {
                return true;
            }
        }
        return false;
    }
}
