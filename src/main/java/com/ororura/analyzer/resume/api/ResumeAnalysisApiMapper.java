package com.ororura.analyzer.resume.api;

import com.ororura.analyzer.resume.application.ResumeAnalysisOutcome;
import org.springframework.stereotype.Component;

@Component
public class ResumeAnalysisApiMapper {

    private final ResumeAnalysisMarkdownRenderer markdownRenderer;

    public ResumeAnalysisApiMapper(ResumeAnalysisMarkdownRenderer markdownRenderer) {
        this.markdownRenderer = markdownRenderer;
    }

    public ResumeAnalysisResult toResponse(ResumeAnalysisOutcome source) {
        var result = new ResumeAnalysisResult(source.targetRole(),
                ResumeAnalysisResult.CandidateLevel.valueOf(source.detectedLevel().name()),
                new ResumeAnalysisResult.Scores(source.scores().assessments(), source.scores().commercialExperience(),
                        source.scores().experienceDescription(), source.scores().ats(), source.scores().resumeQuality()),
                source.overallScore(), source.candidateStrength(),
                ResumeAnalysisResult.InterviewChance.valueOf(source.hrScreeningChance().name()),
                ResumeAnalysisResult.InterviewChance.valueOf(source.technicalInterviewChance().name()),
                new ResumeAnalysisResult.Experience(source.experience().commercialMonths(),
                        source.experience().commercialYears(), source.experience().remainingMonths()),
                new ResumeAnalysisResult.Skills(source.skills().confirmed(), source.skills().weakEvidence(),
                        source.skills().missing()),
                source.strengths(), source.weaknesses(), source.atsIssues(), source.recommendations(),
                source.vacancyFit(), new ResumeAnalysisResult.Market(source.market().source(), source.market().sampleSize()),
                new ResumeAnalysisResult.Metadata(source.metadata().analysisSchemaVersion(),
                        source.metadata().analysisProfile(), source.metadata().analysisVersion(),
                        source.metadata().baselineVersion(), source.metadata().marketProfileVersion(),
                        source.metadata().marketProfileSource(), source.metadata().generatedAt(),
                        source.metadata().provider(), source.metadata().model()),
                source.marketFit(), source.marketPosition(), source.technicalProfile(), source.skillEvidence(),
                source.skillGaps(), source.skillRoi(), source.gradeFit(), source.ats(), source.claimRisks(),
                source.interviewRisks(), source.risks(), source.recommendationAnalysis(), null, source.warnings());
        return result.withMarkdownReport(markdownRenderer.render(result));
    }
}
