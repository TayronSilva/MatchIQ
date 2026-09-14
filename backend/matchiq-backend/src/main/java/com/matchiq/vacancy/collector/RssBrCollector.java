package com.matchiq.vacancy.collector;

import com.matchiq.profile.domain.WorkModality;
import com.matchiq.vacancy.domain.VacancySource;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Order(3)
public class RssBrCollector implements JobBoardCollector {

    private static final List<String> DEFAULT_FEEDS = List.of(
            "https://www.infojobs.com.br/rss",
            "https://rsshub.app/programathor/vagas"
    );

    private final List<String> feeds;

    public RssBrCollector() {
        this(DEFAULT_FEEDS);
    }

    public RssBrCollector(List<String> feeds) {
        this.feeds = feeds;
    }

    @Override
    public VacancySource source() {
        return VacancySource.RSSBR;
    }

    @Override
    public List<RawVacancy> collect(PoliteHttpClient http) throws Exception {
        return fetchFromFeeds(http, feeds);
    }

    @Override
    public List<RawVacancy> collect(PoliteHttpClient http, List<String> keywords) throws Exception {
        return fetchFromFeeds(http, feeds);
    }

    private List<RawVacancy> fetchFromFeeds(PoliteHttpClient http, List<String> targetFeeds) throws Exception {
        List<RawVacancy> out = new ArrayList<>();
        for (String feedUrl : targetFeeds) {
            try {
                String xml = http.get(feedUrl);
                if (xml == null || xml.isBlank()) {
                    continue;
                }
                Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
                Elements items = doc.select("item");
                if (items.isEmpty()) {
                    items = doc.select("entry");
                }
                for (Element item : items) {
                    String id = item.select("guid, id").text();
                    if (id.isBlank()) {
                        id = item.select("link").attr("href");
                    }
                    if (id.isBlank()) {
                        id = feedUrl + "#" + System.nanoTime();
                    }
                    String title = item.select("title").text();
                    String link = item.select("link").text();
                    if (link.isBlank()) {
                        link = item.select("link").attr("href");
                    }
                    String description = item.select("description, summary, content\\:encoded").text();
                    if (description.isBlank()) {
                        description = item.select("description").html();
                    }
                    if (title.isBlank() || description.isBlank()) {
                        continue;
                    }
                    WorkModality modality = null;
                    String titleLower = title.toLowerCase();
                    if (titleLower.contains("remoto") || titleLower.contains("remote")) {
                        modality = WorkModality.REMOTE;
                    } else if (titleLower.contains("hibrido") || titleLower.contains("hibrido")) {
                        modality = WorkModality.HYBRID;
                    } else if (titleLower.contains("presencial")) {
                        modality = WorkModality.ONSITE;
                    }
                    out.add(new RawVacancy(id, title, null, description, link, null,
                            modality, null, null));
                }
            } catch (Exception ignored) {
            }
        }
        return out;
    }
}
