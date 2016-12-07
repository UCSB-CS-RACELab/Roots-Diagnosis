package edu.ucsb.cs.roots.data.es;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import edu.ucsb.cs.roots.data.ElasticSearchConfig;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class Query<T> {

    private static final String SCROLL_QUERY = "{\"scroll_id\": \"%s\", \"scroll\": \"%s\"}";

    private final Logger log = LoggerFactory.getLogger(getClass());

    public abstract T run(ElasticSearchConfig es) throws IOException;
    protected abstract String jsonString(ElasticSearchConfig es);

    protected final JsonElement makeHttpCall(ElasticSearchConfig es, String url) throws IOException{
        return makeHttpCall(es, url, jsonString(es));
    }

    protected final JsonElement nextBatch(ElasticSearchConfig es, String scrollId) throws IOException {
        String json = String.format(SCROLL_QUERY, scrollId, "1m");
        return makeHttpCall(es, "/_search/scroll", json);
    }

    protected final JsonElement makeHttpCall(ElasticSearchConfig es, String uri, String json) throws IOException {
        String fullUri = String.format("http://%s:%d%s", es.getHost(), es.getPort(), uri);
        log.debug("URL: {}; Payload: {}", fullUri, json);
        HttpPost post = new HttpPost(fullUri);
        post.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
        return es.getClient().execute(post, new ElasticSearchResponseHandler());
    }

    private static class ElasticSearchResponseHandler implements ResponseHandler<JsonElement> {
        @Override
        public JsonElement handleResponse(HttpResponse response) throws IOException {
            int status = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            if (status != 200 && status != 201) {
                String error = entity != null ? EntityUtils.toString(entity) : null;
                throw new ClientProtocolException("Unexpected status code: " + status
                        + "; response: " + error);
            }

            if (entity == null) {
                return null;
            }
            JsonParser parser = new JsonParser();
            ContentType contentType = ContentType.getOrDefault(entity);
            Charset charset = contentType.getCharset() != null ?
                    contentType.getCharset() : Charset.defaultCharset();
            return parser.parse(new InputStreamReader(entity.getContent(), charset));
        }
    }

    static String loadTemplate(String name) {
        try (InputStream in = Query.class.getResourceAsStream(name)) {
            checkNotNull(in, "Failed to load resource: %s", name);
            return IOUtils.toString(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
