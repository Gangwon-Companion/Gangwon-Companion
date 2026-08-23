package com.gangwon.companion.domain.search.elasticsearch;

public class ElasticsearchOperationException extends RuntimeException {
    public ElasticsearchOperationException(String message) { super(message); }
    public ElasticsearchOperationException(String message, Throwable cause) { super(message, cause); }
}
