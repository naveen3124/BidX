package com.bengaluru.rss;

import java.net.URI;
import java.util.List;

public final class HinduRssFeeder implements RssFeeder {

    private static final URI FEED_URI =
            URI.create("https://www.thehindu.com/feeder/default.rss");

    private final RssFetcher fetcher;
    private final RssParser parser;

    public HinduRssFeeder(RssFetcher fetcher, RssParser parser) {
        this.fetcher = fetcher;
        this.parser = parser;
    }

    @Override
    public String name() {
        return "TheHindu-TopStories";
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
