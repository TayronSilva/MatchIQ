package com.matchiq.vacancy.collector;

import com.matchiq.profile.domain.WorkModality;
import com.matchiq.vacancy.domain.VacancySource;
import org.jsoup.Jsoup;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Order(10)
public class WeWorkRemotelyCollector implements JobBoardCollector {

    private static final List<String> ALL_FEEDS = List.of(
            "https://weworkremotely.com/categories/remote-programming-jobs.rss",
            "https://weworkremotely.com/categories/remote-back-end-programming-jobs.rss",
            "https://weworkremotely.com/categories/remote-front-end-programming-jobs.rss",
            "https://weworkremotely.com/categories/remote-full-stack-programming-jobs.rss",
            "https://weworkremotely.com/categories/remote-devops-sysadmin-jobs.rss"
    );

    private final List<String> feeds;

    public WeWorkRemotelyCollector() {
        this(ALL_FEEDS);
    }

    public WeWorkRemotelyCollector(List<String> feeds) {
        this.feeds = feeds;
    }

    @Override
    public VacancySource source() {
        return VacancySource.WEWORKREMOTELY;
    }

    @Override
    public List<RawVacancy> collect(PoliteHttpClient http) throws Exception {
        return fetchFromFeeds(http, feeds);
    }

    @Override
    public List<RawVacancy> collect(PoliteHttpClient http, List<String> keywords) throws Exception {
        if (keywords == null || keywords.isEmpty()) {
            return collect(http);
        }
        List<String> relevantFeeds = selectFeedsByKeywords(keywords);
        return fetchFromFeeds(http, relevantFeeds);
    }

    private List<String> selectFeedsByKeywords(List<String> keywords) {
        List<String> lower = keywords.stream().map(k -> k.toLowerCase(Locale.ROOT)).toList();
        List<String> selected = new ArrayList<>();

        boolean hasBackend = lower.stream().anyMatch(k ->
                k.contains("java") || k.contains("python") || k.contains("backend") ||
                k.contains("spring") || k.contains("node") || k.contains("go") || k.contains("rust"));
        boolean hasFrontend = lower.stream().anyMatch(k ->
                k.contains("react") || k.contains("angular") || k.contains("vue") ||
                k.contains("frontend") || k.contains("javascript") || k.contains("typescript") || k.contains("css"));
        boolean hasDevops = lower.stream().anyMatch(k ->
                k.contains("docker") || k.contains("kubernetes") || k.contains("devops") ||
                k.contains("aws") || k.contains("ci/cd") || k.contains("terraform"));

        if (hasBackend) {
            selected.add(ALL_FEEDS.get(1));
        }
        if (hasFrontend) {
            selected.add(ALL_FEEDS.get(2));
        }
        if (hasBackend || hasFrontend) {
            selected.add(ALL_FEEDS.get(3));
        }
        if (hasDevops) {
            selected.add(ALL_FEEDS.get(4));
        }
        if (selected.isEmpty()) {
            selected.add(ALL_FEEDS.get(0));
        }
        return selected;
    }

    private List<RawVacancy> fetchFromFeeds(PoliteHttpClient http, List<String> targetFeeds) throws Exception {
        List<RawVacancy> out = new ArrayList<>();
        for (String feed : targetFeeds) {
            try {
                String body = http.get(feed);
                Document doc = Jsoup.parse(body, "", Parser.xmlParser());
                Elements items = doc.select("item");
                for (Element item : items) {
                    String title = textOf(item, "title");
                    String link = textOf(item, "link");
                    String description = CollectorSupport.htmlToText(textOf(item, "description"));
                    String guid = textOf(item, "guid");
                    String company = parseCompany(title);
                    out.add(new RawVacancy(guid, title, company, description, link, null,
                            WorkModality.REMOTE, null,
                            CollectorSupport.parseRssDate(textOf(item, "pubDate"))));
                }
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    private static String textOf(Element item, String tag) {
        Element el = item.selectFirst(tag);
        return el == null ? null : el.text();
    }

    private static String parseCompany(String title) {
        if (title == null) {
            return null;
        }
        int idx = title.lastIndexOf(" - ");
        return idx > 0 ? title.substring(idx + 3).trim() : null;
    }
}
