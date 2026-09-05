package com.ororura.analyzer.analysis.api;

import java.net.URI;
import java.util.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.ororura.analyzer.analysis.domain.UserAnalysisProfile;
import com.ororura.analyzer.analysis.application.AnalysisProfileService;

@RestController
@RequestMapping("/api/analysis-profiles")
public class AnalysisProfileController {
    private final AnalysisProfileService service;
    public AnalysisProfileController(AnalysisProfileService service) { this.service=service; }
    @PostMapping
    public ResponseEntity<UserAnalysisProfile> create(@Valid @RequestBody ProfileRequest request,HttpSession session) {
        var p=service.create(ProfileSession.owner(session),request);
        return ResponseEntity.created(URI.create("/api/analysis-profiles/"+p.id())).eTag(tag(p.version())).body(p);
    }
    @GetMapping
    public List<UserAnalysisProfile> list(HttpSession session,@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) { return service.list(ProfileSession.owner(session),page,size); }
    @GetMapping("/{id}")
    public ResponseEntity<UserAnalysisProfile> get(@PathVariable UUID id,HttpSession session) {
        var p=service.get(ProfileSession.owner(session),id); return ResponseEntity.ok().eTag(tag(p.version())).body(p);
    }
    @PutMapping("/{id}")
    public ResponseEntity<UserAnalysisProfile> update(@PathVariable UUID id,@Valid @RequestBody ProfileRequest request,
            @RequestHeader(value="If-Match",required=false) String match,HttpSession session) {
        var p=service.update(ProfileSession.owner(session),id,version(match),request);
        return ResponseEntity.ok().eTag(tag(p.version())).body(p);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,@RequestHeader(value="If-Match",required=false) String match,
            HttpSession session) {
        service.delete(ProfileSession.owner(session),id,version(match)); return ResponseEntity.noContent().build();
    }
    private static String tag(long version) { return "\""+version+"\""; }
    private static long version(String value) {
        if(value==null) throw new ResponseStatusException(org.springframework.http.HttpStatus.PRECONDITION_REQUIRED,"If-Match is required");
        if(!value.matches("\"[0-9]+\"")) throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Expected a quoted version ETag");
        try { return Long.parseLong(value.substring(1,value.length()-1)); }
        catch(NumberFormatException e) { throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Invalid version"); }
    }
}
