package com.matchiq.vacancy.collector;

import com.matchiq.profile.domain.WorkModality;
import com.matchiq.vacancy.domain.VacancySource;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * We Work Remotely via feeds RSS por categoria (XML).
 * Cada item traz título, link, descrição (HTML), guid e pubDate.
 */
@Service
public class WeWorkRemotelyCollector implements JobBoardCollector {

    private final List<String> feeds;

    public WeWorkRemotelyCollector() {
        this(List.of(
                "https://weworkremotely.com/categories/remote-programming-jobs.rss",
                "https://weworkremotely.com/categories/remote-back-end-programming-jobs.rss",
                "https://weworkremotely.com/categories/remote-front-end-programming-jobs.rss",
                "https://weworkremotely.com/categories/remote-full-stack-programming-jobs.rss",
                "https://weworkremotely.com/categories/remote-devops-sysadmin-jobs.rss"));
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
        List<RawVacancy> out = new ArrayList<>();
        for (String feed : feeds) {
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
                // falha num feed não derruba os demais
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
