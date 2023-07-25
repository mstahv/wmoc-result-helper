package org.example;

import org.orienteering.datastandard._3.Iof3ClassResult;
import org.orienteering.datastandard._3.Iof3PersonResult;
import org.orienteering.datastandard._3.Iof3ResultList;
import org.orienteering.datastandard._3.PersonRaceResult;
import org.orienteering.datastandard._3.ResultStatus;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ClassSplitterService {


    public static String splitToFinals(Iof3ResultList qual, Iof3ResultList middleResults) {
        StringBuilder sb = new StringBuilder();

        Map<String, List<Iof3ClassResult>> mainClassToResult = new LinkedHashMap<>();

        // collect all results per main class for easier handling
        middleResults.getClassResult().forEach(cr -> {
            String mainClass = cr.getClazz().getName().substring(0, 3);
            List<Iof3ClassResult> results = mainClassToResult.get(mainClass);
            if (results == null) {
                results = new ArrayList<>();
                mainClassToResult.put(mainClass, results);
            }
            results.add(cr);
        });

        List<AgeClass> ageClasses = new ArrayList<>();
        mainClassToResult.forEach((mainClass, results) -> {
            AgeClass ageClass = new AgeClass(mainClass);
            for (int i = 0; i < results.size(); i++) {
                // assumes the age classes are in order in the result file!
                // for example H55A before H55B
                ClazzQualifier qualifier = ClazzQualifier.values()[i];
                ageClass.getStartList(qualifier).initFromPersonResults(results.get(i).getPersonResult());
            }
            ageClasses.add(ageClass);

            // Handle rules for promotion & relegation
            List<PromotionRule> promotionRules = clazzAmountToPromotionRules.get(results.size());
            if (promotionRules != null) {
                promotionRules.forEach(r -> {
                    StartList from = ageClass.getStartList(r.getFrom());
                    StartList to = ageClass.getStartList(r.getTo());
                    List<FinalCompetitor> promotions = from.pickFromTop(r.getAmountFromTop());
                    to.addPromoted(promotions);

                    List<FinalCompetitor> relegations = from.pickFromBottom(r.getAmountFromBottom());
                    to.addRelegated(relegations);
                    System.out.println("Promotion rule handled!");
                });
            } else if (results.size() == 2) {
                System.out.println("10% rule for only two classes");
                StartList a = ageClass.getStartList(ClazzQualifier.A);
                StartList b = ageClass.getStartList(ClazzQualifier.B);

                int promotionAmount = (int) Math.floor(a.normalStartGroup.size() * 0.1);
                List<FinalCompetitor> relegated = a.pickFromBottom(promotionAmount);
                b.addRelegated(relegated);

                // Note, in theory we might now relegate competitors with valid result
                // from A, and promote competitors without one from B, but I guess the
                // rules can't be perfect

                List<FinalCompetitor> promoted = b.pickFromTop(relegated.size());
                a.addPromoted(promoted);

                System.out.println("10% rule handled!");
            } else {
                System.out.println("No promotion rules for " + results.size() + " class(s)");
            }

            // finally check the "top 4 from qualification always to A rule"
            // if more than 1 classes
            if(ageClass.getStartLists().size() > 1) {
                // go through qualification results and pick top 4 from each class
                qual.getClassResult().forEach(cr -> {
                    String mc = cr.getClazz().getName().substring(0, 3);
                    if (mc.equals(mainClass)) {
                        List<Iof3PersonResult> eligibleForFinal = new ArrayList<>();
                        cr.getPersonResult().stream()
                                .filter(pr -> pr.getResult().get(0).getPosition() != null
                                        && pr.getResult().get(0).getPosition().intValue() < 5)
                                .forEach(eligibleForFinal::add);

                        for (Iof3PersonResult pr : eligibleForFinal) {
                            StartList a = ageClass.getStartList(ClazzQualifier.A);
                            if (!a.normalStartGroup.contains(pr.getPerson())) {
                                // not in normal start group in A, so must be relegated to B
                                // lift back to A
                                System.out.println("Top 4 from qualification always to A rule: " + pr.getPerson().getName().getFamily() + " " + pr.getPerson().getName().getGiven());
                                FinalCompetitor finalCompetitor = ageClass.getStartList(ClazzQualifier.B).relegated.pick(pr.getPerson());
                                if(finalCompetitor == null) {
                                    // didn't start in middle final at all -> skip this one
                                    System.out.println(pr.getPerson().getName().getFamily() + " " + pr.getPerson().getName().getGiven() + " not found in relegated list, most likely didn't start in Middle final at all.");
                                    continue;
                                }
                                finalCompetitor.setReason("Top 4 from qualification always to A");
                                a.addExtraStarter(finalCompetitor);
                            }
                        }
                    }
                });
            }
        });

        // print the start orders
        ageClasses.forEach(ageClass -> {
            ageClass.getStartLists().forEach((clazzQualifier, startList) -> {
                // first the extra starters, e.g. top from qualification, but mp in middle
                sortStartGroup(startList.extraStarters);
                startList.extraStarters.forEach(fc -> {
                    printCompetitor(sb, ageClass, clazzQualifier, fc, "extra starter");
                });

                // thin the promoted, empty for last final
                sortStartGroup(startList.promoted);
                startList.promoted.forEach(fc -> {
                    printCompetitor(sb, ageClass, clazzQualifier, fc, "promoted");
                });

                // then print normal start group
                sortStartGroup(startList.normalStartGroup);
                startList.normalStartGroup.forEach(fc -> {
                    printCompetitor(sb, ageClass, clazzQualifier, fc, "normal");
                });

                // last the relegated, empty for A finals
                sortStartGroup(startList.relegated);
                startList.relegated.forEach(fc -> {
                    printCompetitor(sb, ageClass, clazzQualifier, fc, "relegated");
                });

            });
        });
        return sb.toString();
    }

    public static class PromotionRule {
        private ClazzQualifier from;
        private ClazzQualifier to;
        private int amountFromTop;
        private int amountFromBottom;

        public PromotionRule(ClazzQualifier from, ClazzQualifier to, int amountFromTop, int amountFromBottom) {
            this.from = from;
            this.to = to;
            this.amountFromTop = amountFromTop;
            this.amountFromBottom = amountFromBottom;
        }

        public ClazzQualifier getFrom() {
            return from;
        }

        public ClazzQualifier getTo() {
            return to;
        }

        public int getAmountFromTop() {
            return amountFromTop;
        }

        public int getAmountFromBottom() {
            return amountFromBottom;
        }

    }

    private static final Map<Integer, List<PromotionRule>> clazzAmountToPromotionRules = new HashMap<>();

    static {
        List<PromotionRule> rules;

        // 3 aka A,B,C
        rules = new ArrayList<>();
        rules.add(new PromotionRule(ClazzQualifier.A, ClazzQualifier.B, 0, 12));
        rules.add(new PromotionRule(ClazzQualifier.B, ClazzQualifier.A, 10, 0));
        rules.add(new PromotionRule(ClazzQualifier.B, ClazzQualifier.C, 0, 10));
        rules.add(new PromotionRule(ClazzQualifier.C, ClazzQualifier.A, 2, 0));
        rules.add(new PromotionRule(ClazzQualifier.C, ClazzQualifier.B, 8, 0));
        clazzAmountToPromotionRules.put(3, rules);

        // 4 aka A,B,C,D
        rules = new ArrayList<>();
        rules.add(new PromotionRule(ClazzQualifier.A, ClazzQualifier.B, 0, 15));
        rules.add(new PromotionRule(ClazzQualifier.B, ClazzQualifier.A, 12, 0));
        rules.add(new PromotionRule(ClazzQualifier.B, ClazzQualifier.C, 0, 12));
        rules.add(new PromotionRule(ClazzQualifier.C, ClazzQualifier.A, 1, 0));
        rules.add(new PromotionRule(ClazzQualifier.C, ClazzQualifier.B, 9, 0));
        rules.add(new PromotionRule(ClazzQualifier.C, ClazzQualifier.D, 0, 10));
        rules.add(new PromotionRule(ClazzQualifier.D, ClazzQualifier.A, 2, 0));
        rules.add(new PromotionRule(ClazzQualifier.D, ClazzQualifier.C, 8, 0));
        clazzAmountToPromotionRules.put(4, rules);

        // 5 aka A,B,C,D,E
        rules = new ArrayList<>();
        rules.add(new PromotionRule(ClazzQualifier.A, ClazzQualifier.B, 0, 18));
        rules.add(new PromotionRule(ClazzQualifier.B, ClazzQualifier.A, 14, 0));
        rules.add(new PromotionRule(ClazzQualifier.B, ClazzQualifier.C, 0, 14));
        rules.add(new PromotionRule(ClazzQualifier.C, ClazzQualifier.A, 1, 0));
        rules.add(new PromotionRule(ClazzQualifier.C, ClazzQualifier.B, 10, 0));
        rules.add(new PromotionRule(ClazzQualifier.C, ClazzQualifier.D, 0, 12));
        rules.add(new PromotionRule(ClazzQualifier.D, ClazzQualifier.A, 1, 0));
        rules.add(new PromotionRule(ClazzQualifier.D, ClazzQualifier.C, 9, 0));
        rules.add(new PromotionRule(ClazzQualifier.D, ClazzQualifier.E, 0, 10));
        rules.add(new PromotionRule(ClazzQualifier.E, ClazzQualifier.A, 2, 0));
        rules.add(new PromotionRule(ClazzQualifier.E, ClazzQualifier.D, 8, 0));
        clazzAmountToPromotionRules.put(5, rules);

    }


    private static void sortStartGroup(StartGroup startgroup) {
        // First randomize everyone, so that non-placed gets random start times
        Collections.shuffle(startgroup);
        // Then sort by position or time if defined
        Collections.sort(startgroup, (o1, o2) -> {
            if (o1.getPosition() == Integer.MAX_VALUE && o2.getPosition() == Integer.MAX_VALUE) {
                // neither has been placed, check by time if defined or consider it a tie if neither has time defined
                return o1.getTime() - o2.getTime();
            }
            // normally check by position
            // non-set (DNF,DSQ, etc) is Integer.MAX_VALUE -> last
            return o1.getPosition() - o2.getPosition();
        });
        // Reverse, so that worst start firs, best in the end
        Collections.reverse(startgroup);
    }

    private static void printCompetitor(StringBuilder sb, AgeClass ageClass, ClazzQualifier clazzQualifier, FinalCompetitor fc, String comment) {
        sb.append(ageClass.getName() + "-" + clazzQualifier);
        sb.append(";");
        sb.append(fc.getPerson().getName().getGiven() + " " + fc.getPerson().getName().getFamily());
        sb.append(";");
        sb.append(comment);
        sb.append(";");
        if (fc.getPosition() != Integer.MAX_VALUE) {
            sb.append(fc.getPosition());
        }
        sb.append(";");
        if (fc.getTime() != Integer.MAX_VALUE) {
            sb.append(Duration.ofMillis(fc.getTime() * 10));
        }
        sb.append(";");
        if(fc.getReason() != null) {
            sb.append(fc.getReason());
        }
        sb.append(";\n");
    }

}
