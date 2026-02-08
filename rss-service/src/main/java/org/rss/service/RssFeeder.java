package org.rss.service;

import java.net.URI;

public interface RssFeeder {
    String name();
    URI feedUri();
    void fetchAndIndex() throws Exception;
}
