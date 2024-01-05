package org.orienteering.wmoc.domain;

import org.orienteering.datastandard._3.Iof3PersonResult;
import org.orienteering.datastandard._3.Person;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StartList {
    private final String clazz;
    final ClazzQualifier clazzQualifier;

    StartGroup relegated = new StartGroup();
    StartGroup normalStartGroup = new StartGroup();
    StartGroup promoted = new StartGroup();
    StartGroup extraStarters = new StartGroup();

    public StartList(String clazz, ClazzQualifier clazzQualifier) {
        this.clazz = clazz;
        this.clazzQualifier = clazzQualifier;
    }

    public ClazzQualifier getClazzQualifier() {
        return clazzQualifier;
    }


    public void initFromPersonResults(List<Iof3PersonResult> personResult) {
        personResult.forEach(pr -> {
            normalStartGroup.add(new FinalCompetitor(pr.getPerson(), pr.getResult().get(0).getPosition(), pr.getResult().get(0).getTime(), clazzQualifier));
        });
    }

    public List<FinalCompetitor> pickFromTop(int promotionAmount) {
        List<FinalCompetitor> promotions = new ArrayList(normalStartGroup.subList(0, promotionAmount));
        if(promotions.size() == 0) {
            return promotions;
        }
        FinalCompetitor lastQualifier = promotions.get(promotionAmount - 1);
        int i = promotionAmount;
        while(normalStartGroup.size() > i ) {
            FinalCompetitor potentialTie = normalStartGroup.get(i);
            if(potentialTie.getPosition() == lastQualifier.getPosition()) {
                promotions.add(potentialTie);
                System.out.println("Took in a tie: " + potentialTie.getPerson().getName().getGiven() + " " + potentialTie.getPerson().getName().getFamily());
                i++;
            } else {
                break;
            }
        }
        normalStartGroup.removeAll(promotions);
        return promotions;
    }

    public void addPromoted(List<FinalCompetitor> promotions) {
        promoted.addAll(promotions);
    }

    public void addRelegated(List<FinalCompetitor> relegations) {
        relegated.addAll(relegations);
    }

    public void addExtraStarter(FinalCompetitor c) {
        this.extraStarters.add(c);
    }

    public List<FinalCompetitor> pickFromBottom(int amountFromBottom) {
        Collections.reverse(normalStartGroup);
        List<FinalCompetitor> competitors = pickFromTop(amountFromBottom);
        Collections.reverse(normalStartGroup);
        return competitors;
    }

    public FinalCompetitor pick(Person person) {
        return null;
    }

    public StartGroup getExtraStarters() {
        return extraStarters;
    }

    public StartGroup getPromoted() {
        return promoted;
    }

    public StartGroup getRelegated() {
        return relegated;
    }

    public StartGroup getNormalStartGroup() {
        return normalStartGroup;
    }
}
