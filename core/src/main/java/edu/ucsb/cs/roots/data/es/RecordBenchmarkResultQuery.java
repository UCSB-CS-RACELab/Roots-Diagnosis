package edu.ucsb.cs.roots.data.es;

import com.google.gson.Gson;
import edu.ucsb.cs.roots.data.BenchmarkResult;
import edu.ucsb.cs.roots.data.ElasticSearchConfig;

import java.io.IOException;

public class RecordBenchmarkResultQuery extends Query<Void> {

    private static final Gson GSON = new Gson();

    private final BenchmarkResult result;

    public RecordBenchmarkResultQuery(BenchmarkResult result) {
        this.result = result;
    }

    @Override
    public Void run(ElasticSearchConfig es) throws IOException {
        String path = String.format("/%s/%s", es.getBenchmarkIndex(), result.getApplication());
        makeHttpCall(es, path);
        return null;
    }

    @Override
    protected String jsonString(ElasticSearchConfig es) {
        return GSON.toJson(result);
    }
}
