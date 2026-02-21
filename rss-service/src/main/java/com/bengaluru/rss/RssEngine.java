package com.bengaluru.rss;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class RssEngine {

    private static final int QUEUE_CAPACITY = 10_000;
    private static final int INDEX_WORKERS =
            Runtime.getRuntime().availableProcessors();
    private static final int MAX_CONCURRENT_FETCH = 100;

    private static final Duration POLL_INTERVAL = Duration.ofSeconds(10);

    private final BlockingQueue<FeedEntry> queue =
            new ArrayBlockingQueue<>(QUEUE_CAPACITY);

    private final Semaphore fetchLimiter = new Semaphore(MAX_CONCURRENT_FETCH);

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean healthy;

    private final RssFetcher fetcher;
    private final RssParser parser;
    private final LuceneIndexer indexer;

    public RssEngine(RssFetcher fetcher, RssParser parser,
            LuceneIndexer indexer, AtomicBoolean healthy) {

        this.fetcher = fetcher;
        this.parser = parser;
        this.indexer = indexer;
        this.healthy = healthy;
    }

    /**
     * Starts engine using virtual threads without preview APIs.
     */
    public void start(List<String> feedUrls) throws Exception {

        List<Future<Void>> tasks = new ArrayList<>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            // One feed worker per feed URL
            for (String url : feedUrls) {
                tasks.add(executor.submit(() -> feedWorker(url)));
            }

            // Parallel indexing workers
            for (int i = 0; i < INDEX_WORKERS; i++) {
                tasks.add(executor.submit(this::indexLoop));
            }

            // Fail fast if any worker crashes.
            for (Future<Void> task : tasks) {
                try {
                    task.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    stop();
                    throw e;
                } catch (ExecutionException e) {
                    stop();
                    healthy.set(false);
                    executor.shutdownNow();
                    Throwable cause = e.getCause();
                    if (cause instanceof Exception ex) {
                        throw ex;
                    }
                    throw new RuntimeException(cause);
                }
            }
        }
    }

    /**
     * Each feed has a long-running worker.
     */
    private Void feedWorker(String url) {

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            while (running.get()) {

                // Rate limiting (max 100 concurrent fetches)
                if (!fetchLimiter.tryAcquire(5, TimeUnit.SECONDS)) {
                    continue;
                }

                executor.submit(() -> {
                    try {
                        byte[] xml = fetcher.fetch(URI.create(url));
                        List<FeedEntry> entries = parser.parse(url, xml);

                        for (FeedEntry entry : entries) {
                            queue.put(entry); // backpressure
                        }

                    } catch (Exception e) {
                        healthy.set(false);
                    } finally {
                        fetchLimiter.release();
                    }
                });

                Thread.sleep(POLL_INTERVAL.toMillis());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            healthy.set(false);
            throw new RuntimeException(e);
        }

        return null;
    }

    /**
     * Parallel index workers drain queue.
     */
    private Void indexLoop() {

        try {
            while (running.get() || !queue.isEmpty()) {

                FeedEntry entry = queue.poll(5, TimeUnit.SECONDS);

                if (entry != null) {
                    indexer.index(entry);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            healthy.set(false);
            throw new RuntimeException(e);
        }

        return null;
    }

    /**
     * Graceful stop signal.
     */
    public void stop() {
        running.set(false);
    }
}
