package com.bengaluru.rss;

import java.net.URI;
import java.util.List;

public final class ToiRssFeeder implements RssFeeder {

    private static final URI FEED_URI = URI.create(
            "https://timesofindia.indiatimes.com/rssfeedstopstories.cms");

    private final RssFetcher fetcher;
    private final RssParser parser;

    public ToiRssFeeder(RssFetcher fetcher, RssParser parser) {
        this.fetcher = fetcher;
        this.parser = parser;
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
    public List<FeedEntry> fetchEntries() throws Exception {

        byte[] xml = fetcher.fetch(feedUri());

        return parser.parse(name(), xml);
    }
}
