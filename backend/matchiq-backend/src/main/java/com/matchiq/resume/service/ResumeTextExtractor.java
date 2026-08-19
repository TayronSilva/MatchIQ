package com.matchiq.resume.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Component
public class ResumeTextExtractor {

    private static final String PDF_TYPE = "application/pdf";
    private static final String DOCX_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    public String extract(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (PDF_TYPE.equals(contentType)) {
            return extractPdf(file);
        }
        if (DOCX_TYPE.equals(contentType)) {
            return extractDocx(file);
        }
        throw new IllegalArgumentException("Unsupported file type: " + contentType);
    }

    private String extractPdf(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractDocx(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.isBlank()) {
                    sb.append(text).append(System.lineSeparator());
                }
            }
            return sb.toString();
        }
    }
}
