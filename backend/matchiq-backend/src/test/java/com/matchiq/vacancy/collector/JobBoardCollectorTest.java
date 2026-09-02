package com.matchiq.vacancy.collector;

import com.matchiq.profile.domain.WorkModality;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JobBoardCollectorTest {

    private static HttpServer server;
    private static String base;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        ctx("/remotive", REMOTIVE_JSON);
        ctx("/remoteok", REMOTEOK_JSON);
        ctx("/arbeitnow", ARBEITNOW_JSON);
        ctx("/himalayas", HIMALAYAS_JSON);
        ctx("/wwr.rss", WWR_RSS);
        ctx("/repos/owner/repo/issues", GITHUB_ISSUES_JSON);
        server.start();
        base = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    private static void ctx(String path, String body) {
        server.createContext(path, exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
    }

    private final PoliteHttpClient http = new PoliteHttpClient(0);

    @Test
    void remotive_shouldParseJobs() throws Exception {
        List<RawVacancy> r = new RemotiveCollector(base + "/remotive").collect(http);
        assertEquals(2, r.size());
        assertEquals("Java Developer", r.get(0).title());
        assertEquals("Acme", r.get(0).company());
        assertEquals("Worldwide", r.get(0).location());
        assertEquals("$40,000 - $50,000", r.get(0).salaryRange());
        assertEquals(WorkModality.REMOTE, r.get(0).workModality());
        assertTrue(r.get(0).description().contains("Java") && r.get(0).description().contains("Spring Boot"));
        assertFalse(r.get(0).description().contains("<"));
    }

    @Test
    void remoteok_shouldSkipMetadataAndParse() throws Exception {
        List<RawVacancy> r = new RemoteOkCollector(base + "/remoteok").collect(http);
        assertEquals(2, r.size());
        assertEquals("Senior Java Engineer", r.get(0).title());
        assertEquals("$120000 - $150000", r.get(0).salaryRange());
        assertEquals(WorkModality.REMOTE, r.get(0).workModality());
        assertNotNull(r.get(0).postedAt());
        assertEquals("Python Dev", r.get(1).title());
    }

    @Test
    void arbeitnow_shouldParseAndDeriveModality() throws Exception {
        List<RawVacancy> r = new ArbeitnowCollector(base + "/arbeitnow").collect(http);
        assertEquals(2, r.size());
        assertEquals(WorkModality.REMOTE, r.get(0).workModality());
        assertEquals(WorkModality.REMOTE, r.get(1).workModality());
        assertTrue(r.get(0).description().contains("Go") && r.get(0).description().contains("Kubernetes"));
    }

    @Test
    void himalayas_shouldParseExcerptAndSalary() throws Exception {
        List<RawVacancy> r = new HimalayasCollector(base + "/himalayas").collect(http);
        assertEquals(2, r.size());
        assertEquals("React Native Dev", r.get(0).title());
        assertEquals("$80000 - $100000 / year", r.get(0).salaryRange());
        assertEquals(WorkModality.REMOTE, r.get(0).workModality());
        assertNotNull(r.get(0).postedAt());
        assertEquals("Data Engineer", r.get(1).title());
        assertTrue(r.get(1).description().contains("Build pipelines"));
    }

    @Test
    void weworkremotely_shouldParseRss() throws Exception {
        List<RawVacancy> r = new WeWorkRemotelyCollector(List.of(base + "/wwr.rss")).collect(http);
        assertEquals(2, r.size());
        assertEquals("Backend Engineer - Acme", r.get(0).title());
        assertEquals("Acme", r.get(0).company());
        assertEquals(WorkModality.REMOTE, r.get(0).workModality());
        assertNotNull(r.get(0).postedAt());
        assertFalse(r.get(0).description().contains("<"));
    }

    @Test
    void vagasbr_shouldParseIssuesAndSkipPullRequests() throws Exception {
        List<RawVacancy> r = new VagasBrCollector(List.of("owner/repo"), base).collect(http);
        assertEquals(3, r.size());
        assertEquals(WorkModality.REMOTE, r.get(0).workModality());
        assertEquals("Empresa X - Vaga Backend", r.get(0).title());
        assertNull(r.get(1).workModality());
        assertEquals("São Paulo", r.get(1).location());
        assertEquals(WorkModality.HYBRID, r.get(2).workModality());
    }

    private static final String REMOTIVE_JSON = """
            {"0-legal-notice":"x","job-count":2,"jobs":[
              {"id":"101","url":"https://remotive.com/remote-jobs/java-dev","title":"Java Developer","company_name":"Acme","candidate_required_location":"Worldwide","salary":"$40,000 - $50,000","description":"<p>Need <b>Java</b> and Spring Boot.</p>"},
              {"id":"102","url":"https://remotive.com/remote-jobs/python-dev","title":"Python Developer","company_name":"Beta","candidate_required_location":"Latam","salary":"","description":"<p>Python and Django.</p>"}
            ]}""";

    private static final String REMOTEOK_JSON = """
            [
              {"legal":"terms"},
              {"id":"999","slug":"remote-java-999","position":"Senior Java Engineer","company":"Acme","description":"<p>Java/Kotlin.</p>","url":"https://remoteok.com/remote-jobs/remote-java-999","location":"Worldwide","tags":["java"],"salary_min":120000,"salary_max":150000,"date":"2026-01-15T10:00:00Z"},
              {"id":"998","slug":"remote-py-998","position":"Python Dev","company":"Beta","description":"<p>Python.</p>","url":"https://remoteok.com/remote-jobs/remote-py-998","location":"US","date":"2026-02-01T12:00:00+00:00"}
            ]""";

    private static final String ARBEITNOW_JSON = """
            {"data":[
              {"slug":"senior-backend-acme","title":"Senior Backend","company_name":"Acme","description":"<p>Go and Kubernetes.</p>","url":"https://www.arbeitnow.com/jobs/acme/backend","location":"Berlin, Germany","remote":true,"salary":"","created_at":"2026-03-01T08:00:00"},
              {"slug":"frontend-beta","title":"Frontend","company_name":"Beta","description":"<p>React.</p>","url":"https://www.arbeitnow.com/jobs/beta/frontend","location":"Remote","remote":false,"created_at":"2026-03-02T08:00:00"}
            ]}""";

    private static final String HIMALAYAS_JSON = """
            {"comments":"x","updatedAt":1,"offset":0,"limit":1,"totalCount":2,"jobs":[
              {"guid":"himalayas-1","title":"React Native Dev","companyName":"lemon.io","excerpt":"Are you a talented developer...","applicationLink":"https://himalayas.app/jobs/1","minSalary":80000,"maxSalary":100000,"salaryPeriod":"year","pubDate":1784275242},
              {"guid":"himalayas-2","title":"Data Engineer","companyName":"Corp","description":"<p>Build pipelines.</p>","applicationLink":"https://himalayas.app/jobs/2","pubDate":1784361600}
            ]}""";

    private static final String WWR_RSS = """
            <rss version="2.0"><channel>
              <item><title>Backend Engineer - Acme</title><link>https://weworkremotely.com/remote-jobs/backend-acme</link><description><![CDATA[<p>Java role.</p>]]></description><guid>https://weworkremotely.com/remote-jobs/backend-acme</guid><pubDate>Mon, 02 Jun 2026 12:00:00 +0000</pubDate></item>
              <item><title>Frontend Dev - Beta</title><link>https://weworkremotely.com/remote-jobs/frontend-beta</link><description>React.</description><guid>https://weworkremotely.com/remote-jobs/frontend-beta</guid><pubDate>Tue, 03 Jun 2026 09:00:00 +0000</pubDate></item>
            </channel></rss>""";

    private static final String GITHUB_ISSUES_JSON = """
            [
              {"id":"555","title":"[Remoto] Empresa X - Vaga Backend","body":"Vaga para backend com Java.","html_url":"https://github.com/backend-br/vagas/issues/555","created_at":"2026-04-01T10:00:00Z"},
              {"id":"556","title":"[São Paulo] Empresa Y - Vaga Frontend","body":"Vaga frontend React.","html_url":"https://github.com/frontendbr/vagas/issues/556","created_at":"2026-04-02T10:00:00Z"},
              {"id":"557","title":"[Híbrido] Empresa Z - Vaga QA","body":"QA.","html_url":"https://github.com/backend-br/vagas/issues/557","created_at":"2026-04-03T10:00:00Z"},
              {"id":"558","title":"Pull request test","body":"x","html_url":"https://github.com/backend-br/vagas/pull/558","created_at":"2026-04-04T10:00:00Z","pull_request":{"url":"x"}}
            ]""";
}
