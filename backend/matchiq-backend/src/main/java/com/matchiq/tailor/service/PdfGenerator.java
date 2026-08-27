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
            "body{font-family:'Helvetica','Arial',sans-serif;font-size:10.5pt;margin:28px 32px;color:#1a1a1a;line-height:1.35;}" +
            "h1{font-size:20pt;margin:0 0 2pt;text-align:center;text-transform:uppercase;letter-spacing:1px;color:#111;}" +
            "h2{font-size:12.5pt;margin:14pt 0 4pt;padding-bottom:2pt;border-bottom:1.5pt solid #333;color:#111;" +
            "text-transform:uppercase;letter-spacing:0.5px;}" +
            "h3{font-size:11pt;margin:8pt 0 2pt;color:#222;}" +
            "p{margin:3pt 0;}" +
            "ul,ol{margin:2pt 0 6pt 16pt;padding:0;}" +
            "li{margin:2pt 0;}" +
            "strong{color:#000;}" +
            "a{color:#1a1a1a;text-decoration:none;}" +
            "hr{border:none;}";

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
