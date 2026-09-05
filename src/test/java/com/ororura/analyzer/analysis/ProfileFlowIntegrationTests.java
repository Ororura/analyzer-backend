package com.ororura.analyzer.analysis;

import java.io.ByteArrayOutputStream;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.*;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;
import com.ororura.analyzer.analysis.domain.*;
import com.ororura.analyzer.analysis.application.AnalysisRunStore;
import com.ororura.analyzer.analysis.persistence.*;
import com.ororura.analyzer.resume.ai.*;
import com.ororura.analyzer.resume.market.RequirementType;
import com.ororura.analyzer.vacancy.model.Vacancy;
import com.ororura.analyzer.vacancy.persistence.*;
import com.ororura.analyzer.vacancy.requirement.*;
import com.ororura.analyzer.vacancy.requirement.generation.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileFlowIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired VacancyRepository vacancies;
    @Autowired VacancyNormalizer normalizer;
    @Autowired VacancyContentHasher hasher;
    @Autowired JdbcTemplate jdbc;
    @Autowired AnalysisRunRepository runs;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean PolzaAiProvider provider;
    @MockitoBean VacancyRequirementExtractor extractor;
    @MockitoBean MarketCriterionGenerator generator;
    MockHttpSession session;
    @BeforeEach void setup() {
        session=new MockHttpSession();
        vacancies.deleteAll();
        when(provider.type()).thenReturn(AiProviderType.POLZA);
        when(provider.isAvailable()).thenReturn(true);
        when(provider.model()).thenReturn(Optional.of("test-model"));
        when(extractor.extract(anyList())).thenAnswer(inv -> {
            List<VacancyRequirementInput> inputs=inv.getArgument(0);
            return inputs.stream().map(v -> new ExtractedVacancyRequirements(v.vacancyId(),List.of(
                    new ExtractedRequirement("PostgreSQL",RequirementType.TECHNOLOGY,RequirementImportance.REQUIRED)))).toList();
        });
        when(generator.group(anyString(),any())).thenAnswer(inv -> {
            MarketRequirementStatistics stats=inv.getArgument(1);
            return stats.requirements().isEmpty() ? List.of() : List.of(new GeneratedCriterionGroup("Engineering","Concrete engineering tasks",
                    stats.requirements().stream().map(r -> r.id()).toList()));
        });
        doAnswer(inv -> response(inv.getArgument(0))).when(provider).analyze(any(EffectiveAnalysisConfig.class),anyString());
    }
    @ParameterizedTest @CsvSource({"BACKEND,Java","FRONTEND,React","BACKEND,Go"})
    void profileToSnapshotToAnalysisRun(String direction,String specialization) throws Exception {
        seed(direction,specialization,65);
        String profile=create(direction,specialization);
        var execution=mvc.perform(multipart("/api/resume/analyze").file(pdf(specialization)).param("profileId",profile)
                .param("provider","POLZA").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.targetGrade").value("JUNIOR"))
                .andExpect(jsonPath("$.effectiveConfig.profile.specialization").value(specialization))
                .andExpect(jsonPath("$.result.gradeFit.targetGrade").value("JUNIOR"))
                .andExpect(jsonPath("$.effectiveConfig.market.sampleSize").value(65)).andReturn();
        var run=json.readTree(execution.getResponse().getContentAsString());
        String runId=run.get("id").asText();
        assertThat(run.get("result").get("overallScore").asInt()).isBetween(0,100);
        assertThat(run.get("effectiveConfig").get("scoringPolicy").get("parameters").size()).isGreaterThan(20);
        // Persistence round trip, owner isolation, and profile deletion cannot rewrite the old configuration.
        mvc.perform(get("/api/analysis-runs/"+runId).session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.effectiveConfig.profile.version").value(0));
        mvc.perform(get("/api/analysis-runs/"+runId).session(new MockHttpSession())).andExpect(status().isNotFound());
        mvc.perform(put("/api/analysis-profiles/"+profile).session(session).header("If-Match","\"0\"")
                .contentType(MediaType.APPLICATION_JSON).content(body(direction,specialization).replace("JUNIOR","SENIOR")))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/analysis-profiles/"+profile).session(session).header("If-Match","\"1\""))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/analysis-runs/"+runId).session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.effectiveConfig.profile.specialization").value(specialization));
        assertThat(jdbc.queryForObject("select count(*) from market_snapshot_requirements where snapshot_id = ?",Integer.class,
                UUID.fromString(run.get("effectiveConfig").get("marketSnapshotId").asText()))).isPositive();
        assertThat(jdbc.queryForObject("select extracted_text from analysis_runs where id = ?",String.class,UUID.fromString(runId)))
                .contains(specialization);
    }
    @Test void crudEnforcesOwnershipVersionsValidationAndCopiedDefaults() throws Exception {
        var created=mvc.perform(post("/api/analysis-profiles").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Java preset\",\"preset\":\"JAVA_BACKEND\"}"))
                .andExpect(status().isCreated()).andExpect(header().string("ETag","\"0\""))
                .andExpect(jsonPath("$.specialization").value("Java")).andReturn();
        String id=json.readTree(created.getResponse().getContentAsString()).get("id").asText();
        mvc.perform(get("/api/analysis-profiles").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/analysis-profiles/"+id).session(new MockHttpSession())).andExpect(status().isNotFound());
        String body=body("BACKEND","Go");
        mvc.perform(put("/api/analysis-profiles/"+id).session(session).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isPreconditionRequired());
        mvc.perform(put("/api/analysis-profiles/"+id).session(session).header("If-Match","\"0\"")
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andExpect(header().string("ETag","\"1\""));
        mvc.perform(delete("/api/analysis-profiles/"+id).session(session).header("If-Match","\"0\""))
                .andExpect(status().isPreconditionFailed());
        mvc.perform(post("/api/analysis-profiles").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"direction\":\"BACKEND\"}")).andExpect(status().isBadRequest());
    }
    @Test void failuresAreRecordedAfterConfigHasBeenPinned() throws Exception {
        String id=create("BACKEND","Go");
        doThrow(new IllegalStateException("external failure")).when(provider).analyze(any(EffectiveAnalysisConfig.class),anyString());
        var failure=mvc.perform(multipart("/api/resume/analyze").session(session).file(pdf("Go")).param("profileId",id))
                .andExpect(status().isInternalServerError()).andReturn();
        String runId=failure.getResponse().getHeader("X-Analysis-Run-Id");
        assertThat(runId).isNotBlank();
        mvc.perform(get("/api/analysis-runs/"+runId).session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.effectiveConfig.profile.specialization").value("Go"));
    }
    @Test void targetGradeAndSearchGradeAreIndependent() throws Exception {
        seed("BACKEND","Go",5);
        String request=body("BACKEND","Go").replace("JUNIOR","SENIOR")
                .replace("\"workFormat\":\"REMOTE\"", "\"workFormat\":\"REMOTE\",\"searchGrade\":\"JUNIOR\"");
        var created=mvc.perform(post("/api/analysis-profiles").session(session).contentType(MediaType.APPLICATION_JSON)
                .content(request)).andExpect(status().isCreated()).andReturn();
        String id=json.readTree(created.getResponse().getContentAsString()).get("id").asText();
        mvc.perform(multipart("/api/resume/analyze").session(session).file(pdf("Go")).param("profileId",id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.targetGrade").value("SENIOR"))
                .andExpect(jsonPath("$.detectedGrade").value("JUNIOR_PLUS"))
                .andExpect(jsonPath("$.effectiveConfig.market.sampleSize").value(5))
                .andExpect(jsonPath("$.result.gradeFit.targetGrade").value("SENIOR"));
    }
    @Test void corsSupportsSessionClientsAndExposesVersionHeaders() throws Exception {
        mvc.perform(options("/api/analysis-profiles").header("Origin","http://localhost:5173")
                .header("Access-Control-Request-Method","PUT").header("Access-Control-Request-Headers","If-Match, Content-Type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Credentials","true"));
        mvc.perform(post("/api/analysis-profiles").header("Origin","https://untrusted.example")
                .contentType(MediaType.APPLICATION_JSON).content(body("BACKEND","Go"))).andExpect(status().isForbidden());
    }
    String create(String direction,String specialization) throws Exception {
        var result=mvc.perform(post("/api/analysis-profiles").session(session).contentType(MediaType.APPLICATION_JSON)
                .content(body(direction,specialization))).andExpect(status().isCreated()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
    String body(String direction,String specialization) {
        return "{\"name\":\"Custom\",\"direction\":\""+direction+"\",\"specialization\":\""+specialization
                +"\",\"targetGrade\":\"JUNIOR\",\"technologies\":[\""+specialization+"\"],\"marketFilters\":{\"workFormat\":\"REMOTE\"}}";
    }
    void seed(String direction,String specialization,int count) {
        for(int i=0;i<count+2;i++) {
            String title=(i==count ? "Senior " : "Junior ")+specialization+" "+direction+" Developer";
            var v=new Vacancy("hh-"+i,""+i,title,"Company",null,null,"Moscow",null,title,
                    List.of(specialization),List.of("PostgreSQL"),List.of(),"1–3 года","full",null,
                    i==count+1 ? "OFFICE" : "REMOTE","hh.ru","2026-09-01T00:00:00Z",null);
            var now=OffsetDateTime.now(ZoneOffset.UTC);var content=normalizer.normalize(v);
            var entity=VacancyEntity.create("hh.ru",""+i,now);
            entity.replaceContent(content,hasher.hash(content),normalizer.rawPayload(v),now);vacancies.saveAndFlush(entity);
        }
    }
    static LlmResumeAnalysisResponse response(EffectiveAnalysisConfig config) {
        var score=new LlmResumeAnalysisResponse.SemanticScore(7,List.of("Implemented services"));
        return new LlmResumeAnalysisResponse(config.analysisProfile().criteria().stream()
                .map(c -> new CriterionAssessment(c.id(),7,List.of("Implemented services"))).toList(),
                new LlmResumeAnalysisResponse.ExperienceAssessment(score,score,score),
                new LlmResumeAnalysisResponse.ResumeAssessment(score,score),
                new LlmResumeAnalysisResponse.Skills(config.analysisProfile().requirements().stream().map(r -> r.label()).toList(),List.of(),List.of()),
                List.of(new LlmResumeAnalysisResponse.EmploymentPeriod("Company","Developer",2021,1,2024,12,false)),
                List.of("Implemented services"),List.of(),List.of(),List.of(),List.of());
    }
    static MockMultipartFile pdf(String specialization) throws Exception {
        try(var document=new PDDocument();var bytes=new ByteArrayOutputStream()) {
            var page=new PDPage();document.addPage(page);
            try(var content=new PDPageContentStream(document,page)) {
                content.beginText();content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),12);
                content.newLineAtOffset(30,700);content.showText("Experience: "+specialization+" PostgreSQL. Implemented services at Company 2021-2024. john@example.com");content.endText();
            }
            document.save(bytes);return new MockMultipartFile("file","resume.pdf","application/pdf",bytes.toByteArray());
        }
    }
}
