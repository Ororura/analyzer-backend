package com.ororura.analyzer.resume.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.ororura.analyzer.resume.application.port.AiProvider;
import com.ororura.analyzer.resume.application.AiProviderRegistry;
import com.ororura.analyzer.resume.application.port.AiProviderType;
import com.ororura.analyzer.resume.application.port.ResumeDocument;
import com.ororura.analyzer.resume.application.LlmResponseValidator;
import com.ororura.analyzer.analysis.semantic.LlmResumeAnalysisResponse;
import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfile;
import com.ororura.analyzer.market.application.VacancyMarketRequest;
import com.ororura.analyzer.resume.config.ResumeAnalysisProperties;
import com.ororura.analyzer.resume.domain.ExperienceModels.EmploymentPeriod;
import com.ororura.analyzer.market.domain.MarketAnalysisProfile;
import com.ororura.analyzer.market.application.port.MarketAnalysisProfileProvider;
import com.ororura.analyzer.resume.application.PdfFileValidator;
import com.ororura.analyzer.resume.application.port.PdfTextExtractor;
import com.ororura.analyzer.resume.error.ResumeAnalysisException;
import com.ororura.analyzer.resume.error.ResumeErrorCode;
import com.ororura.analyzer.market.application.VacancyMarketService;
import com.ororura.analyzer.market.domain.VacancyMarketData;
import com.ororura.analyzer.vacancy.application.selection.VacancySelectionException;
import com.ororura.analyzer.vacancy.application.port.VacancySourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ResumeAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ResumeAnalysisService.class);

    private final PdfFileValidator fileValidator;
    private final PdfTextExtractor textExtractor;
    private final VacancyMarketService marketService;
    private final AiProviderRegistry aiProviderRegistry;
    private final LlmResponseValidator llmValidator;
    private final ResumeAnalysisEngine analysisEngine;
    private final ResumeAnalysisAssembler assembler;
    private final ResumeAnalysisProperties properties;
    private final Clock clock;
    private final MarketAnalysisProfileProvider marketProfileProvider;

    public ResumeAnalysisService(PdfFileValidator fileValidator, PdfTextExtractor textExtractor,
            VacancyMarketService marketService, AiProviderRegistry aiProviderRegistry, LlmResponseValidator llmValidator,
            ResumeAnalysisEngine analysisEngine, ResumeAnalysisAssembler assembler,
            ResumeAnalysisProperties properties, Clock clock, MarketAnalysisProfileProvider marketProfileProvider) {
        this.fileValidator = fileValidator;
        this.textExtractor = textExtractor;
        this.marketService = marketService;
        this.aiProviderRegistry = aiProviderRegistry;
        this.llmValidator = llmValidator;
        this.analysisEngine = analysisEngine;
        this.assembler = assembler;
        this.properties = properties;
        this.clock = clock;
        this.marketProfileProvider = marketProfileProvider;
    }

    public ResumeAnalysisOutcome analyze(ResumeDocument document) {
        return analyze(document, null);
    }

    public ResumeAnalysisOutcome analyze(ResumeDocument document, AiProviderType requestedProvider) {
        return analyze(document, requestedProvider, VacancyMarketRequest.autoMarket());
    }

    public ResumeAnalysisOutcome analyze(ResumeDocument document, AiProviderType requestedProvider,
            VacancyMarketRequest vacancyAnalysis) {
        return analyze(document, requestedProvider, null, vacancyAnalysis);
    }

    public ResumeAnalysisOutcome analyze(ResumeDocument document, AiProviderType requestedProvider,
            ResumeAnalysisProfile requestedProfile, VacancyMarketRequest vacancyAnalysis) {
        String analysisId = UUID.randomUUID().toString();
        Instant started = clock.instant();
        log.info("resume analysis started analysisId={} fileSize={}", analysisId,
                document == null ? 0 : document.declaredSize());
        try {
            return analyze(document, requestedProvider, requestedProfile, vacancyAnalysis, analysisId, started);
        } catch (VacancySelectionException exception) {
            ResumeErrorCode code = exception.getMessage().contains("maximumProcessingSize")
                    ? ResumeErrorCode.SELECTION_TOO_LARGE : ResumeErrorCode.INVALID_SELECTION;
            throw new ResumeAnalysisException(code, exception.getMessage(), exception);
        } catch (VacancySourceException exception) {
            ResumeErrorCode code = switch (exception.getCode()) {
                case "NOT_FOUND" -> ResumeErrorCode.VACANCY_NOT_FOUND;
                case "RATE_LIMITED" -> ResumeErrorCode.VACANCY_RATE_LIMITED;
                case "TIMEOUT" -> ResumeErrorCode.VACANCY_PROVIDER_TIMEOUT;
                default -> ResumeErrorCode.VACANCY_PROVIDER_FAILED;
            };
            throw new ResumeAnalysisException(code, exception.getMessage(), exception);
        } catch (ResumeAnalysisException exception) {
            log.warn("resume analysis failed analysisId={} category={}", analysisId, exception.getCode());
            throw exception;
        } catch (RuntimeException exception) {
            log.error("resume analysis failed analysisId={} category={}", analysisId, ResumeErrorCode.ANALYSIS_FAILED);
            throw new ResumeAnalysisException(ResumeErrorCode.ANALYSIS_FAILED, "Resume analysis failed", exception);
        }
    }

    private ResumeAnalysisOutcome analyze(ResumeDocument document, AiProviderType requestedProvider,
            ResumeAnalysisProfile requestedProfile, VacancyMarketRequest vacancyAnalysis,
            String analysisId, Instant started) {
        byte[] pdf = fileValidator.validate(document);
        log.info("pdf validated analysisId={} fileSize={}", analysisId, pdf.length);
        String text = textExtractor.extract(pdf);
        log.info("pdf text extracted analysisId={} textLength={}", analysisId, text.length());
        if (text.length() > properties.maxTextLength()) {
            throw new ResumeAnalysisException(ResumeErrorCode.RESUME_TEXT_TOO_LARGE,
                    "Extracted resume text exceeds the configured size limit");
        }
        AiProvider aiProvider = aiProviderRegistry.get(requestedProvider);
        ResumeAnalysisProfile profile = requestedProfile == null
                ? ResumeAnalysisProfile.defaultProfile() : requestedProfile;
        var resolvedMarketProfile = marketProfileProvider.resolve(profile);
        MarketAnalysisProfile marketProfile = resolvedMarketProfile.profile();
        VacancyMarketRequest.Mode mode = vacancyAnalysis == null
                ? VacancyMarketRequest.Mode.AUTO_MARKET : vacancyAnalysis.mode();
        VacancyMarketData market = mode == VacancyMarketRequest.Mode.AUTO_MARKET
                ? marketService.load(profile, marketProfile.targetRole())
                : marketService.load(profile, vacancyAnalysis, marketProfile.targetRole());
        if (mode == VacancyMarketRequest.Mode.SINGLE_VACANCY) {
            marketProfile = marketProfile.enrichedWith(market);
        }
        log.info("market context resolved analysisId={} source={} sampleSize={}",
                analysisId, market.source(), market.sampleSize());
        LlmResumeAnalysisResponse llm = aiProvider.analyze(marketProfile, text, market);
        log.info("AI request completed analysisId={} provider={}", analysisId, aiProvider.type());
        List<EmploymentPeriod> periods = llmValidator.validateAndConvert(llm, marketProfile);
        log.info("LLM response validated analysisId={}", analysisId);

        var analysis = analysisEngine.analyze(marketProfile, llm, periods, text, market,
                mode == VacancyMarketRequest.Mode.SINGLE_VACANCY);
        Instant completed = clock.instant();
        log.info("deterministic calculations completed analysisId={} ats={} overall={}", analysisId,
                analysis.atsScore(), analysis.overallScore());
        ResumeAnalysisOutcome result = assembler.assemble(marketProfile, llm, analysis.experience(),
                analysis.technologies(), market, analysis.commercialScore(), analysis.atsScore(),
                analysis.overallScore(), analysis.candidateStrength(), analysis.level(), analysis.hrChance(),
                analysis.technicalChance(), completed, aiProvider.type(), aiProvider.model().orElse(null),
                resolvedMarketProfile.source(), analysis.details());
        log.info("resume analysis completed analysisId={} durationMs={}", analysisId,
                Duration.between(started, completed).toMillis());
        return result;
    }

}
