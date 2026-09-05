package com.ororura.analyzer.analysis.domain;

import java.util.*;
import java.util.regex.Pattern;

/** Versioned semantics; experience ranges are evidence, never an exact vacancy grade. */
public final class GradePolicy {
    public static final String VERSION = "grades-v1";
    public enum Match { MATCH, MISMATCH, UNKNOWN }
    public record Evidence(CandidateGrade vacancyGrade, String source) {}
    public int rank(CandidateGrade grade) {
        return switch (grade) {
            case INTERN -> 0; case JUNIOR -> 1; case JUNIOR_PLUS -> 2;
            case MIDDLE -> 3; case MIDDLE_PLUS -> 4; case SENIOR -> 5;
        };
    }
    public CandidateGrade detect(int months, int technical, int responsibility, int overall) {
        if (months >= 72 && technical >= 90 && responsibility >= 9 && overall >= 88) return CandidateGrade.SENIOR;
        if (months >= 48 && technical >= 82 && responsibility >= 8 && overall >= 82) return CandidateGrade.MIDDLE_PLUS;
        if (months >= 36 && technical >= 75 && responsibility >= 7 && overall >= 75) return CandidateGrade.MIDDLE;
        if (months >= 12 && technical >= 55 && overall >= 55) return CandidateGrade.JUNIOR_PLUS;
        return months == 0 ? CandidateGrade.INTERN : CandidateGrade.JUNIOR;
    }
    public Match match(CandidateGrade filter, CandidateGrade vacancy) {
        return vacancy == null ? Match.UNKNOWN : filter == vacancy ? Match.MATCH : Match.MISMATCH;
    }
    public Evidence classify(String title) {
        String text = title == null ? "" : title.toLowerCase(Locale.ROOT);
        Set<CandidateGrade> found = new HashSet<>();
        if (matches(text, "intern|trainee|стаж[её]р")) found.add(CandidateGrade.INTERN);
        if (matches(text, "junior|джун(?:иор)?")) found.add(text.matches(".*(?:junior|джун(?:иор)?)\\s*(?:\\+|plus).*" )
                ? CandidateGrade.JUNIOR_PLUS : CandidateGrade.JUNIOR);
        if (matches(text, "middle|мидл")) found.add(text.matches(".*(?:middle|мидл)\\s*(?:\\+|plus).*" )
                ? CandidateGrade.MIDDLE_PLUS : CandidateGrade.MIDDLE);
        if (matches(text, "senior|сеньор|сениор")) found.add(CandidateGrade.SENIOR);
        return new Evidence(found.size() == 1 ? found.iterator().next() : null,
                found.size() == 1 ? "TITLE" : found.isEmpty() ? "UNKNOWN" : "AMBIGUOUS_TITLE");
    }
    private boolean matches(String text, String tokens) {
        return Pattern.compile("(?<![\\p{L}])(?:" + tokens + ")(?![\\p{L}])").matcher(text).find();
    }
}
