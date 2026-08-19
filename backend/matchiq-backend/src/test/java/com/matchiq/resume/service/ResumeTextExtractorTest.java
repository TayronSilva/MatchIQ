package com.matchiq.resume.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ResumeTextExtractorTest {

    private final ResumeTextExtractor extractor = new ResumeTextExtractor();

    @Test
    void extract_shouldReturnTextFromPdf() throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText("Java Spring Boot PostgreSQL");
                cs.endText();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);

            MockMultipartFile file = new MockMultipartFile(
                    "file", "curriculo.pdf", "application/pdf", baos.toByteArray());

            String text = extractor.extract(file);

            assertNotNull(text);
            assertTrue(text.contains("Java Spring Boot PostgreSQL"));
        }
    }

    @Test
    void extract_shouldReturnTextFromDocx() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setText("Desenvolvedor Backend com foco em APIs REST");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.write(baos);

            MockMultipartFile file = new MockMultipartFile(
                    "file", "curriculo.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    baos.toByteArray());

            String text = extractor.extract(file);

            assertNotNull(text);
            assertTrue(text.contains("Desenvolvedor Backend com foco em APIs REST"));
        }
    }

    @Test
    void extract_shouldThrowWhenUnsupportedType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "arquivo.txt", "text/plain", new byte[]{1, 2, 3});

        assertThrows(IllegalArgumentException.class, () -> extractor.extract(file));
    }
}
