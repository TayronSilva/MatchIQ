package com.matchiq.tailor.service;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;

/**
 * Converte markdown em PDF no servidor: markdown → HTML (commonmark) →
 * XHTML normalizado (jsoup) → PDF (Flying Saucer / iText).
 */
@Component
public class PdfGenerator {

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();

    private static final String CSS =
            "body{font-family:Helvetica,Arial,sans-serif;font-size:12px;margin:36px;color:#222;line-height:1.5;}" +
            "h1{font-size:20px;margin:0 0 4px;}" +
            "h2{font-size:15px;border-bottom:1px solid #ccc;margin-top:18px;padding-bottom:3px;color:#333;}" +
            "h3{font-size:13px;margin:12px 0 4px;}" +
            "ul,ol{margin:4px 0 10px 18px;}" +
            "li{margin:2px 0;}" +
            "p{margin:6px 0;}" +
            "strong{color:#111;}";

    public byte[] markdownToPdf(String markdown) {
        Node document = parser.parse(markdown == null ? "" : markdown);
        String bodyHtml = htmlRenderer.render(document);
        String xhtml = toXhtml(bodyHtml);
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(xhtml);
            renderer.layout();
            renderer.createPDF(os);
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao gerar o PDF do currículo", e);
        }
    }

    private String toXhtml(String bodyHtml) {
        String full = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/>" +
                "<style>" + CSS + "</style></head><body>" + bodyHtml + "</body></html>";
        Document doc = Jsoup.parse(full);
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        return doc.toString();
    }
}
