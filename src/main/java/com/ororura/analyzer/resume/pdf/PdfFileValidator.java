package com.ororura.analyzer.resume.pdf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.ororura.analyzer.resume.config.ResumeAnalysisProperties;
import com.ororura.analyzer.resume.error.ResumeAnalysisException;
import com.ororura.analyzer.resume.error.ResumeErrorCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class PdfFileValidator {

    private static final byte[] PDF_HEADER = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private final ResumeAnalysisProperties properties;

    public PdfFileValidator(ResumeAnalysisProperties properties) {
        this.properties = properties;
    }

    public byte[] validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResumeAnalysisException(ResumeErrorCode.INVALID_FILE, "PDF file is required and must not be empty");
        }
        if (file.getSize() > properties.maxFileSize().toBytes()) {
            throw new ResumeAnalysisException(ResumeErrorCode.PDF_TOO_LARGE, "PDF file exceeds the configured size limit");
        }
        if (!MediaType.APPLICATION_PDF_VALUE.equalsIgnoreCase(file.getContentType())) {
            throw new ResumeAnalysisException(ResumeErrorCode.UNSUPPORTED_FILE_TYPE, "Only application/pdf is supported");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw new ResumeAnalysisException(ResumeErrorCode.INVALID_FILE, "Unable to read the uploaded file", exception);
        }
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
