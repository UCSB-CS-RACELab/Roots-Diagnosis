package edu.ucsb.cs.roots.data;

import com.google.common.collect.ImmutableList;
import junit.framework.Assert;
import org.junit.Test;

public class ApplicationRequestTest {

    private ApiCall newApiCall(long timestamp, String service, String op, int timeElapsed) {
        return ApiCall.newBuilder()
                .setRequestTimestamp(timestamp)
                .setTimestamp(timestamp)
                .setService(service)
                .setOperation(op)
                .setTimeElapsed(timeElapsed)
                .setRequestOperation("GET /test")
                .build();
    }

    @Test
    public void testApiCall() {
        ApiCall call = newApiCall(100, "foo", "bar", 10);
        Assert.assertEquals("foo:bar", call.name());
    }

    @Test
    public void testPathString0() {
        ImmutableList<ApiCall> calls = ImmutableList.of();
        ApplicationRequest request = new ApplicationRequest("test", 100, "app", "op", calls);
        Assert.assertEquals("", request.getPathAsString());
        Assert.assertEquals(0, request.getResponseTime());
    }

    @Test
    public void testPathString1() {
        ImmutableList<ApiCall> calls = ImmutableList.of(
                newApiCall(100, "foo", "bar", 10)
        );
        ApplicationRequest request = new ApplicationRequest("test", 100, "app", "op", calls);
        Assert.assertEquals("foo:bar", request.getPathAsString());
        Assert.assertEquals(10, request.getResponseTime());
    }

    @Test
    public void testPathString2() {
        ImmutableList<ApiCall> calls = ImmutableList.of(
                newApiCall(100, "foo", "bar", 10),
                newApiCall(100, "foo", "baz", 10)
        );
        ApplicationRequest request = new ApplicationRequest("test", 100, "app", "op", calls);
        Assert.assertEquals("foo:bar, foo:baz", request.getPathAsString());
        Assert.assertEquals(20, request.getResponseTime());
    }

}
