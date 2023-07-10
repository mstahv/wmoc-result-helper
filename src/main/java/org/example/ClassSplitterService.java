package org.example;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.orienteering.datastandard._3.Class;
import org.orienteering.datastandard._3.Iof3ClassResult;
import org.orienteering.datastandard._3.Iof3PersonResult;
import org.orienteering.datastandard._3.Iof3ResultList;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ClassSplitterService {

    public static String splitToFinals(String value) {
        return splitToFinals(value, null);
    }

    public static String splitToFinals(InputStream content) {
        return splitToFinals(null, content);
    }

    public enum ClazzQualifier {
        A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z
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


    private static String splitToFinals(String urlValue, InputStream stream) {
        StringBuilder sb = new StringBuilder();
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Iof3ResultList.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            Iof3ResultList rl;
            if(stream != null) {
                rl = (Iof3ResultList) jaxbUnmarshaller.unmarshal(stream);
            } else {
                rl = (Iof3ResultList) jaxbUnmarshaller.unmarshal(new URL(urlValue));
            }

            Map<String, List<Iof3ClassResult>> mainClassToResult = new LinkedHashMap<>();

            rl.getClassResult().forEach(cr -> {
                String mainClass = cr.getClazz().getName().substring(0, 3);
                List<Iof3ClassResult> results = mainClassToResult.get(mainClass);
                if (results == null) {
                    results = new ArrayList<>();
                    mainClassToResult.put(mainClass, results);
                }
                results.add(cr);
            });

            Map<String, List<Iof3ClassResult>> nextMainClassToStarter = new LinkedHashMap<>();

            mainClassToResult.forEach((mainClass, results) -> {
                List<Iof3ClassResult> nextclasses = new ArrayList<>();
                for (int i = 0; i < results.size(); i++) {
                    String clazzName = mainClass + ClazzQualifier.values()[i];
                    Iof3ClassResult iof3ClassResult = new Iof3ClassResult();
                    Class aClass = new Class();
                    aClass.setName(clazzName);
                    iof3ClassResult.setClazz(aClass);
                    iof3ClassResult.getPersonResult().addAll(results.get(i).getPersonResult());
                    nextclasses.add(iof3ClassResult);
                }
                nextMainClassToStarter.put(mainClass, nextclasses);

                List<PromotionRule> promotionRules = clazzAmountToPromotionRules.get(results.size());
                if (promotionRules != null) {
                    promotionRules.forEach(r -> {
                        Iof3ClassResult from = nextclasses.get(r.getFrom().ordinal());
                        Iof3ClassResult to = nextclasses.get(r.getTo().ordinal());
                        List<Iof3PersonResult> personResult = results.get(r.getFrom().ordinal()).getPersonResult();

                        List<Iof3PersonResult> promotions = new ArrayList<>(personResult.subList(0, r.getAmountFromTop()));
                        takeInTies(promotions, personResult, r.getAmountFromTop());
                        to.getPersonResult().addAll(promotions);
                        from.getPersonResult().removeAll(promotions);

                        // abusing sex as comment field for testing...
                        promotions.forEach(p -> p.getPerson().setSex("promoted, from " + results.get(r.getFrom().ordinal()).getClazz().getName()));

                        // Currently takes just the last from the result list, TODO should DNS/DNF/DQ be taken into account?
                        Collections.reverse(personResult);
                        List<Iof3PersonResult> relegations = new ArrayList<>(personResult.subList(0, r.getAmountFromBottom()));
                        takeInTies(relegations, personResult, r.getAmountFromBottom());
                        // abusing sex as comment field for testing...
                        relegations.forEach(p -> p.getPerson().setSex("relegated, from " + results.get(r.getFrom().ordinal()).getClazz().getName()));
                        Collections.reverse(personResult);

                        to.getPersonResult().addAll(relegations);
                        from.getPersonResult().removeAll(relegations);
                        System.out.println("Promotion rule handled!");
                    });
                } else if(nextclasses.size() == 2) {
                    System.out.println("10% rule for only two classes");

                    Iof3ClassResult a = nextclasses.get(0);
                    Iof3ClassResult b = nextclasses.get(1);
                    List<Iof3PersonResult> personResult = results.get(1).getPersonResult();

                    int promotionAmount = (int) Math.floor(personResult.size() * 0.1);

                    List<Iof3PersonResult> promotions = new ArrayList<>(personResult.subList(0, promotionAmount));
                    takeInTies(promotions, personResult, promotionAmount);
                    a.getPersonResult().addAll(promotions);
                    b.getPersonResult().removeAll(promotions);

                    // abusing sex as comment field for testing...
                    promotions.forEach(p -> p.getPerson().setSex("promoted, from B"));

                    // Currently takes just the last from the result list, TODO should DNS/DNF/DQ be taken into account?
                    personResult = results.get(0).getPersonResult();
                    Collections.reverse(personResult);
                    List<Iof3PersonResult> relegations = new ArrayList<>(personResult.subList(0, promotionAmount));
                    takeInTies(relegations, personResult, promotionAmount);
                    // abusing sex as comment field for testing...
                    relegations.forEach(p -> p.getPerson().setSex("relegated, from A"));
                    Collections.reverse(personResult);

                    b.getPersonResult().addAll(relegations);
                    a.getPersonResult().removeAll(relegations);
                    System.out.println("10% rule handled!");
                } else {
                    System.out.println("No promotion rules for " + results.size() + " class(s)");
                }

            });

            nextMainClassToStarter.forEach((nextMainClass, nextResults) -> {
                nextResults.forEach(nextResult -> {
                    List<Iof3PersonResult> personResult = nextResult.getPersonResult();
                    Collections.reverse(personResult);
                    personResult.forEach(p -> {
                        sb.append(nextResult.getClazz().getName());
                        sb.append(";");
                        sb.append(p.getPerson().getName().getGiven() + " " + p.getPerson().getName().getFamily());
                        sb.append(";");
                        sb.append(p.getPerson().getSex());
                        sb.append(";\n");
                    });
                });
            });

        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
        return sb.toString();
    }

    private static void takeInTies(List<Iof3PersonResult> promotions, List<Iof3PersonResult> personResult, int promotionAmount) {
        if(promotions.size() == 0) {
            return;
        }
        Iof3PersonResult lastQualifier = promotions.get(promotionAmount - 1);
        int i = promotionAmount;
        while(personResult.size() > i ) {
            Iof3PersonResult potentialTie = personResult.get(i);
            // TODO check if/when there can be multiple results in the file. Not with test data at least.
            if(potentialTie.getResult().get(0).getPosition() == lastQualifier.getResult().get(0).getPosition()) {
                promotions.add(potentialTie);
                System.out.println("Took in a tie: " + potentialTie.getPerson().getName().getGiven() + " " + potentialTie.getPerson().getName().getFamily());
                i++;
            } else {
                break;
            }
        }
    }
}
