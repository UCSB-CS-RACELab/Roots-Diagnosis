package edu.ucsb.cs.roots.data;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class ElasticSearchConfig {

    private final String host;
    private final int port;
    private final ImmutableMap<String,String> fieldMappings;
    private final boolean rawStringFilters;
    private final String accessLogIndex;
    private final String benchmarkIndex;
    private final String apiCallIndex;
    private final CloseableHttpClient client;

    private ElasticSearchConfig(Builder builder) {
        checkArgument(!Strings.isNullOrEmpty(builder.host), "Host is required");
        checkArgument(builder.port > 0 && builder.port < 65535, "Port number is invalid");
        checkNotNull(builder.fieldMappings, "Field mappings are required");
        checkArgument(builder.connectTimeout >= -1);
        checkArgument(builder.socketTimeout >= -1);
        this.host = builder.host;
        this.port = builder.port;
        this.fieldMappings = ImmutableMap.copyOf(builder.fieldMappings);
        this.rawStringFilters = builder.rawStringFilters;
        this.accessLogIndex = builder.accessLogIndex;
        this.benchmarkIndex = builder.benchmarkIndex;
        this.apiCallIndex = builder.apiCallIndex;

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(builder.connectTimeout)
                .setSocketTimeout(builder.socketTimeout)
                .build();
        this.client = HttpClients.custom().setDefaultRequestConfig(requestConfig).build();
    }

    void cleanup() {
        IOUtils.closeQuietly(client);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String field(String name, String def) {
        return fieldMappings.getOrDefault(name, def);
    }

    public String stringField(String name, String def) {
        String result = fieldMappings.getOrDefault(name, def);
        if (rawStringFilters) {
            return result + ".raw";
        }
        return result;
    }

    public String getAccessLogIndex() {
        checkArgument(!Strings.isNullOrEmpty(accessLogIndex), "Access log index not specified");
        return accessLogIndex;
    }

    public String getBenchmarkIndex() {
        checkArgument(!Strings.isNullOrEmpty(benchmarkIndex), "Benchmark index not specified");
        return benchmarkIndex;
    }

    public String getApiCallIndex() {
        checkArgument(!Strings.isNullOrEmpty(apiCallIndex), "API call index not specified");
        return apiCallIndex;
    }

    public CloseableHttpClient getClient() {
        return client;
    }


    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private String host;
        private int port;
        private String accessLogIndex;
        private String benchmarkIndex;
        private String apiCallIndex;
        private int connectTimeout = -1;
        private int socketTimeout = -1;
        private boolean rawStringFilters = true;

        private final Map<String,String> fieldMappings = new HashMap<>();

        private Builder() {
        }

        public Builder setHost(String host) {
            this.host = host;
            return this;
        }

        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        public Builder setAccessLogIndex(String accessLogIndex) {
            this.accessLogIndex = accessLogIndex;
            return this;
        }

        public Builder setBenchmarkIndex(String benchmarkIndex) {
            this.benchmarkIndex = benchmarkIndex;
            return this;
        }

        public Builder setApiCallIndex(String apiCallIndex) {
            this.apiCallIndex = apiCallIndex;
            return this;
        }

        public Builder setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder setSocketTimeout(int socketTimeout) {
            this.socketTimeout = socketTimeout;
            return this;
        }

        public Builder setFieldMapping(String field, String mapping) {
            fieldMappings.put(field, mapping);
            return this;
        }

        public Builder setRawStringFilters(boolean rawStringFilters) {
            this.rawStringFilters = rawStringFilters;
            return this;
        }

        public ElasticSearchConfig build() {
            return new ElasticSearchConfig(this);
        }
    }
}
