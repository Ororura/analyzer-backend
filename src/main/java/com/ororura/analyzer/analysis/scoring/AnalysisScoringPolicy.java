package com.ororura.analyzer.analysis.scoring;

public record AnalysisScoringPolicy(
        double mustHaveWeight,
        double skillCoverageWeight,
        double experienceWeight,
        double atsWeight,
        double gradeWeight,
        double mustHaveRequiredFrequency,
        double coreFrequency,
        double highFrequency,
        double mediumFrequency,
        double highGapThreshold,
        double mediumGapThreshold,
        double[] atsWeights,
        double highGapWeight,
        double mediumGapWeight,
        double lowGapWeight,
        double lowEffortWeight,
        double mediumEffortWeight,
        double highEffortWeight) {

    public AnalysisScoringPolicy {
        atsWeights = atsWeights.clone();
    }

    @Override
    public double[] atsWeights() {
        return atsWeights.clone();
    }

    public static AnalysisScoringPolicy defaults() {
        return new AnalysisScoringPolicy(.40, .25, .20, .05, .10, .40, .70, .40, .20, .35, .15,
                new double[] {.20, .15, .10, .20, .10, .10, .15}, 1, .65, .35, .75, 1, 1.5);
    }
}
