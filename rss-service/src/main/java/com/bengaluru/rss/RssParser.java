package com.bengaluru.rss;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class RssParser {

    public List<FeedEntry> parse(String source, byte[] xmlBytes) throws Exception {

        try (InputStream in = new ByteArrayInputStream(xmlBytes);
             XmlReader reader = new XmlReader(in)) {

            SyndFeed feed = new SyndFeedInput().build(reader);

            List<FeedEntry> entries = new ArrayList<>();

            for (SyndEntry e : feed.getEntries()) {

                String id = e.getUri() != null
                        ? e.getUri()
                        : e.getLink();

                entries.add(new FeedEntry(
                        source,
                        id,
                        e.getTitle(),
                        e.getLink(),
                        e.getDescription() != null
                                ? e.getDescription().getValue()
                                : "",
                        e.getPublishedDate() != null
                                ? e.getPublishedDate().toInstant()
                                : Instant.now()
                ));
            }

            return entries;
        }
    }

}
