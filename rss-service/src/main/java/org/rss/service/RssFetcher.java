package org.rss.service;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class RssFetcher {

    private final HttpClient client = HttpClient.newHttpClient();

    public InputStream fetch(URI uri) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("User-Agent",
                        "SimpleRssCrawler/1.0 (contact@example.com)")
                .GET().build();

        HttpResponse<InputStream> response =
                client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        return response.body();
    }
}
