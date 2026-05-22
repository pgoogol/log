package com.pgoogol.httpexchangelogger.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "type",
        "requestId",
        "method",
        "path",
        "queryString",
        "clientIp",
        "status",
        "durationMs",
        "configuredMode",
        "effectiveMode",
        "requestHeaders",
        "responseHeaders",
        "requestBody",
        "responseBody",
        "requestBodyTruncated",
        "responseBodyTruncated",
        "exceptionClass",
        "exceptionMessage"
})
public final class HttpExchangeLogEvent {

    private String type = "http_exchange";
    private String requestId;
    private String method;
    private String path;
    private String queryString;
    private String clientIp;
    private int status;
    private long durationMs;
    private HttpLogMode configuredMode;
    private HttpLogMode effectiveMode;
    private Map<String, List<String>> requestHeaders;
    private Map<String, List<String>> responseHeaders;
    private Object requestBody;
    private Object responseBody;
    private boolean requestBodyTruncated;
    private boolean responseBodyTruncated;
    private String exceptionClass;
    private String exceptionMessage;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getQueryString() {
        return queryString;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public HttpLogMode getConfiguredMode() {
        return configuredMode;
    }

    public void setConfiguredMode(HttpLogMode configuredMode) {
        this.configuredMode = configuredMode;
    }

    public HttpLogMode getEffectiveMode() {
        return effectiveMode;
    }

    public void setEffectiveMode(HttpLogMode effectiveMode) {
        this.effectiveMode = effectiveMode;
    }

    public Map<String, List<String>> getRequestHeaders() {
        return requestHeaders;
    }

    public void setRequestHeaders(Map<String, List<String>> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public Map<String, List<String>> getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(Map<String, List<String>> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public Object getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(Object requestBody) {
        this.requestBody = requestBody;
    }

    public Object getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(Object responseBody) {
        this.responseBody = responseBody;
    }

    public boolean isRequestBodyTruncated() {
        return requestBodyTruncated;
    }

    public void setRequestBodyTruncated(boolean requestBodyTruncated) {
        this.requestBodyTruncated = requestBodyTruncated;
    }

    public boolean isResponseBodyTruncated() {
        return responseBodyTruncated;
    }

    public void setResponseBodyTruncated(boolean responseBodyTruncated) {
        this.responseBodyTruncated = responseBodyTruncated;
    }

    public String getExceptionClass() {
        return exceptionClass;
    }

    public void setExceptionClass(String exceptionClass) {
        this.exceptionClass = exceptionClass;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }
}
