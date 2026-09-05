package com.ororura.analyzer.analysis.api;

import java.util.UUID;
import jakarta.servlet.http.HttpSession;

public final class ProfileSession {
    private static final String OWNER = ProfileSession.class.getName()+".owner";
    private ProfileSession() {}
    public static UUID owner(HttpSession session) {
        synchronized(session) {
            UUID owner=(UUID)session.getAttribute(OWNER);
            if(owner==null) { owner=UUID.randomUUID(); session.setAttribute(OWNER,owner); }
            return owner;
        }
    }
}
