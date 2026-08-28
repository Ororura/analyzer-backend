package com.ororura.analyzer.resume.pdf;

import java.io.IOException;

import com.ororura.analyzer.resume.error.ResumeAnalysisException;
import com.ororura.analyzer.resume.error.ResumeErrorCode;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
public class PdfBoxTextExtractor implements PdfTextExtractor {

    @Override
    public String extract(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            if (document.isEncrypted()) {
                throw new ResumeAnalysisException(ResumeErrorCode.PDF_PARSE_FAILED,
                        "Encrypted PDF files are not supported");
            }
            String text = normalize(new PDFTextStripper().getText(document));
            if (text.isBlank()) {
                throw new ResumeAnalysisException(ResumeErrorCode.EMPTY_RESUME,
                        "The PDF does not contain an extractable text layer");
            }
            return text;
        } catch (ResumeAnalysisException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new ResumeAnalysisException(ResumeErrorCode.PDF_PARSE_FAILED, "Unable to parse the PDF", exception);
        }
    }

    private static String normalize(String value) {
        return value.replace('\u0000', ' ')
                .replaceAll("[\\t\\x0B\\f\\r ]+", " ")
                .replaceAll(" *\\n+ *", "\\n")
                .trim();
    }
}
