package com.ororura.analyzer.analysis;

import java.util.*;
import org.junit.jupiter.api.Test;
import com.ororura.analyzer.analysis.domain.*;
import com.ororura.analyzer.analysis.market.*;
import com.ororura.analyzer.vacancy.search.*;
import com.ororura.analyzer.vacancy.model.Vacancy;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class GradeAndSegmentTests {
    @Test void gradesAreExplicitAndAmbiguousTitlesStayUnknown() {
        var policy=new GradePolicy();
        assertThat(policy.classify("Junior+ Java Developer").vacancyGrade()).isEqualTo(CandidateGrade.JUNIOR_PLUS);
        assertThat(policy.classify("Middle plus Go backend").vacancyGrade()).isEqualTo(CandidateGrade.MIDDLE_PLUS);
        assertThat(policy.classify("Junior / Middle Java").vacancyGrade()).isNull();
        assertThat(policy.classify("Java developer, 3–6 years").vacancyGrade()).isNull();
        assertThat(policy.match(CandidateGrade.JUNIOR,null)).isEqualTo(GradePolicy.Match.UNKNOWN);
        assertThat(policy.detect(0,90,9,90)).isEqualTo(CandidateGrade.INTERN);
        assertThat(policy.detect(80,95,9,90)).isEqualTo(CandidateGrade.SENIOR);
        assertThat(policy.rank(CandidateGrade.MIDDLE)-policy.rank(CandidateGrade.JUNIOR_PLUS)).isEqualTo(1);
    }
    @Test void paginatesDeduplicatesAndFiltersGradesAfterSearch() {
        var query=mock(VacancyQueryService.class);
        var strategy=mock(VacancySearchStrategy.class);
        var segment=new MarketSegment(CareerDirection.BACKEND,"Go",CandidateGrade.JUNIOR,List.of(),MarketFilters.defaults(),CandidateGrade.JUNIOR);
        var criteria=new GenericVacancySearchStrategy().plan(segment).getFirst();
        when(strategy.plan(segment)).thenReturn(List.of(criteria,criteria));
        when(strategy.matchesDirection(eq(segment),any())).thenReturn(true);
        when(query.searchDomain(any())).thenAnswer(inv -> {
            VacancySearchCriteria c=inv.getArgument(0);
            assertThat(c.pageSize()).isEqualTo(50);
            var items=c.page()==0 ? java.util.stream.IntStream.range(0,50).mapToObj(i -> vacancy(i,"Junior Go Backend")).toList()
                    : List.of(vacancy(50,"Junior Go Backend"),vacancy(0,"Junior Go Backend"),vacancy(51,"Senior Go Backend"));
            return new VacancyQueryService.DomainSearchResult(items,2,53L,c.page()==0,List.of(),c);
        });
        var result=new MarketSegmentCollector(query,strategy).collect(segment);
        assertThat(result.vacancies()).hasSize(51);
        assertThat(result.uniqueCount()).isEqualTo(52);
        assertThat(result.truncated()).isFalse();
        verify(query,times(4)).searchDomain(any());
    }
    @Test void specializationMatchingDoesNotConfuseJavaWithJavascript() {
        var segment=new MarketSegment(CareerDirection.BACKEND,"Java",CandidateGrade.JUNIOR,List.of(),MarketFilters.defaults(),CandidateGrade.JUNIOR);
        assertThat(new GenericVacancySearchStrategy().matchesDirection(segment,vacancy(1,"Junior JavaScript backend"))).isFalse();
        assertThat(new GenericVacancySearchStrategy().matchesDirection(segment,vacancy(1,"Junior Java backend"))).isTrue();
    }
    @Test void copiesPresetDefaultsWithoutEncodingCombinations() {
        var java=AnalysisPreset.legacy(com.ororura.analyzer.resume.ai.ResumeAnalysisProfile.JAVA_BACKEND);
        var react=AnalysisPreset.legacy(com.ororura.analyzer.resume.ai.ResumeAnalysisProfile.REACT_FRONTEND);
        assertThat(java.direction()).isEqualTo(CareerDirection.BACKEND);
        assertThat(react.specialization()).isEqualTo("React");
        var segment=new MarketSegment(CareerDirection.BACKEND,"Go",CandidateGrade.MIDDLE,List.of("Go","go"),MarketFilters.defaults(),CandidateGrade.MIDDLE);
        assertThat(segment.technologies()).containsExactly("go");
        assertThat(new GenericVacancySearchStrategy().plan(segment)).allSatisfy(c -> {
            assertThat(c.pageSize()).isEqualTo(50); assertThat(c.level()).isNull();
        });
    }
    @Test void scheduledSyncIncludesCustomPlansAndKeepsPageSizeBounded() {
        var provider=mock(com.ororura.analyzer.vacancy.provider.VacancyProvider.class);
        var persistence=mock(com.ororura.analyzer.vacancy.sync.VacancySyncPersistenceService.class);
        var cache=mock(com.ororura.analyzer.vacancy.cache.VacancyCacheFacade.class);
        var version=mock(com.ororura.analyzer.vacancy.cache.VacancyMarketVersionService.class);
        var refresh=mock(com.ororura.analyzer.vacancy.requirement.generation.MarketProfileRefreshCoordinator.class);
        var legacy=mock(com.ororura.analyzer.resume.ai.ResumeAnalysisProfileRegistry.class);
        var source=mock(ProfileMarketQuerySource.class);
        var segment=new MarketSegment(CareerDirection.BACKEND,"Go",CandidateGrade.JUNIOR,List.of(),MarketFilters.defaults(),CandidateGrade.JUNIOR);
        var plan=new GenericVacancySearchStrategy().plan(segment).getFirst();
        when(legacy.all()).thenReturn(List.of());
        when(source.nextPlans()).thenReturn(List.of(plan));
        when(provider.search(any())).thenReturn(new com.ororura.analyzer.vacancy.provider.VacancyProviderSearchResult(
                List.of(),1,0L,false,List.of()));
        var properties=new com.ororura.analyzer.vacancy.sync.VacancySyncProperties(true,java.time.Duration.ofMinutes(30),
                java.time.Duration.ZERO,"legacy",null,4,200,50);
        new com.ororura.analyzer.vacancy.sync.VacancySyncService(provider,persistence,properties,cache,version,refresh,legacy,source).synchronize();
        verify(provider).search(argThat(c -> c.pageSize()==50 && c.query().contains("Go")));
    }
    static Vacancy vacancy(int id,String title) {
        return new Vacancy("hh-"+id,""+id,title,"Company",null,null,"Moscow",null,"backend",
                List.of("Go"),List.of(),List.of(),"1–3 года","full",null,"REMOTE","hh.ru",null,null);
    }
}
