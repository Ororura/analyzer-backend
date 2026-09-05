package com.ororura.analyzer.analysis.application;

import java.time.Clock;
import java.util.*;
import com.ororura.analyzer.analysis.api.ProfileRequest;
import com.ororura.analyzer.analysis.domain.*;
import com.ororura.analyzer.analysis.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
@Transactional
public class AnalysisProfileService {
    private final AnalysisProfileRepository repository;
    private final Clock clock;
    public AnalysisProfileService(AnalysisProfileRepository repository, Clock clock) { this.repository=repository; this.clock=clock; }
    public UserAnalysisProfile create(UUID owner, ProfileRequest request) {
        var value = resolve(UUID.randomUUID(),0,request,null);
        return repository.saveAndFlush(new AnalysisProfileEntity(owner,value)).domain();
    }
    @Transactional(readOnly=true)
    public UserAnalysisProfile get(UUID owner, UUID id) { return find(owner,id).domain(); }
    @Transactional(readOnly=true)
    public List<UserAnalysisProfile> list(UUID owner, int page, int size) {
        if(page<0 || size<1 || size>50) throw new ResponseStatusException(BAD_REQUEST,"page >= 0, size 1..50");
        return repository.findByOwnerIdAndDeletedAtIsNull(owner,PageRequest.of(page,size,Sort.by("createdAt","id")))
                .stream().map(AnalysisProfileEntity::domain).toList();
    }
    public UserAnalysisProfile update(UUID owner, UUID id, long version, ProfileRequest request) {
        var entity=find(owner,id); var current=entity.domain(); check(current,version);
        entity.update(resolve(id,version,request,current));
        return repository.saveAndFlush(entity).domain();
    }
    public void delete(UUID owner, UUID id, long version) {
        var entity=find(owner,id); check(entity.domain(),version); entity.delete(clock.instant()); repository.flush();
    }
    private AnalysisProfileEntity find(UUID owner, UUID id) {
        return repository.findByIdAndOwnerIdAndDeletedAtIsNull(id,owner)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND,"Profile not found"));
    }
    private void check(UserAnalysisProfile profile,long expected) {
        if(profile.version()!=expected) throw new ResponseStatusException(PRECONDITION_FAILED,"Profile version has changed");
    }
    private UserAnalysisProfile resolve(UUID id,long version,ProfileRequest r,UserAnalysisProfile current) {
        var preset=current==null && r.preset()!=null ? AnalysisPreset.legacy(r.preset()) : null;
        var direction=r.direction()!=null ? r.direction() : preset==null ? null : preset.direction();
        var specialization=r.specialization()!=null ? r.specialization().trim() : preset==null ? null : preset.specialization();
        var grade=r.targetGrade()!=null ? r.targetGrade() : preset==null ? null : preset.targetGrade();
        var tech=r.technologies()!=null ? r.technologies() : preset==null ? null : preset.technologies();
        if(direction==null || specialization==null || specialization.isBlank() || grade==null || tech==null)
            throw new ResponseStatusException(BAD_REQUEST,"direction, specialization, targetGrade and technologies are required without preset defaults");
        if(current!=null && r.preset()!=null && r.preset()!=current.preset())
            throw new ResponseStatusException(BAD_REQUEST,"preset provenance cannot be changed");
        var filters=r.marketFilters()==null ? MarketFilters.defaults() : r.marketFilters();
        return new UserAnalysisProfile(id,version,r.name().trim(),direction,specialization,grade,
                tech.stream().map(String::trim).map(v -> v.toLowerCase(Locale.ROOT)).distinct().sorted().toList(),
                filters,current==null ? r.preset() : current.preset(),ScoringPolicy.VERSION,
                current==null ? clock.instant() : current.createdAt(),clock.instant());
    }
}
