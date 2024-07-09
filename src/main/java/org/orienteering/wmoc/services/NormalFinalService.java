package org.orienteering.wmoc.services;

import org.orienteering.datastandard._3.Iof3ClassResult;
import org.orienteering.datastandard._3.Iof3PersonResult;
import org.orienteering.datastandard._3.Iof3ResultList;
import org.orienteering.datastandard._3.PersonRaceResult;
import org.orienteering.wmoc.domain.ClazzQualifier;
import org.orienteering.wmoc.domain.FinalClazz;
import org.orienteering.wmoc.domain.FinalRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class NormalFinalService {

    public static List<FinalClazz> getFinalClazzes(Iof3ResultList qualificationResults) {
        Map<String, List<Iof3ClassResult>> mainClassToClassResult = new TreeMap<>();
        qualificationResults.getClassResult().forEach(r -> {
            String className = r.getClazz().getShortName();
            if(className == null) {
                className = r.getClazz().getName();
            }
            String mainClass = className.substring(0, className.indexOf("-"));
            mainClassToClassResult.computeIfAbsent(mainClass, s -> new ArrayList<>()).add(r);
        });

        List<FinalClazz> finals = new ArrayList<>();

        mainClassToClassResult.entrySet().forEach(entry -> {
            String mainClass = entry.getKey();
            List<Iof3ClassResult> resultLists = entry.getValue();
            // sort by desc qualification by class name, first from last heat is the last starter
            Collections.sort(resultLists, (o1, o2) -> {
                String o1name;
                String o2name;
                if(o1.getClazz().getShortName() == null) {
                    o1name = o1.getClazz().getName();
                    o2name = o2.getClazz().getName();
                } else {
                    o1name = o1.getClazz().getShortName();
                    o2name = o2.getClazz().getShortName();
                }
                return o2name.compareTo(o1name);
            });

            int numberOfRunners = resultLists.stream()
                    .mapToInt(rl -> rl.getPersonResult().size())
                    .sum();
            int runnersInFinals = 0;
            if(numberOfRunners < 20) {
                // All to A final
                List<FinalRunner> a = new ArrayList<>();
                collectPositionedToFinalClass(resultLists, 1, a);
                // Also non-positioned ones, but these should be "ignored"
                // in the final (can get OK result, but non-positioned)
                // TODO should have special flag in the data structure/CSV export??
                while (collectNonPositioned(resultLists, a)) {}
                finals.add(new FinalClazz(mainClass + "A", a));
                runnersInFinals += a.size();
            } else if(numberOfRunners <= 160) {
                // A & B finals
                int numberToAFinal = numberOfRunners / 2 + (numberOfRunners%2==0 ? 0 : 1);
                int numberToBFinal = numberOfRunners / 2;
                int pos = 1;
                boolean resultsFound = true;
                List<FinalRunner> a = new ArrayList<>();
                // collect those with result, in order by position
                while(a.size() < numberToAFinal && resultsFound) {
                    resultsFound = collectResultsForPosition(resultLists, pos, a);
                    pos++;
                }
                // Note, that only runners with a proper result are picked to A.
                // In theory A final could become quite small

                List<FinalRunner> b = new ArrayList<>();
                // collect those with result, in order by position
                while(b.size() < numberToBFinal && resultsFound) {
                    resultsFound = collectResultsForPosition(resultLists, pos, b);
                    pos++;
                }

                // Then pick remaining runners (mis-punched, dnf, dns, etc)
                // TODO Picking these 1 by qual class as long as found, don't
                // know what would be right. Should be randomized or sorted with some logic??
                while(collectNonPositioned(resultLists, b)) {}

                finals.add(new FinalClazz(mainClass + "A", a));
                finals.add(new FinalClazz(mainClass + "B", b));
                runnersInFinals += a.size();
                runnersInFinals += b.size();
            } else {
                // 3 or more final classes...
                List<FinalRunner> f;
                int clazzIndex = 0;
                int pos = 1;
                do {
                    f = new ArrayList<>();
                    pos = collectPositionedToFinalClass(resultLists, pos, f);
                    runnersInFinals += f.size();
                    ClazzQualifier qualifier = ClazzQualifier.values()[clazzIndex];
                    finals.add(new FinalClazz(mainClass + qualifier, f));
                    clazzIndex++;
                } while(f.size() >= 80);

                // collect non-positioned runners
                List<FinalRunner> nonPositioned = new ArrayList<>();
                while(collectNonPositioned(resultLists, nonPositioned)) {}

                // Note, there is no actual rule for this, but apparently
                // for iof 2022 they have done it this way
                if((f.size() + nonPositioned.size()) > 100) {
                    // Huge amount of non-positioned, "ö final" becomes
                    // too big -> create yet another for non-positioned
                    ClazzQualifier qualifier = ClazzQualifier.values()[clazzIndex];
                    finals.add(new FinalClazz(mainClass + qualifier, nonPositioned));
                } else {
                    // normal situation, add non-positioned to the last one
                    f.addAll(nonPositioned);
                    if(f.size() < 20) {
                        // too few for the last final, merge with the previous
                        finals.get(finals.size() -2).runners().addAll(f);
                        finals.remove(finals.size() - 1);
                    }
                }
                runnersInFinals += nonPositioned.size();
            }
            if(runnersInFinals != numberOfRunners) {
                throw new RuntimeException("Not all qualifiers collected to finals!");
            }
        });

        // All collected, reverse order (fastest starts last)
        for (FinalClazz fc : finals) {
            Collections.reverse(fc.runners());
        }
        return finals;
    }

    /**
     * Compiles a final from qualification results, where there is 80 runners
     * (if found), but so that all runners with same position as the 80th
     * qualified runners will be taken in.
     *
     * @param resultLists
     * @param pos
     * @return
     */
    private static int collectPositionedToFinalClass(List<Iof3ClassResult> resultLists,int pos, List<FinalRunner> list) {
        while(list.size() < 80) {
            // collect next round by position
            boolean foundOkResults = collectResultsForPosition(resultLists, pos, list);
            pos++;
            if(!foundOkResults) {
                break;
            }
        }
        return pos;
    }

    private static boolean collectNonPositioned(List<Iof3ClassResult> resultLists, List<FinalRunner> b) {
        boolean found = false;
        for(Iof3ClassResult classResult : resultLists) {
            if(!classResult.getPersonResult().isEmpty()) {
                found = true;
                Iof3PersonResult r = classResult.getPersonResult().remove(0);
                if(r.getResult().get(0).getPosition() != null) {
                    throw new IllegalStateException("A positioned runner is not yet collected!");
                }
                b.add(new FinalRunner(
                        r.getPerson().getId().get(0).getValue(),
                        r.getPerson().getName().getGiven() + " " + r.getPerson().getName().getFamily(),
                        r.getPerson().getName().getGiven(),
                        r.getPerson().getName().getFamily(),
                        (classResult.getClazz().getShortName() == null ? classResult.getClazz().getName(): classResult.getClazz().getShortName() ) + "/" + r.getResult().get(0).getStatus().value()
                ));
            }
        }
        return found;
    }

    private static boolean collectResultsForPosition(List<Iof3ClassResult> resultLists, int pos, List<FinalRunner> a) {
        boolean hasResults = false;
        for(var rl : resultLists) {
            Iterator<Iof3PersonResult> iterator = rl.getPersonResult().iterator();
            while(iterator.hasNext() ) {
                Iof3PersonResult pr = iterator.next();
                PersonRaceResult prr = pr.getResult().get(0);
                Integer position = prr.getPosition();
                if (position != null) {
                    hasResults = true;
                    if(position.intValue() == pos) {
                        a.add(new FinalRunner(
                                pr.getPerson().getId().get(0).getValue(),
                                pr.getPerson().getName().getGiven() + " " + pr.getPerson().getName().getFamily(),
                                pr.getPerson().getName().getGiven(),
                                pr.getPerson().getName().getFamily(),
                                (rl.getClazz().getShortName() == null ? rl.getClazz().getName(): rl.getClazz().getShortName() ) + "/" + position.intValue()
                        ));
                        iterator.remove();
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        return hasResults;
    }
}
