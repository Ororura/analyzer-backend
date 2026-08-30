package com.ororura.analyzer.vacancy.cache;

import java.util.concurrent.atomic.AtomicLong;

import com.ororura.analyzer.vacancy.persistence.VacancySyncStateRepository;
import org.springframework.stereotype.Service;

@Service
public class VacancyMarketVersionService {
    private static final String SOURCE = "hh.ru";
    private final VacancySyncStateRepository repository;
    private final AtomicLong localVersion = new AtomicLong(-1);

    public VacancyMarketVersionService(VacancySyncStateRepository repository) { this.repository = repository; }

    public long current() {
        long current = localVersion.get();
        if (current >= 0) return current;
        long loaded = repository.findById(SOURCE).map(state -> state.marketVersion()).orElse(0L);
        localVersion.compareAndSet(-1, loaded);
        return localVersion.get();
    }

    public void changed(long version) { localVersion.set(version); }
}
