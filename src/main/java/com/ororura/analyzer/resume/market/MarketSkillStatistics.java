package com.ororura.analyzer.resume.market;

public record MarketSkillStatistics(
        String normalizedSkill,
        String displayName,
        Long vacancyCount,
        Long totalVacancies,
        double frequency,
        double requiredFrequency,
        double preferredFrequency,
        double optionalFrequency,
        SkillImportance importance) {

    public MarketSkillStatistics {
        if (normalizedSkill == null || normalizedSkill.isBlank() || displayName == null || displayName.isBlank()
                || importance == null) {
            throw new IllegalArgumentException("Market skill identity and importance are required");
        }
        validate(frequency);
        validate(requiredFrequency);
        validate(preferredFrequency);
        validate(optionalFrequency);
        if ((vacancyCount == null) != (totalVacancies == null)
                || vacancyCount != null && (vacancyCount < 0 || totalVacancies < vacancyCount)) {
            throw new IllegalArgumentException("Market skill counts are inconsistent");
        }
    }

    private static void validate(double value) {
        if (!Double.isFinite(value) || value < 0 || value > 1) {
            throw new IllegalArgumentException("Market skill frequency must be between 0 and 1");
        }
    }
}
