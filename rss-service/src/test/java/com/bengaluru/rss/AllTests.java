package com.bengaluru.rss;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RomeRssTest {

    @Test
    void shouldParseTimesOfIndiaRssFeed() throws Exception {
        System.out.println(
                org.slf4j.LoggerFactory.getILoggerFactory().getClass()
              );
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder(
                URI.create("https://timesofindia.indiatimes.com/rssfeedstopstories.cms"))
            .header("User-Agent", "RssServiceTest/1.0")
            .GET()
            .build();

        HttpResponse<InputStream> response =
            client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        assertEquals(200, response.statusCode(), "HTTP request failed");

        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(response.body()));

        assertNotNull(feed, "Feed should not be null");
        assertNotNull(feed.getTitle(), "Feed title should exist");

        List<SyndEntry> entries = feed.getEntries();
        assertNotNull(entries, "Entries should not be null");
        assertFalse(entries.isEmpty(), "Feed should have at least one entry");

        // Print one entry for visual confirmation
        SyndEntry first = entries.get(0);
        System.out.println("Title: " + first.getTitle());
        System.out.println("Link : " + first.getLink());
    }
}
