package com.ororura.analyzer.vacancy.infrastructure.persistence;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import com.ororura.analyzer.vacancy.domain.Vacancy;
import com.ororura.analyzer.vacancy.application.port.VacancySyncBatchResult;
import com.ororura.analyzer.vacancy.application.port.VacancySyncStore;
import com.ororura.analyzer.vacancy.infrastructure.persistence.VacancyContentHasher;
import com.ororura.analyzer.vacancy.infrastructure.persistence.VacancyEntity;
import com.ororura.analyzer.vacancy.infrastructure.persistence.VacancyNormalizer;
import com.ororura.analyzer.vacancy.infrastructure.persistence.VacancyRepository;
import com.ororura.analyzer.vacancy.infrastructure.persistence.VacancySyncStateEntity;
import com.ororura.analyzer.vacancy.infrastructure.persistence.VacancySyncStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class VacancySyncPersistenceService implements VacancySyncStore {
    static final String SOURCE = "hh.ru";

    private final VacancyRepository repository;
    private final VacancySyncStateRepository stateRepository;
    private final VacancyNormalizer normalizer;
    private final VacancyContentHasher hasher;
    private final Clock clock;

    @Autowired
    public VacancySyncPersistenceService(VacancyRepository repository, VacancySyncStateRepository stateRepository,
            VacancyNormalizer normalizer, VacancyContentHasher hasher) {
        this(repository, stateRepository, normalizer, hasher, Clock.systemUTC());
    }

    VacancySyncPersistenceService(VacancyRepository repository, VacancySyncStateRepository stateRepository,
            VacancyNormalizer normalizer, VacancyContentHasher hasher, Clock clock) {
        this.repository = repository;
        this.stateRepository = stateRepository;
        this.normalizer = normalizer;
        this.hasher = hasher;
        this.clock = clock;
    }

    @Transactional
    public void markStarted() {
        VacancySyncStateEntity state = stateRepository.findById(SOURCE)
                .orElseGet(() -> new VacancySyncStateEntity(SOURCE));
        state.started(now());
        stateRepository.save(state);
    }

    //TODO Decompose
    @Transactional
    public VacancySyncBatchResult persistBatch(List<Vacancy> vacancies) {
        int created = 0;
        int updated = 0;
        int unchanged = 0;
        List<String> changedIds = new ArrayList<>();
        OffsetDateTime now = now();
        for (Vacancy vacancy : vacancies) {
            String externalId = vacancy.hhId() == null || vacancy.hhId().isBlank()
                    ? stripStablePrefix(vacancy.id()) : vacancy.hhId();
            var content = normalizer.normalize(vacancy);
            String hash = hasher.hash(content);
            VacancyEntity entity = repository.findBySourceAndExternalId(SOURCE, externalId).orElse(null);
            if (entity == null) {
                entity = VacancyEntity.create(SOURCE, externalId, now);
                entity.replaceContent(content, hash, normalizer.rawPayload(vacancy), now);
                repository.save(entity);
                created++;
                changedIds.add(externalId);
            } else if (!hash.equals(entity.contentHash()) || !entity.active()) {
                entity.replaceContent(content, hash, normalizer.rawPayload(vacancy), now);
                updated++;
                changedIds.add(externalId);
            } else {
                entity.markSeen(now);
                unchanged++;
            }
        }
        Long marketVersion = null;
        if (!changedIds.isEmpty()) {
            VacancySyncStateEntity state = stateRepository.findById(SOURCE)
                    .orElseGet(() -> new VacancySyncStateEntity(SOURCE));
            state.incrementMarketVersion();
            stateRepository.save(state);
            marketVersion = state.marketVersion();
        }
        return new VacancySyncBatchResult(created, updated, unchanged, List.copyOf(changedIds), marketVersion);
    }

    @Transactional
    public void markFinished(boolean successful, String error) {
        VacancySyncStateEntity state = stateRepository.findById(SOURCE)
                .orElseGet(() -> new VacancySyncStateEntity(SOURCE));
        state.finished(now(), successful, error);
        stateRepository.save(state);
    }

    private OffsetDateTime now() { return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC); }
    private static String stripStablePrefix(String id) { return id != null && id.startsWith("hh-") ? id.substring(3) : id; }
}
