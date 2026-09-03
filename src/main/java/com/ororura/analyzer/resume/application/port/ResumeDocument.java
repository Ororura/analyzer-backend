package com.ororura.analyzer.resume.application.port;

public record ResumeDocument(String filename, String contentType, long declaredSize, byte[] content) {

    public ResumeDocument {
        content = content == null ? new byte[0] : content.clone();
    }

    @Override
    public byte[] content() {
        return content.clone();
    }

    public boolean isEmpty() {
        return content.length == 0;
    }
}
