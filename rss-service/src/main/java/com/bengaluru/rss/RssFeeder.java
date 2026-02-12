package com.bengaluru.rss;

import java.net.URI;
import java.util.List;

public interface RssFeeder {

    String name();

    URI feedUri();

    List<FeedEntry> fetchEntries() throws Exception;
}
