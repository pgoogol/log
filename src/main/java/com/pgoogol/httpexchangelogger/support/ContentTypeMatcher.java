package com.pgoogol.httpexchangelogger.support;

import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;

public final class ContentTypeMatcher {

    private ContentTypeMatcher() {
    }

    public static boolean isLoggable(@Nullable String contentType) {
        if (Objects.isNull(contentType) || contentType.isBlank()) {
            return false;
        }
        return isJson(contentType)
                || isXml(contentType)
                || isFormUrlEncoded(contentType)
                || isText(contentType);
    }

    public static boolean isJson(@Nullable String contentType) {
        var base = baseType(contentType);
        if (Objects.isNull(base)) {
            return false;
        }
        if (Objects.equals(base, "application/json")) {
            return true;
        }
        return base.startsWith("application/") && base.endsWith("+json");
    }

    public static boolean isXml(@Nullable String contentType) {
        var base = baseType(contentType);
        if (Objects.isNull(base)) {
            return false;
        }
        if (Objects.equals(base, "application/xml")) {
            return true;
        }
        return base.startsWith("application/") && base.endsWith("+xml");
    }

    public static boolean isFormUrlEncoded(@Nullable String contentType) {
        var base = baseType(contentType);
        return Objects.equals(base, "application/x-www-form-urlencoded");
    }

    public static boolean isText(@Nullable String contentType) {
        var base = baseType(contentType);
        return Objects.nonNull(base) && base.startsWith("text/");
    }

    public static boolean isMultipart(@Nullable String contentType) {
        var base = baseType(contentType);
        return Objects.nonNull(base) && base.startsWith("multipart/");
    }

    public static boolean isBinary(@Nullable String contentType) {
        var base = baseType(contentType);
        if (Objects.isNull(base)) {
            return false;
        }
        if (base.startsWith("multipart/")
                || base.startsWith("image/")
                || base.startsWith("audio/")
                || base.startsWith("video/")) {
            return true;
        }
        return Objects.equals(base, "application/octet-stream")
                || Objects.equals(base, "application/pdf")
                || Objects.equals(base, "application/zip");
    }

    private static @Nullable String baseType(@Nullable String contentType) {
        if (Objects.isNull(contentType)) {
            return null;
        }
        var normalized = contentType.toLowerCase(Locale.ROOT).trim();
        var semicolon = normalized.indexOf(';');
        if (semicolon >= 0) {
            normalized = normalized.substring(0, semicolon);
        }
        return normalized.trim();
    }
}
