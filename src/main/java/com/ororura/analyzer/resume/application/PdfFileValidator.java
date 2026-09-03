package com.ororura.analyzer.resume.application;

import java.nio.charset.StandardCharsets;

import com.ororura.analyzer.resume.application.port.ResumeDocument;
import com.ororura.analyzer.resume.config.ResumeAnalysisProperties;
import com.ororura.analyzer.resume.error.ResumeAnalysisException;
import com.ororura.analyzer.resume.error.ResumeErrorCode;

public class PdfFileValidator {

    private static final byte[] PDF_HEADER = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private final ResumeAnalysisProperties properties;

    public PdfFileValidator(ResumeAnalysisProperties properties) {
        this.properties = properties;
    }

    public byte[] validate(ResumeDocument document) {
        if (document == null || document.isEmpty()) {
            throw new ResumeAnalysisException(ResumeErrorCode.INVALID_FILE, "PDF file is required and must not be empty");
        }
        if (document.declaredSize() > properties.maxFileSize().toBytes()) {
            throw new ResumeAnalysisException(ResumeErrorCode.PDF_TOO_LARGE, "PDF file exceeds the configured size limit");
        }
        if (!"application/pdf".equalsIgnoreCase(document.contentType())) {
            throw new ResumeAnalysisException(ResumeErrorCode.UNSUPPORTED_FILE_TYPE, "Only application/pdf is supported");
        }
        byte[] bytes = document.content();
        if (!containsPdfHeader(bytes)) {
            throw new ResumeAnalysisException(ResumeErrorCode.UNSUPPORTED_FILE_TYPE, "The uploaded file is not a PDF");
        }
        return bytes;
    }

    private static boolean containsPdfHeader(byte[] bytes) {
        int limit = Math.min(bytes.length - PDF_HEADER.length, 1024);
        for (int offset = 0; offset <= limit; offset++) {
            boolean matches = true;
            for (int index = 0; index < PDF_HEADER.length; index++) {
                if (bytes[offset + index] != PDF_HEADER[index]) {
                    matches = false;
                    break;
                }
            }
            if (matches) return true;
        }
        return false;
    }
}
