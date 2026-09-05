package com.ororura.analyzer.analysis.market;

import java.util.*;
import org.springframework.stereotype.Component;
import com.ororura.analyzer.analysis.domain.*;
import com.ororura.analyzer.vacancy.model.Vacancy;
import com.ororura.analyzer.vacancy.search.*;

@Component
public class GenericVacancySearchStrategy implements VacancySearchStrategy {
    public String version() { return "segment-search-v1"; }
    public List<VacancySearchCriteria> plan(MarketSegment s) {
        Set<String> queries=new LinkedHashSet<>();
        String grade=s.searchGrade().name().replace('_',' ');
        queries.add(grade+" "+s.specialization()+" Developer");
        queries.add(s.specialization()+" "+s.direction().name()+" Developer");
        s.technologies().stream().limit(2).forEach(t -> queries.add(s.specialization()+" "+t+" Developer"));
        // Broad final query admits localized titles. Direction and grade still need explicit evidence.
        queries.add(s.specialization());
        var f=s.filters();
        return queries.stream().map(q -> new VacancySearchCriteria(q,null,f.location(),f.employer(),List.of(),
                null,f.workFormat(),f.salaryFrom(),f.salaryTo(),f.currency(),f.salaryOnly(),s.technologies(),
                f.publishedFrom(),VacancySort.DATE,0,50,f.employment(),f.schedule())).toList();
    }
    public boolean matchesDirection(MarketSegment segment,Vacancy vacancy) {
        String title=Objects.toString(vacancy.title(),"").toLowerCase(Locale.ROOT);
        String text=(title+" "+Objects.toString(vacancy.description(),"")).toLowerCase(Locale.ROOT);
        String specialization=segment.specialization().toLowerCase(Locale.ROOT);
        String specialtyTokens=specialization.equals("go") ? "(?:go|golang)" : java.util.regex.Pattern.quote(specialization);
        String evidence=text+" "+String.join(" ",vacancy.skills()).toLowerCase(Locale.ROOT);
        if(!java.util.regex.Pattern.compile("(?<![\\p{L}\\p{N}])"+specialtyTokens+"(?![\\p{L}\\p{N}])")
                .matcher(evidence).find()) return false;
        String words=switch(segment.direction()) {
            case BACKEND -> "backend|back-end|бэкенд|бекенд|spring|quarkus|micronaut|серверн";
            case FRONTEND -> "frontend|front-end|фронтенд|react|vue|angular";
            case MOBILE -> "mobile|android|ios|мобильн|flutter";
            case DEVOPS -> "devops|sre|platform engineer|инфраструктур";
            case QA -> "\\bqa\\b|quality assurance|тестиров|test engineer";
            case DATA -> "data engineer|data analyst|аналитик данных|инженер данных|etl";
            case ML -> "machine learning|\\bml\\b|data scientist|машинн";
        };
        if(segment.direction()==CareerDirection.BACKEND && title.matches(".*(?:android|ios|frontend|front-end|mobile).*")) return false;
        if(segment.direction()==CareerDirection.FRONTEND && title.matches(".*(?:react native|android|ios|mobile).*")) return false;
        return java.util.regex.Pattern.compile(words).matcher(text).find();
    }
}
