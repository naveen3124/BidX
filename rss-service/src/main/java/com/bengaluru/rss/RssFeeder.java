package com.bengaluru.rss;

import java.net.URI;

public interface RssFeeder {
    String name();
    URI feedUri();
    void fetchAndIndex() throws Exception;
}
