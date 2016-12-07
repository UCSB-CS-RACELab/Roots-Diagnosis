package edu.ucsb.cs.roots.bm;

import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class RequestContent {

    private final String content;
    private final String contentType;

    public RequestContent(String content, String contentType) {
        checkNotNull(content, "Content must not be null");
        checkArgument(!Strings.isNullOrEmpty(contentType), "Content type is required");
        this.content = content;
        this.contentType = contentType;
    }

    public String getContent() {
        return content;
    }

    public String getContentType() {
        return contentType;
    }
}
