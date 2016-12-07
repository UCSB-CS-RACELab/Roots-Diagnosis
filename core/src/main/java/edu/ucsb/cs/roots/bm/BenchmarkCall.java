package edu.ucsb.cs.roots.bm;

import com.google.common.base.Strings;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkArgument;

public final class BenchmarkCall {

    private final String method;
    private final URI url;
    private final int timeoutInSeconds;
    private final RequestContent content;
    private final AtomicBoolean firstExecution;

    public BenchmarkCall(String method, String url, int timeoutInSeconds, RequestContent content) {
        checkArgument(!Strings.isNullOrEmpty(method), "HTTP method is required");
        checkArgument(!Strings.isNullOrEmpty(url), "HTTP URL is required");
        checkArgument(timeoutInSeconds > 0, "Timeout must be greater than zero");
        if (content != null) {
            checkArgument(method.equals("POST") || method.equals("PUT"),
                    "Content provided for entity non-enclosing method");
        }
        this.method = method;
        this.url = URI.create(url);
        this.content = content;
        this.timeoutInSeconds = timeoutInSeconds;
        this.firstExecution = new AtomicBoolean(true);
    }

    public BenchmarkCall(String method, String url, int timeoutInSeconds) {
        this(method, url, timeoutInSeconds, null);
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        String path = url.getPath();
        if (Strings.isNullOrEmpty(path)) {
            return "/";
        }
        return path;
    }

    public long execute(CloseableHttpClient client) throws IOException {
        HttpUriRequest request;
        switch (method) {
            case "GET":
                request = new HttpGet(url);
                break;
            case "POST":
                request = new HttpPost(url);
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        if (content != null) {
            HttpEntity entity = new StringEntity(content.getContent(),
                    ContentType.create(content.getContentType()));
            ((HttpEntityEnclosingRequest) request).setEntity(entity);
        }

        HttpClientContext context = HttpClientContext.create();
        context.setRequestConfig(RequestConfig.custom()
                .setSocketTimeout(timeoutInSeconds * 1000).build());

        HttpHost host = URIUtils.extractHost(url);
        long timeElapsed;
        long start = System.nanoTime();
        try (CloseableHttpResponse response = client.execute(host, request, context)) {
            EntityUtils.consumeQuietly(response.getEntity());
            long end = System.nanoTime();
            timeElapsed = Math.floorDiv(end - start, 1000000L);
        }

        if (firstExecution.compareAndSet(true, false)) {
            return execute(client);
        }
        return timeElapsed;
    }

}
