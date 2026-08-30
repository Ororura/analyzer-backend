package com.ororura.analyzer.resume.ai.profile;

import java.util.List;

import com.ororura.analyzer.resume.ai.AnalysisCriterion;
import com.ororura.analyzer.resume.ai.AnalysisTechnology;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfile;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfileDefinition;
import org.springframework.stereotype.Component;

@Component
public class ReactFrontendAnalysisProfile implements ResumeAnalysisProfileDefinition {

    @Override public ResumeAnalysisProfile profile() { return ResumeAnalysisProfile.REACT_FRONTEND; }
    @Override public String targetRole() { return "React Frontend Developer"; }

    @Override
    public List<AnalysisCriterion> criteria() {
        return List.of(
                criterion("javascriptDepth", "JavaScript", "подтверждённая практическая работа с современным JavaScript: ES6+, promises, async/await, modules, работа с объектами/массивами и browser/event-driven logic. Одного упоминания JavaScript в Skills недостаточно для высокого score", .10),
                criterion("typescriptDepth", "TypeScript", "подтверждённое практическое использование TypeScript: типы, interfaces/types, generics, narrowing, utility types и моделирование данных. Не повышай score только за наличие TypeScript в списке технологий", .12),
                criterion("reactDepth", "React", "подтверждённая работа с React: components, props/state, hooks, forms, composition, rendering behavior, работа с API и состоянием интерфейса. Высокий score требует конкретных React-задач", .18),
                criterion("frontendDepth", "Frontend", "разработка клиентских приложений: UI logic, API, формы, routing, auth flows, error/loading states, responsive behavior, browser interactions и production frontend-задачи. Title Frontend Developer сам по себе не является evidence", .15),
                criterion("htmlCssDepth", "HTML/CSS", "практическая работа с semantic HTML, responsive layout, Flexbox/Grid, адаптивностью и доступностью. Tailwind, Sass или CSS Modules являются реализациями, но ни одна конкретная CSS-технология не обязательна", .10),
                criterion("stateManagementDepth", "State management", "практическая работа с React Context, Redux Toolkit, Zustand, MobX или аналогичными подходами. Не штрафуй только за отсутствие Redux, если используются другие адекватные решения", .10),
                criterion("webPlatformDepth", "Web platform", "подтверждённое применение HTTP, REST, fetch/axios, cookies, storage, CORS, WebSocket, базовых XSS/CSRF аспектов и взаимодействия frontend с backend. Оценивай только явно подтверждённые задачи", .10),
                criterion("testingDepth", "Testing", "unit/component/integration/e2e testing с Jest, Vitest, React Testing Library, Playwright, Cypress или аналогами. Название библиотеки без описания тестовых задач является слабым evidence", .07),
                criterion("frontendArchitectureDepth", "Frontend architecture", "организация frontend-кода и архитектурная ответственность: component composition, separation of concerns, reusable abstractions, feature/module boundaries, FSD или другие явно описанные подходы. Не считай FSD обязательным стандартом", .08));
    }

    @Override
    public List<AnalysisTechnology> technologies() {
        return List.of(
                tech("JavaScript", AnalysisTechnology.Tier.CORE, "\\bjavascript\\b|\\bjs\\b"),
                tech("TypeScript", AnalysisTechnology.Tier.CORE, "\\btypescript\\b|\\bts\\b"),
                tech("React", AnalysisTechnology.Tier.CORE, "\\breact(?:\\.js|js)?\\b"),
                tech("HTML", AnalysisTechnology.Tier.CORE, "\\bhtml5?\\b"),
                tech("CSS", AnalysisTechnology.Tier.CORE, "\\bcss3?\\b"),
                tech("REST API", AnalysisTechnology.Tier.CORE, "\\brest(?:ful)?(?:\\s+api)?\\b|\\bweb api\\b"),
                tech("Git", AnalysisTechnology.Tier.CORE, "\\bgit\\b"),
                tech("Frontend development", AnalysisTechnology.Tier.CORE, "front[ -]?end(?: development)?"),
                tech("State management", AnalysisTechnology.Tier.COMMON, "state management|react context|\\bredux(?: toolkit)?\\b|\\bzustand\\b|\\bmobx\\b"),
                tech("Testing", AnalysisTechnology.Tier.COMMON, "\\bjest\\b|\\bvitest\\b|react testing library|\\bplaywright\\b|\\bcypress\\b|unit test|integration test|e2e"),
                tech("Responsive UI", AnalysisTechnology.Tier.COMMON, "responsive|adaptive layout|адаптив"),
                tech("Accessibility", AnalysisTechnology.Tier.COMMON, "accessibility|\\ba11y\\b|доступност"),
                tech("Next.js", AnalysisTechnology.Tier.BONUS, "\\bnext(?:\\.js|js)?\\b"),
                tech("Tailwind", AnalysisTechnology.Tier.BONUS, "\\btailwind(?:css)?\\b"),
                tech("Sass", AnalysisTechnology.Tier.BONUS, "\\bsass\\b|\\bscss\\b"),
                tech("Webpack/Vite", AnalysisTechnology.Tier.BONUS, "\\bwebpack\\b|\\bvite\\b"),
                tech("Docker", AnalysisTechnology.Tier.BONUS, "\\bdocker\\b"),
                tech("WebSocket", AnalysisTechnology.Tier.BONUS, "websockets?|socket\\.io"));
    }

    private static AnalysisCriterion criterion(String id, String label, String description, double weight) {
        return new AnalysisCriterion(id, label, description, weight);
    }

    private static AnalysisTechnology tech(String name, AnalysisTechnology.Tier tier, String... patterns) {
        return new AnalysisTechnology(name, tier, List.of(patterns));
    }
}
