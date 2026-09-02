package com.matchiq.vacancy.collector;

import com.matchiq.profile.domain.WorkModality;
import org.jsoup.Jsoup;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Helpers compartilhados pelos coletores: parsing de JSON, limpeza de HTML,
 * normalização de modalidade e parsing de datas em formatos variados.
 */
public final class CollectorSupport {

    private CollectorSupport() {
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter RSS_DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss z", java.util.Locale.US);
    private static final DateTimeFormatter RSS_DATE_OFFSET =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss Z", java.util.Locale.US);

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static String str(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    public static String htmlToText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return Jsoup.parse(html).text().replaceAll("\\s+", " ").trim();
    }

    public static WorkModality modalityFromText(String text) {
        if (text == null) {
            return null;
        }
        String t = text.toLowerCase();
        if (t.contains("remoto") || t.contains("remote") || t.contains("anywhere") || t.contains("worldwide")) {
            return WorkModality.REMOTE;
        }
        if (t.contains("híbrido") || t.contains("hibrido") || t.contains("hybrid")) {
            return WorkModality.HYBRID;
        }
        if (t.contains("presencial") || t.contains("onsite") || t.contains("on-site")) {
            return WorkModality.ONSITE;
        }
        return null;
    }

    public static LocalDateTime parseIso(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return java.time.OffsetDateTime.parse(value).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return java.time.Instant.parse(value).atZone(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
        }
        return null;
    }

    public static LocalDateTime fromEpochSeconds(Long seconds) {
        if (seconds == null) {
            return null;
        }
        try {
            return LocalDateTime.ofInstant(java.time.Instant.ofEpochSecond(seconds), ZoneOffset.UTC);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static LocalDateTime parseRssDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        int comma = v.indexOf(',');
        if (comma >= 0) {
            v = v.substring(comma + 1).trim();
        }
        try {
            return java.time.OffsetDateTime.parse(v, RSS_DATE_OFFSET).toLocalDateTime();
        } catch (Exception ignored) {
        }
        try {
            return java.time.OffsetDateTime.parse(v, RSS_DATE).toLocalDateTime();
        } catch (Exception ignored) {
        }
        return null;
    }

    public static String buildSalary(String min, String max) {
        if (min == null && max == null) {
            return null;
        }
        try {
            long mn = min == null ? 0 : Long.parseLong(min.trim());
            long mx = max == null ? 0 : Long.parseLong(max.trim());
            if (mn > 0 && mx > 0) {
                return "$" + mn + " - $" + mx;
            }
            if (mx > 0) {
                return "$" + mx;
            }
            if (mn > 0) {
                return "$" + mn;
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }
}
