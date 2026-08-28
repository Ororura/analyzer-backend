package com.ororura.analyzer.resume.pdf;

import java.io.ByteArrayOutputStream;

import com.ororura.analyzer.resume.config.ResumeAnalysisProperties;
import com.ororura.analyzer.resume.error.ResumeAnalysisException;
import com.ororura.analyzer.resume.error.ResumeErrorCode;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfProcessingTests {

    private final PdfFileValidator validator = new PdfFileValidator(
            new ResumeAnalysisProperties(DataSize.ofBytes(1024), "Java Backend Developer", "1", "2026-08"));
    private final PdfBoxTextExtractor extractor = new PdfBoxTextExtractor();

    @Test
    void validatesAndExtractsTextPdf() throws Exception {
        byte[] pdf = pdf("Java Spring Backend");
        byte[] validated = validator.validate(new MockMultipartFile("file", "resume.pdf", "application/pdf", pdf));
        assertThat(extractor.extract(validated)).contains("Java Spring Backend");
    }

    @Test
    void rejectsWrongMimeMagicOversizeAndEmptyText() throws Exception {
        byte[] pdf = pdf("Java");
        assertCode(new MockMultipartFile("file", "resume.pdf", "text/plain", pdf), ResumeErrorCode.UNSUPPORTED_FILE_TYPE);
        assertCode(new MockMultipartFile("file", "resume.pdf", "application/pdf", "not-pdf".getBytes()),
                ResumeErrorCode.UNSUPPORTED_FILE_TYPE);
        assertCode(new MockMultipartFile("file", "resume.pdf", "application/pdf", new byte[1025]),
                ResumeErrorCode.PDF_TOO_LARGE);
        assertThatThrownBy(() -> extractor.extract(pdf("")))
                .isInstanceOf(ResumeAnalysisException.class)
                .extracting(error -> ((ResumeAnalysisException) error).getCode())
                .isEqualTo(ResumeErrorCode.EMPTY_RESUME);
    }

    @Test
    void rejectsCorruptedPdfWithValidHeader() {
        assertThatThrownBy(() -> extractor.extract("%PDF-corrupt".getBytes()))
                .isInstanceOf(ResumeAnalysisException.class)
                .extracting(error -> ((ResumeAnalysisException) error).getCode())
                .isEqualTo(ResumeErrorCode.PDF_PARSE_FAILED);
    }

    private void assertCode(MockMultipartFile file, ResumeErrorCode code) {
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(ResumeAnalysisException.class)
                .extracting(error -> ((ResumeAnalysisException) error).getCode())
                .isEqualTo(code);
    }

    private static byte[] pdf(String text) throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            if (!text.isEmpty()) {
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    content.newLineAtOffset(50, 700);
                    content.showText(text);
                    content.endText();
                }
            }
            document.save(output);
            return output.toByteArray();
        }
    }
}
