package org.rss.service;

import java.time.Instant;

public record FeedEntry(String source, String id, String title, String link,
        String summary, Instant publishedAt) {
}
