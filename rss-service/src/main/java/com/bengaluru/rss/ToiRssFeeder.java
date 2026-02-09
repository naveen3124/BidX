package com.bengaluru.rss;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

public final class ToiRssFeeder implements RssFeeder {

    private static final URI FEED_URI = URI.create(
            "https://timesofindia.indiatimes.com/rssfeedstopstories.cms");

    private final RssFetcher fetcher;
    private final RssParser parser;
    private final LuceneIndexer indexer;

    public ToiRssFeeder(RssFetcher fetcher, RssParser parser,
            LuceneIndexer indexer) {
        this.fetcher = fetcher;
        this.parser = parser;
        this.indexer = indexer;
    }

    @Override
    public String name() {
        return "TimesOfIndia-TopStories";
    }

    @Override
    public URI feedUri() {
        return FEED_URI;
    }

    @Override
    public void fetchAndIndex() throws Exception {
        try (InputStream in = fetcher.fetch(feedUri())) {
            List<FeedEntry> entries = parser.parse(name(), in);
            for (FeedEntry e : entries) {
                indexer.index(e);
            }
            indexer.commit();
        }
    }
}
