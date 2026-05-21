package com.yourcompany.httpexchangelogger.support;

import java.util.Locale;

public final class ContentTypeMatcher {

    private ContentTypeMatcher() {
    }

    public static boolean isLoggable(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return false;
        }
        String normalized = normalize(contentType);
        return isJson(normalized)
                || isXml(normalized)
                || isFormUrlEncoded(normalized)
                || isText(normalized);
    }

    public static boolean isJson(String contentType) {
        if (contentType == null) {
            return false;
        }
        String normalized = normalize(contentType);
        if (normalized.equals("application/json") || normalized.startsWith("application/json;")) {
            return true;
        }
        return normalized.startsWith("application/") && normalized.contains("+json");
    }

    public static boolean isXml(String contentType) {
        if (contentType == null) {
            return false;
        }
        String normalized = normalize(contentType);
        if (normalized.equals("application/xml") || normalized.startsWith("application/xml;")) {
            return true;
        }
        return normalized.startsWith("application/") && normalized.contains("+xml");
    }

    public static boolean isFormUrlEncoded(String contentType) {
        if (contentType == null) {
            return false;
        }
        String normalized = normalize(contentType);
        return normalized.equals("application/x-www-form-urlencoded")
                || normalized.startsWith("application/x-www-form-urlencoded;");
    }

    public static boolean isText(String contentType) {
        if (contentType == null) {
            return false;
        }
        return normalize(contentType).startsWith("text/");
    }

    public static boolean isMultipart(String contentType) {
        if (contentType == null) {
            return false;
        }
        return normalize(contentType).startsWith("multipart/");
    }

    public static boolean isBinary(String contentType) {
        if (contentType == null) {
            return false;
        }
        String normalized = normalize(contentType);
        if (isMultipart(normalized)) {
            return true;
        }
        if (normalized.startsWith("image/")
                || normalized.startsWith("audio/")
                || normalized.startsWith("video/")) {
            return true;
        }
        return normalized.equals("application/octet-stream")
                || normalized.equals("application/pdf")
                || normalized.equals("application/zip");
    }

    private static String normalize(String contentType) {
        return contentType.toLowerCase(Locale.ROOT).trim();
    }
}
