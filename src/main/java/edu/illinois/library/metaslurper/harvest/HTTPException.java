package edu.illinois.library.metaslurper.harvest;

import java.io.IOException;
import java.util.Optional;

/**
 * HTTP exception. Constructors are available for succeeded and failed
 * requests.
 */
public class HTTPException extends IOException {

    private Integer statusCode; // allow null
    private String method, requestBody, responseBody, uri;

    /**
     * Variant to use for failed requests.
     *
     * @param method Method.
     * @param uri    Request URI.
     */
    public HTTPException(String method,
                         String uri,
                         Exception cause) {
        super(method + " " + uri + ": " + cause.getMessage(), cause);
        this.method       = method;
        this.uri          = uri;
    }

    /**
     * @param method       Method.
     * @param uri          Request URI.
     * @param statusCode   Response status.
     * @param requestBody  Request body.
     * @param responseBody Response body.
     */
    public HTTPException(String method,
                         String uri,
                         int statusCode,
                         String requestBody,
                         String responseBody) {
        super("HTTP " + statusCode + " for " + method + " " + uri);
        this.method       = method;
        this.uri          = uri;
        this.statusCode   = statusCode;
        this.requestBody  = requestBody;
        this.responseBody = responseBody;
    }

    /**
     * @param method       Method.
     * @param uri          Request URI.
     * @param statusCode   Response status.
     * @param requestBody  Request body.
     * @param responseBody Response body.
     * @param cause        Cause.
     */
    public HTTPException(String method,
                         String uri,
                         int statusCode,
                         String requestBody,
                         String responseBody,
                         Exception cause) {
        super("HTTP " + statusCode + " for " + method + " " + uri, cause);
        this.method       = method;
        this.uri          = uri;
        this.statusCode   = statusCode;
        this.requestBody  = requestBody;
        this.responseBody = responseBody;
    }

    public String getMethod() {
        return method;
    }

    public Optional<String> getRequestBody() {
        return Optional.ofNullable(requestBody);
    }

    public Optional<String> getResponseBody() {
        return Optional.ofNullable(responseBody);
    }

    public Optional<Integer> getStatusCode() {
        return Optional.ofNullable(statusCode);
    }

    public String getURI() {
        return uri;
    }

}
