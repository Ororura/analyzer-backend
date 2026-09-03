package com.ororura.analyzer.resume.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.resume.scoring")
public class ResumeScoringProperties {
    private double mustHaveWeight = .40;
    private double skillCoverageWeight = .25;
    private double experienceWeight = .20;
    private double atsWeight = .05;
    private double gradeWeight = .10;
    private double mustHaveRequiredFrequency = .40;
    private double coreFrequency = .70;
    private double highFrequency = .40;
    private double mediumFrequency = .20;
    private double highGapThreshold = .35;
    private double mediumGapThreshold = .15;
    private double atsParsingWeight = .20;
    private double atsSectionsWeight = .15;
    private double atsContactsWeight = .10;
    private double atsExperienceWeight = .20;
    private double atsEducationWeight = .10;
    private double atsSkillsWeight = .10;
    private double atsKeywordWeight = .15;
    private double highGapWeight = 1;
    private double mediumGapWeight = .65;
    private double lowGapWeight = .35;
    private double lowEffortWeight = .75;
    private double mediumEffortWeight = 1;
    private double highEffortWeight = 1.5;

    public double getMustHaveWeight() { return mustHaveWeight; }
    public void setMustHaveWeight(double value) { mustHaveWeight = value; }
    public double getSkillCoverageWeight() { return skillCoverageWeight; }
    public void setSkillCoverageWeight(double value) { skillCoverageWeight = value; }
    public double getExperienceWeight() { return experienceWeight; }
    public void setExperienceWeight(double value) { experienceWeight = value; }
    public double getAtsWeight() { return atsWeight; }
    public void setAtsWeight(double value) { atsWeight = value; }
    public double getGradeWeight() { return gradeWeight; }
    public void setGradeWeight(double value) { gradeWeight = value; }
    public double getMustHaveRequiredFrequency() { return mustHaveRequiredFrequency; }
    public void setMustHaveRequiredFrequency(double value) { mustHaveRequiredFrequency = value; }
    public double getCoreFrequency() { return coreFrequency; }
    public void setCoreFrequency(double value) { coreFrequency = value; }
    public double getHighFrequency() { return highFrequency; }
    public void setHighFrequency(double value) { highFrequency = value; }
    public double getMediumFrequency() { return mediumFrequency; }
    public void setMediumFrequency(double value) { mediumFrequency = value; }
    public double getHighGapThreshold() { return highGapThreshold; }
    public void setHighGapThreshold(double value) { highGapThreshold = value; }
    public double getMediumGapThreshold() { return mediumGapThreshold; }
    public void setMediumGapThreshold(double value) { mediumGapThreshold = value; }
    public double[] atsWeights() { return new double[] {atsParsingWeight, atsSectionsWeight, atsContactsWeight,
            atsExperienceWeight, atsEducationWeight, atsSkillsWeight, atsKeywordWeight}; }
    public void setAtsParsingWeight(double value) { atsParsingWeight = value; }
    public void setAtsSectionsWeight(double value) { atsSectionsWeight = value; }
    public void setAtsContactsWeight(double value) { atsContactsWeight = value; }
    public void setAtsExperienceWeight(double value) { atsExperienceWeight = value; }
    public void setAtsEducationWeight(double value) { atsEducationWeight = value; }
    public void setAtsSkillsWeight(double value) { atsSkillsWeight = value; }
    public void setAtsKeywordWeight(double value) { atsKeywordWeight = value; }
    public void setHighGapWeight(double value) { highGapWeight = value; }
    public void setMediumGapWeight(double value) { mediumGapWeight = value; }
    public void setLowGapWeight(double value) { lowGapWeight = value; }
    public void setLowEffortWeight(double value) { lowEffortWeight = value; }
    public void setMediumEffortWeight(double value) { mediumEffortWeight = value; }
    public void setHighEffortWeight(double value) { highEffortWeight = value; }
    public double gapWeight(com.ororura.analyzer.resume.domain.analysis.SkillGapAnalysis.GapPriority priority) {
        return switch (priority) { case HIGH -> highGapWeight; case MEDIUM -> mediumGapWeight; case LOW -> lowGapWeight; };
    }
    public double effortWeight(com.ororura.analyzer.resume.domain.analysis.SkillRoiAnalysis.Effort effort) {
        return switch (effort) { case LOW -> lowEffortWeight; case MEDIUM -> mediumEffortWeight; case HIGH -> highEffortWeight; };
    }

    public com.ororura.analyzer.analysis.scoring.AnalysisScoringPolicy toPolicy() {
        return new com.ororura.analyzer.analysis.scoring.AnalysisScoringPolicy(
                mustHaveWeight, skillCoverageWeight, experienceWeight, atsWeight, gradeWeight,
                mustHaveRequiredFrequency, coreFrequency, highFrequency, mediumFrequency,
                highGapThreshold, mediumGapThreshold, atsWeights(), highGapWeight, mediumGapWeight,
                lowGapWeight, lowEffortWeight, mediumEffortWeight, highEffortWeight);
    }

    @jakarta.annotation.PostConstruct
    void validate() {
        validateUnit(mustHaveWeight, skillCoverageWeight, experienceWeight, atsWeight, gradeWeight,
                mustHaveRequiredFrequency, coreFrequency, highFrequency, mediumFrequency,
                highGapThreshold, mediumGapThreshold);
        if (Math.abs(mustHaveWeight + skillCoverageWeight + experienceWeight + atsWeight + gradeWeight - 1) > 1e-9
                || Math.abs(java.util.Arrays.stream(atsWeights()).sum() - 1) > 1e-9) {
            throw new IllegalArgumentException("Resume scoring weights must sum to 1");
        }
    }

    private static void validateUnit(double... values) {
        for (double value : values) if (!Double.isFinite(value) || value < 0 || value > 1) {
            throw new IllegalArgumentException("Resume scoring threshold/weight must be between 0 and 1");
        }
    }
}
