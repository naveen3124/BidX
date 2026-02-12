package com.bengaluru.rss;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;

public class Main {

    private static final AtomicBoolean healthy = new AtomicBoolean(true);
    private static final Duration COMMIT_INTERVAL = Duration.ofSeconds(30);

    public static void main(String[] args) throws Exception {

        String indexPath = System.getenv().getOrDefault("INDEX_PATH",
                "/data/lucene-index");

        RssFetcher fetcher = new RssFetcher();
        RssParser parser = new RssParser();
        LuceneIndexer indexer = new LuceneIndexer(Path.of(indexPath));

        RssEngine engine = new RssEngine(fetcher, parser, indexer, healthy);

        List<String> feeds = List.of(
                "https://timesofindia.indiatimes.com/rssfeeds/-2128936835.cms");

        Thread engineThread =
                Thread.ofPlatform().name("rss-engine").start(() -> {
                    try {
                        engine.start(feeds);
                    } catch (Exception e) {
                        healthy.set(false);
                        e.printStackTrace();
                    }
                });

        ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            try {
                indexer.commit();
            } catch (Exception e) {
                healthy.set(false);
                e.printStackTrace();
            }
        }, 30, COMMIT_INTERVAL.getSeconds(), TimeUnit.SECONDS);

        HttpServer server = startHealthServer(healthy);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                engine.stop();
                engineThread.join();

                scheduler.shutdown();
                scheduler.awaitTermination(10, TimeUnit.SECONDS);

                indexer.commit();
                indexer.close();

                server.stop(0);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }));

        engineThread.join();
    }

    private static HttpServer startHealthServer(AtomicBoolean healthy)
            throws IOException {

        int port =
                Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/health", exchange -> {

            String response = healthy.get() ? "OK" : "UNHEALTHY";

            if (!healthy.get()) {
                exchange.sendResponseHeaders(500, response.length());
            } else {
                exchange.sendResponseHeaders(200, response.length());
            }

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();

        System.out.println("Health endpoint started on port " + port);

        return server;
    }
}
