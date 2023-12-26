package org.example;

import com.helger.commons.mutable.MutableInt;
import org.orienteering.datastandard._3.Iof3ClassResult;
import org.orienteering.datastandard._3.Iof3PersonResult;
import org.orienteering.datastandard._3.Iof3ResultList;
import org.orienteering.datastandard._3.PersonRaceResult;

import java.math.BigInteger;
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
            String mainClass = r.getClazz().getShortName().substring(0, r.getClazz().getShortName().indexOf("-"));
            mainClassToClassResult.computeIfAbsent(mainClass, s -> new ArrayList<>()).add(r);
        });

        List<FinalClazz> finals = new ArrayList<>();

        mainClassToClassResult.entrySet().forEach(entry -> {
            String mainClass = entry.getKey();
            List<Iof3ClassResult> resultLists = entry.getValue();
            // sort by desc qualification by class name, first from last heat is the last starter
            Collections.sort(resultLists, (o1, o2) ->
                    o2.getClazz().getShortName().compareTo(o1.getClazz().getShortName()));

            int numberOfRunners = resultLists.stream()
                    .mapToInt(rl -> rl.getPersonResult().size())
                    .sum();
            int runnersInFinals = 0;
            if(numberOfRunners < 20) {
                // All to A final
                List<FinalRunner> a = collectFinalClass(resultLists, new MutableInt(1));
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
                // Note, that only runners with a proper result are picked to A

                List<FinalRunner> b = new ArrayList<>();
                // collect those with result, in order by position
                while(b.size() < numberToBFinal && resultsFound) {
                    resultsFound = collectResultsForPosition(resultLists, pos, b);
                    pos++;
                }

                // Then pick remaining runners (mis-punched, dnf, dns, etc)
                // TODO Picking these 1 by qual class as long as found, don't
                // know what would be right. Should be randomized or sorted with some logic??
                while(collectNonPositioned(resultLists, b)) {

                }

                finals.add(new FinalClazz(mainClass + "A", a));
                finals.add(new FinalClazz(mainClass + "B", b));
                runnersInFinals += a.size();
                runnersInFinals += b.size();

            } else {

                List<FinalRunner> f;
                MutableInt pos = new MutableInt(1);
                int clazzIndex = 0;
                while(!(f = collectFinalClass(resultLists, pos)).isEmpty()) {
                    runnersInFinals += f.size();
                    if(f.size() < 20) {
                        // too few for the last final, merge with the previous
                        finals.get(finals.size() -1).runners().addAll(f);
                    } else {
                        ClazzQualifier qualifier = ClazzQualifier.values()[clazzIndex];

                        finals.add(new FinalClazz(mainClass + qualifier, f));
                        clazzIndex++;
                    }
                }

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

    private static List<FinalRunner> collectFinalClass(List<Iof3ClassResult> resultLists, MutableInt pos) {
        ArrayList<FinalRunner> list = new ArrayList<>();
        while(list.size() < 80) {
            if(!pos.isLE0()) {
                // collect next round by position
                boolean foundResults = collectResultsForPosition(resultLists, pos.intValue(), list);
                if(!foundResults) {
                    // indicate that no more positioned results available
                    pos.set(-1);
                } else {
                    pos.inc();
                }
            } else {
                // all positioned found, collect non-positioned
                // TODO: refactor so that only one "round" take at once
                boolean foundNonPositioned = collectNonPositioned(resultLists, list);
                if(!foundNonPositioned) {
                    break;
                }
            }
        }
        return list;
    }

    private static boolean collectNonPositioned(List<Iof3ClassResult> resultLists, List<FinalRunner> b) {
        boolean found = false;
        for(Iof3ClassResult classResult : resultLists) {
            if(!classResult.getPersonResult().isEmpty()) {
                found = true;
                Iof3PersonResult r = classResult.getPersonResult().remove(0);
                b.add(new FinalRunner(
                        r.getPerson().getId().get(0).getValue(),
                        r.getPerson().getName().getGiven() + " " + r.getPerson().getName().getFamily(),
                        classResult.getClazz().getShortName() + "/" + r.getResult().get(0).getStatus().value()
                ));
            }
        }
        return found;
    }

    private static boolean collectResultsForPosition(List<Iof3ClassResult> resultLists, int pos, List<FinalRunner> a) {
        int collected = 0;
        boolean hasResults = false;
        for(var rl : resultLists) {
            Iterator<Iof3PersonResult> iterator = rl.getPersonResult().iterator();
            while(iterator.hasNext() ) {
                Iof3PersonResult pr = iterator.next();
                PersonRaceResult prr = pr.getResult().get(0);
                BigInteger position = prr.getPosition();
                if (position != null) {
                    hasResults = true;
                    if(position.intValue() == pos) {
                        a.add(new FinalRunner(
                                pr.getPerson().getId().get(0).getValue(),
                                pr.getPerson().getName().getGiven() + " " + pr.getPerson().getName().getFamily(),
                                rl.getClazz().getShortName() + "/" + position.intValue()
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
