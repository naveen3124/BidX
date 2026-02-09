package com.bengaluru.rss;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {

    private static final Logger LOG =
        LoggerFactory.getLogger(Main.class);

    private static final URI TOI_RSS =
        URI.create("https://timesofindia.indiatimes.com/rssfeedstopstories.cms");

    private static final AtomicBoolean running =
        new AtomicBoolean(false);

    public static void main(String[] args) throws Exception {

        // ---- Lucene setup ----
        Directory dir = FSDirectory.open(Path.of("./lucene-index"));
        IndexWriterConfig cfg =
            new IndexWriterConfig(new StandardAnalyzer());
        cfg.setRAMBufferSizeMB(256);
        cfg.setCommitOnClose(false);

        IndexWriter writer = new IndexWriter(dir, cfg);

        // ---- Background worker ----
        Thread worker = new Thread(
            () -> pollLoop(writer),
            "rss-poller"
        );
        worker.setDaemon(true);

        // ---- CLI ----
        System.out.println("RSS service ready");
        System.out.println("Commands: start | stop | exit");

        Scanner scanner = new Scanner(System.in);

        while (true) {
            String cmd = scanner.nextLine().trim().toLowerCase();

            switch (cmd) {
                case "start" -> {
                    if (running.compareAndSet(false, true)) {
                        if (!worker.isAlive()) {
                            worker.start();
                        }
                        System.out.println("RSS polling started");
                    } else {
                        System.out.println("RSS polling already running");
                    }
                }

                case "stop" -> {
                    running.set(false);
                    System.out.println("RSS polling stopped");
                }

                case "exit" -> {
                    running.set(false);
                    writer.commit();
                    writer.close();
                    System.out.println("Shutdown complete");
                    return;
                }

                default -> {
                    System.out.println("Unknown command");
                    System.out.println("Commands: start | stop | exit");
                }
            }
        }
    }

    // ---------------- INTERNAL SERVICE ----------------

    private static void pollLoop(IndexWriter writer) {
        HttpClient client = HttpClient.newHttpClient();

        while (true) {
            if (!running.get()) {
                sleep(1000);
                continue;
            }

            try {
                HttpRequest req = HttpRequest.newBuilder(TOI_RSS)
                    .header("User-Agent", "BengaluruRSS/1.0")
                    .GET()
                    .build();

                HttpResponse<InputStream> res =
                    client.send(req, HttpResponse.BodyHandlers.ofInputStream());

                if (res.statusCode() != 200) {
                    LOG.warn("TOI RSS fetch failed: HTTP {}", res.statusCode());
                    sleep(30_000);
                    continue;
                }

                SyndFeed feed =
                    new SyndFeedInput().build(new XmlReader(res.body()));

                List<SyndEntry> entries = feed.getEntries();
                int indexed = 0;

                for (SyndEntry e : entries) {
                    String id = e.getUri() != null
                        ? e.getUri()
                        : e.getLink();
                    if (id == null) continue;

                    Document d = new Document();
                    d.add(new StringField("id", id, Field.Store.YES));
                    d.add(new StringField("source", "timesofindia", Field.Store.YES));
                    d.add(new TextField("title", e.getTitle(), Field.Store.YES));
                    d.add(new StringField("link", e.getLink(), Field.Store.YES));

                    if (e.getDescription() != null) {
                        d.add(new TextField(
                            "summary",
                            e.getDescription().getValue(),
                            Field.Store.NO
                        ));
                    }

                    long ts = e.getPublishedDate() != null
                        ? e.getPublishedDate().toInstant().toEpochMilli()
                        : Instant.now().toEpochMilli();

                    d.add(new LongPoint("published", ts));

                    writer.updateDocument(new Term("id", id), d);
                    indexed++;
                }

                writer.commit();
                LOG.info("Indexed {} TOI entries", indexed);

            } catch (Exception e) {
                LOG.error("RSS polling failed", e);
            }

            sleep(10 * 60 * 1000);
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {}
    }
}
