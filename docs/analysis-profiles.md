# Пользовательские профили анализа

## Граница архитектуры

Новый backend-flow:

`AnalysisPreset → UserAnalysisProfile → MarketSegment → VacancySearchStrategy → local vacancies → VacancyRequirementPipeline → unweighted criterion groups → MarketSnapshot → EffectiveAnalysisConfig → ResumeAnalysisService → AnalysisRun`

`AnalysisPreset` копирует начальные значения. Сохранённый профиль не зависит от последующих изменений preset. `ResumeAnalysisProfile` остаётся legacy enum; комбинации направления, специализации и грейда в enum не добавляются.

Профиль хранит пользовательские ограничения. `MarketSnapshot` хранит факты выбранных вакансий, их грейды и происхождение классификации, частоты требований, связи требований с вакансиями, группы критериев и параметры получения рынка. В snapshot нет scoring weights. `EffectiveAnalysisConfigResolver` рассчитывает веса групп по покрытию вакансий и нормализует их в Java; копирует действующие параметры `ResumeScoringProperties` в отдельную `ScoringPolicy`. Последующие детерминированные расчёты используют закреплённые параметры.

`MarketAnalysisProfile` остаётся совместимым адаптером для существующего LLM/Java-анализатора. В полном конструкторе discriminator `profile` может быть `null` для custom-конфигурации; короткий legacy-конструктор по-прежнему требует enum. CUSTOM не маскируется под JAVA_BACKEND. Источник результата нового flow — `SNAPSHOT`.

## HTTP-контракт

Владение профилями и запусками определяется UUID в серверной HTTP-сессии. `ownerId` не принимается извне. Нужно сохранять cookie сессии между запросами. Между разрешёнными CORS origins поддерживаются credentials, `ETag`, `Location`, `X-Analysis-Run-Id`. Без учётных записей потеря/истечение сессии означает потерю доступа к ранее созданным профилям; сами строки БД сохраняются.

| Метод | Endpoint | Результат |
|---|---|---|
| POST | `/api/analysis-profiles` | 201, профиль, Location, ETag |
| GET | `/api/analysis-profiles?page=0&size=20` | Список активных профилей текущей сессии, размер 1–50 |
| GET | `/api/analysis-profiles/{id}` | 200, профиль, ETag; 404 также для чужого профиля |
| PUT | `/api/analysis-profiles/{id}` | Полная замена настроек, 200, новый ETag |
| DELETE | `/api/analysis-profiles/{id}` | Soft delete, 204 |
| POST | `/api/resume/analyze?profileId={id}&provider=POLZA` | multipart `file`, синхронный AnalysisRun с result/effectiveConfig |
| GET | `/api/analysis-runs/{id}` | Статус, результат и закреплённая конфигурация своего запуска |

PUT/DELETE требуют `If-Match: "0"` (значение из актуального ETag). Отсутствующий заголовок: 428; конфликт версий: 412. PATCH намеренно отсутствует: PUT не имеет неоднозначности omitted/null. Для PUT обязательны name, direction, specialization, targetGrade, technologies. Отсутствующие marketFilters сбрасываются к значениям по умолчанию; preset не переопределяет существующий профиль, его происхождение менять нельзя.

Пример создания:

```json
{
  "name": "Go backend — junior",
  "direction": "BACKEND",
  "specialization": "Go",
  "targetGrade": "JUNIOR",
  "technologies": ["Go", "PostgreSQL"],
  "marketFilters": {
    "location": "Москва",
    "employment": ["Полная занятость"],
    "workFormat": "REMOTE",
    "includeUnknownGrade": false
  }
}
```

Создание из preset: `{"name":"Java", "preset":"JAVA_BACKEND"}`. Defaults: Java/BACKEND/JUNIOR/[Java, Spring Boot] или React/FRONTEND/JUNIOR/[React, TypeScript]. Переданные явно настройки перекрывают defaults только при создании. Название обязательно; specialization — непустая строка, не комбинированный enum. Технологии канонизируются по регистру, сортируются и дедуплицируются.

Дополнительные фильтры: employer, schedule, salaryFrom/salaryTo, currency, salaryOnly, publishedFrom, searchGrade. `technologies` сейчас означает ALL-ограничение для поиска вакансий, а не повышающие scoring коэффициенты. Пустой список отключает это ограничение.

Старый multipart-контракт `profile=JAVA_BACKEND|REACT_FRONTEND`, provider и part `analysis` продолжает работать и возвращает прежний ResumeAnalysisResult. Новый путь с profileId возвращает AnalysisRun и поддерживает AUTO_MARKET; комбинации profileId с legacy profile или SELECTED_VACANCIES/SINGLE_VACANCY отвергаются. Старые режимы выбора вакансий доступны через legacy API. Новые запросы не игнорируют выбор режима молча.

Ошибка после создания запуска сохраняет FAILED; `X-Analysis-Run-Id` позволяет получить его состояние. Если ошибка возникла до разрешения конфигурации, effectiveConfig может отсутствовать. Ошибки загрузки до создания запуска возвращаются в существующем формате.

## Грейды

Единая новая шкала: INTERN, JUNIOR, JUNIOR_PLUS, MIDDLE, MIDDLE_PLUS, SENIOR.

- targetGrade — пользовательская цель сравнения.
- detectedGrade — результат Java GradePolicy; targetGrade не является входом детектора. Используются месяцы опыта, semantic technical/responsibility assessments и вычисленный overall. Это контекстная оценка по выбранным критериям, а не независимая от рынка аттестация кандидата.
- vacancyGrade — классификация конкретного заголовка вакансии; отдельно фиксируется в snapshot. В vacancy API вычисляется при чтении, новая изменяемая колонка вакансии не требуется.
- searchGrade — фильтр выборки, по умолчанию равен targetGrade; может быть задан отдельно в marketFilters. Он не меняет targetGrade.

GradePolicy использует явное ранжирование, без ordinal(). Диапазон стажа 1–3/3–6 лет не приравнивается к точному vacancyGrade. Неизвестные и неоднозначные заголовки (например Junior/Middle) дают null, исключаются по умолчанию, могут быть включены через includeUnknownGrade. Точное несовпадение грейда исключается всегда.

Legacy detectedLevel сохраняется. Для его дополнительного представления detectedGrade прежний MIDDLE_MINUS консервативно отображается в JUNIOR_PLUS. Новые запуски используют шестиступенчатую GradePolicy непосредственно; MIDDLE_MINUS не генерируется. При отсутствии подтверждённых месяцев опыта новый детектор выдаёт INTERN. Старые формулы и ответы legacy-пути не переписаны.

## Выборка рынка и синхронизация

GenericVacancySearchStrategy формирует несколько запросов (grade + specialization, specialization + direction, варианты технологий, широкий specialization-запрос). Результаты объединяются по ID, а не складываются как независимые выборки. Общие фильтры применяются в VacancySpecifications, дополнительно проверяются направление, лексическое совпадение специализации и GradePolicy. Go/Golang — явный alias; Java не совпадает с JavaScript.

Collector читает по 50 строк, round-robin по запросам, максимум четыре страницы/200 строк на запрос и 200 принятых вакансий на snapshot. Ограничение выдачи и нехватка выборки отражаются в warnings. Сортировка имеет стабильный дополнительный ключ id. Offset pagination не является repeatable-read снимком изменяющейся БД: во время конкурентной синхронизации возможны пропуски, но принятые вакансии и их содержимое закреплены навсегда в snapshot.

Внешнее пополнение выполняет существующий VacancySyncService. К legacy-поискам добавлены планы активных пользовательских профилей; ProfileMarketQuerySource ротирует страницы по 50 профилей за цикл, дедуплицирует сегменты и запросы. HTTP-анализ использует локальную БД и не ждёт HH. Новый профиль получит новые внешние вакансии после очередного успешного sync; при выключенном `app.vacancy.sync.enabled` или недоступном HH выборка может быть пустой. Возможности provider-фильтров зависят от существующего HH адаптера; локальная фильтрация остаётся обязательной.

Извлечение требований и генерация смысловых групп используют прежние Polza gateways независимо от выбранного resume provider. Формулы и веса LLM не вычисляет. Ошибки extraction учитываются в processed/failed count. При недоступной генерации групп применяется явно отмеченная детерминированная группировка по типу требования. Пустой рынок не заменяется выдуманными частотами: используется один общий критерий роли с нулевой market frequency, market-fit при отсутствии требований не оценивается.

## Persistence и воспроизводимость

Flyway V3 добавляет:

| Таблица | Содержимое |
|---|---|
| analysis_profiles | UUID, owner UUID, @Version, пользовательские настройки, timestamps, deleted_at; market_filters JSONB |
| analysis_profile_technologies | Упорядоченные нормализованные технологии, FK и unique |
| market_snapshots | Неизменяемый segment, версия и provenance pipeline, исходные вакансии/grade evidence, mappings, счётчики, warnings |
| market_snapshot_requirements | Нормализованные требования, raw counts и frequencies |
| market_snapshot_criteria | Группы без весов; requirement IDs JSONB |
| analysis_runs | Статус, profile ID/revision, immutable profile/config JSONB, snapshot FK, result/LLM response JSONB, SHA-256 PDF, текст и точный prompt/schema |

Нормализованы данные, по которым нужны ограничения, связи и запросы. JSONB используется для вложенных immutable агрегатов и схемы конфигурации; runtime services, MultipartFile, JPA proxy и enum-registry не сериализуются.

Run фиксирует профиль конкретной версии и UTC evaluationTime, provider/model, prompt version, scoring algorithm/grade policy versions, все конфигурационные scoring коэффициенты. LlmResponseValidator использует закреплённую дату. Сохраняются извлечённый текст, semantic response, system instruction, prompt input и JSON schema. Это позволяет проверять детерминированные вычисления на тех же фактах; повторный вызов внешней модели не гарантирует идентичный ответ. Версии алгоритмов нужно повышать при изменении формул и сохранять соответствующий код для исторического replay.

Транзакции ограничены операциями БД: PREPARING → RUNNING с атомарной записью snapshot/config → COMPLETED либо FAILED. PDF/LLM/HH не выполняются внутри транзакции. @Version защищает изменения профилей и run state. PostgreSQL triggers запрещают обновление market snapshot и закреплённой конфигурации/provenance запуска. Soft delete профиля не удаляет историю. Текст резюме и LLM-response являются персональными данными; срок хранения и будущая привязка к постоянным аккаунтам требуют отдельного продуктового решения.

Миграция добавочная: V1/V2 не изменены; существующие вакансии и legacy API не требуют backfill. Старые in-memory MarketAnalysisProfile не объявляются пользовательскими профилями и не записываются автоматически как реальные market snapshots.

Текущий execution синхронный. Очередь, idempotency и автоматическое восстановление RUNNING после аварийного завершения процесса не добавлены; такие запуски остаются доступными в истории как незавершённые. История изменения профиля между анализами отдельно не хранится — версия и содержимое профиля закрепляются в каждом запуске.

## Проверка

Обычный прогон: `./gradlew test --offline`.

Для дополнительного настоящего PostgreSQL-прогона задать `GRAPHIFY_TEST_POSTGRES_URL=jdbc:postgresql://127.0.0.1:55439/graphify_test` для отдельной тестовой БД с пользователем postgres, затем выполнить тот же task. PostgresProfileFlowIntegrationTests применяет Flyway и JPA validate и повторяет HTTP-flow; без переменной этот класс пропускается. Не направлять тесты на рабочую БД.

Новые тесты покрывают Java/React/Go, более одной страницы рынка, фильтры/дедупликацию, независимость target/search/detected grade, CRUD и ETag, session isolation, history после update/delete, неуспешные запуски, CORS и PostgreSQL immutable triggers. Внешние AI endpoints заменены тестовыми doubles; реальные платные LLM/HH-вызовы не выполняются.

Проверка реализации: полный прогон на H2 и изолированном PostgreSQL — 199 tests, 0 failures, 0 errors, 0 skipped.

## Изменённые и добавленные файлы

- [docs/analysis-profiles.md](/Users/egorgladkikh/Desktop/analyzer-java/docs/analysis-profiles.md)
- [src/main/java/com/ororura/analyzer/analysis/api/AnalysisProfileController.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/analysis/api/AnalysisProfileController.java)
- [src/main/java/com/ororura/analyzer/analysis/api/ProfileAnalysisController.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/analysis/api/ProfileAnalysisController.java)
- [src/main/java/com/ororura/analyzer/analysis/api/ProfileRequest.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/analysis/api/ProfileRequest.java)
- [src/main/java/com/ororura/analyzer/analysis/api/ProfileSession.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/analysis/api/ProfileSession.java)
- [src/main/java/com/ororura/analyzer/analysis/application/AnalysisProfileService.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/analysis/application/AnalysisProfileService.java)
- [src/main/java/com/ororura/analyzer/analysis/application/AnalysisRunStore.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/analysis/application/AnalysisRunStore.java)
- [src/main/java/com/ororura/analyzer/analysis/application/EffectiveAnalysisConfigResolver.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/analysis/application/EffectiveAnalysisConfigResolver.java)
- [src/main/java/com/ororura/analyzer/analysis/application/ProfileAnalysisService.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/analysis/application/ProfileAnalysisService.java)
- [src/main/java/com/ororura/analyzer/analysis/domain/AnalysisPreset.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/analysis/domain/AnalysisPreset.java)
- [src/main/java/com/ororura/analyzer/analysis/domain/AnalysisRun.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/analysis/domain/AnalysisRun.java)
- [src/main/java/com/ororura/analyzer/analysis/domain/CandidateGrade.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/analysis/domain/CandidateGrade.java)
- [src/main/java/com/ororura/analyzer/analysis/domain/CareerDirection.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/analysis/domain/CareerDirection.java)
- [src/main/java/com/ororura/analyzer/analysis/domain/EffectiveAnalysisConfig.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/analysis/domain/EffectiveAnalysisConfig.java)
- [src/main/java/com/ororura/analyzer/analysis/domain/GradePolicy.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/analysis/domain/GradePolicy.java)
- [src/main/java/com/ororura/analyzer/analysis/domain/MarketFilters.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/analysis/domain/MarketFilters.java)
- [src/main/java/com/ororura/analyzer/analysis/domain/MarketSegment.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/analysis/domain/MarketSegment.java)
- [src/main/java/com/ororura/analyzer/analysis/domain/MarketSnapshot.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/analysis/domain/MarketSnapshot.java)
- [src/main/java/com/ororura/analyzer/analysis/domain/ScoringPolicy.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/analysis/domain/ScoringPolicy.java)
- [src/main/java/com/ororura/analyzer/analysis/domain/UserAnalysisProfile.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/analysis/domain/UserAnalysisProfile.java)
- [src/main/java/com/ororura/analyzer/analysis/market/GenericVacancySearchStrategy.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/analysis/market/GenericVacancySearchStrategy.java)
- [src/main/java/com/ororura/analyzer/analysis/market/MarketSegmentCollector.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/analysis/market/MarketSegmentCollector.java)
- [src/main/java/com/ororura/analyzer/analysis/market/MarketSnapshotBuilder.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/analysis/market/MarketSnapshotBuilder.java)
- [src/main/java/com/ororura/analyzer/analysis/market/ProfileMarketQuerySource.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/analysis/market/ProfileMarketQuerySource.java)
- [src/main/java/com/ororura/analyzer/analysis/market/VacancySearchStrategy.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/analysis/market/VacancySearchStrategy.java)
- [src/main/java/com/ororura/analyzer/analysis/persistence/AnalysisProfileEntity.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/analysis/persistence/AnalysisProfileEntity.java)
- [src/main/java/com/ororura/analyzer/analysis/persistence/AnalysisProfileRepository.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/analysis/persistence/AnalysisProfileRepository.java)
- [src/main/java/com/ororura/analyzer/analysis/persistence/AnalysisRunEntity.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/analysis/persistence/AnalysisRunEntity.java)
- [src/main/java/com/ororura/analyzer/analysis/persistence/AnalysisRunRepository.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/analysis/persistence/AnalysisRunRepository.java)
- [src/main/java/com/ororura/analyzer/analysis/persistence/MarketSnapshotEntity.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/analysis/persistence/MarketSnapshotEntity.java)
- [src/main/java/com/ororura/analyzer/analysis/persistence/MarketSnapshotRepository.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/analysis/persistence/MarketSnapshotRepository.java)
- [src/main/java/com/ororura/analyzer/api/ApiExceptionHandler.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/api/ApiExceptionHandler.java)
- [src/main/java/com/ororura/analyzer/config/WebConfig.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/config/WebConfig.java)
- [src/main/java/com/ororura/analyzer/resume/ai/AiProvider.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/resume/ai/AiProvider.java)
- [src/main/java/com/ororura/analyzer/resume/ai/CodexCliAiProvider.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/resume/ai/CodexCliAiProvider.java)
- [src/main/java/com/ororura/analyzer/resume/ai/DefaultAnalysisTechnologyCatalog.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/resume/ai/DefaultAnalysisTechnologyCatalog.java)
- [src/main/java/com/ororura/analyzer/resume/ai/PolzaAiProvider.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/resume/ai/PolzaAiProvider.java)
- [src/main/java/com/ororura/analyzer/resume/ai/ResumeAnalysisPromptFactory.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/resume/ai/ResumeAnalysisPromptFactory.java)
- [src/main/java/com/ororura/analyzer/resume/api/GradeFitAnalysis.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/resume/api/GradeFitAnalysis.java)
- [src/main/java/com/ororura/analyzer/resume/api/ResumeAnalysisController.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/resume/api/ResumeAnalysisController.java)
- [src/main/java/com/ororura/analyzer/resume/api/ResumeAnalysisResult.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/resume/api/ResumeAnalysisResult.java)
- [src/main/java/com/ororura/analyzer/resume/application/DeterministicAnalysisEngine.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/resume/application/DeterministicAnalysisEngine.java)
- [src/main/java/com/ororura/analyzer/resume/application/ResumeAnalysisService.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/resume/application/ResumeAnalysisService.java)
- [src/main/java/com/ororura/analyzer/resume/config/ResumeScoringProperties.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/resume/config/ResumeScoringProperties.java)
- [src/main/java/com/ororura/analyzer/resume/domain/GradeFitScorer.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/resume/domain/GradeFitScorer.java)
- [src/main/java/com/ororura/analyzer/resume/domain/SkillNormalizer.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/resume/domain/SkillNormalizer.java)
- [src/main/java/com/ororura/analyzer/resume/market/MarketAnalysisProfile.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/resume/market/MarketAnalysisProfile.java)
- [src/main/java/com/ororura/analyzer/resume/market/MarketProfileSource.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/resume/market/MarketProfileSource.java)
- [src/main/java/com/ororura/analyzer/vacancy/model/Vacancy.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/vacancy/model/Vacancy.java)
- [src/main/java/com/ororura/analyzer/vacancy/model/VacancySearchItem.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/vacancy/model/VacancySearchItem.java)
- [src/main/java/com/ororura/analyzer/vacancy/requirement/MarketRequirementStatistics.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/vacancy/requirement/MarketRequirementStatistics.java)
- [src/main/java/com/ororura/analyzer/vacancy/requirement/VacancyRequirementPipeline.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/vacancy/requirement/VacancyRequirementPipeline.java)
- [src/main/java/com/ororura/analyzer/vacancy/requirement/generation/MarketCriterionGenerator.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/vacancy/requirement/generation/MarketCriterionGenerator.java)
- [src/main/java/com/ororura/analyzer/vacancy/requirement/generation/MarketProfileRefreshCoordinator.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/vacancy/requirement/generation/MarketProfileRefreshCoordinator.java)
- [src/main/java/com/ororura/analyzer/vacancy/search/VacancySearchService.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/vacancy/search/VacancySearchService.java)
- [src/main/java/com/ororura/analyzer/vacancy/sync/VacancySyncService.java](/Users/egorgladkikh/Desktop/analyzer-java/src/main/java/com/ororura/analyzer/vacancy/sync/VacancySyncService.java)
- [src/main/resources/application.yml](/Users/egorgladkikh/Desktop/analyzer-java/src/main/resources/application.yml)
- [src/main/resources/db/migration/V3__user_analysis_profiles.sql](/Users/egorgladkikh/Desktop/analyzer-java/src/main/resources/db/migration/V3__user_analysis_profiles.sql)
- [src/test/java/com/ororura/analyzer/analysis/GradeAndSegmentTests.java](/Users/egorgladkikh/Desktop/analyzer-java/src/test/java/com/ororura/analyzer/analysis/GradeAndSegmentTests.java)
- [src/test/java/com/ororura/analyzer/analysis/PostgresProfileFlowIntegrationTests.java](/Users/egorgladkikh/Desktop/analyzer-java/src/test/java/com/ororura/analyzer/analysis/PostgresProfileFlowIntegrationTests.java)
- [src/test/java/com/ororura/analyzer/analysis/ProfileFlowIntegrationTests.java](/Users/egorgladkikh/Desktop/analyzer-java/src/test/java/com/ororura/analyzer/analysis/ProfileFlowIntegrationTests.java)
