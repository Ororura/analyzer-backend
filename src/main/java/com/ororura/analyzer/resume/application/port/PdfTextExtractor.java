package com.ororura.analyzer.resume.application.port;

public interface PdfTextExtractor {

    String extract(byte[] pdf);
}
