package com.bengaluru.rss;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class RssFetcher {

    private final HttpClient client = HttpClient.newHttpClient();

    public byte[] fetch(URI uri) throws Exception {

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(10))   // 🔥 request timeout
                .header("User-Agent",
                        "SimpleRssCrawler/1.0 (contact@example.com)")
                .GET()
                .build();

        HttpResponse<byte[]> response =
                client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }

        return response.body();   // fully materialized
    }

}
