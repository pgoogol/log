package com.pgoogol.httpexchangelogger.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContentTypeMatcherTest {

    @Test
    void isJson_whenJsonOrVariant_returnsTrue() {
        assertThat(ContentTypeMatcher.isJson("application/json")).isTrue();
        assertThat(ContentTypeMatcher.isJson("application/json;charset=UTF-8")).isTrue();
        assertThat(ContentTypeMatcher.isJson("application/hal+json")).isTrue();
        assertThat(ContentTypeMatcher.isJson("application/xml")).isFalse();
        assertThat(ContentTypeMatcher.isJson(null)).isFalse();
    }

    @Test
    void isXml_whenXmlOrVariant_returnsTrue() {
        assertThat(ContentTypeMatcher.isXml("application/xml")).isTrue();
        assertThat(ContentTypeMatcher.isXml("application/atom+xml")).isTrue();
        assertThat(ContentTypeMatcher.isXml("text/xml")).isFalse();
    }

    @Test
    void isText_whenTextType_returnsTrue() {
        assertThat(ContentTypeMatcher.isText("text/plain")).isTrue();
        assertThat(ContentTypeMatcher.isText("text/html;charset=utf-8")).isTrue();
        assertThat(ContentTypeMatcher.isText("application/json")).isFalse();
    }

    @Test
    void isFormUrlEncoded_whenFormUrlEncodedType_returnsTrue() {
        assertThat(ContentTypeMatcher.isFormUrlEncoded("application/x-www-form-urlencoded")).isTrue();
        assertThat(ContentTypeMatcher.isFormUrlEncoded("application/x-www-form-urlencoded;charset=utf-8")).isTrue();
        assertThat(ContentTypeMatcher.isFormUrlEncoded("application/json")).isFalse();
    }

    @Test
    void isBinary_whenBinaryOrMultipartType_returnsTrue() {
        assertThat(ContentTypeMatcher.isBinary("multipart/form-data;boundary=x")).isTrue();
        assertThat(ContentTypeMatcher.isBinary("application/octet-stream")).isTrue();
        assertThat(ContentTypeMatcher.isBinary("image/png")).isTrue();
        assertThat(ContentTypeMatcher.isBinary("audio/mpeg")).isTrue();
        assertThat(ContentTypeMatcher.isBinary("video/mp4")).isTrue();
        assertThat(ContentTypeMatcher.isBinary("application/pdf")).isTrue();
        assertThat(ContentTypeMatcher.isBinary("application/zip")).isTrue();
        assertThat(ContentTypeMatcher.isBinary("application/json")).isFalse();
    }

    @Test
    void isLoggable_whenTextualType_returnsTrue() {
        assertThat(ContentTypeMatcher.isLoggable("application/json")).isTrue();
        assertThat(ContentTypeMatcher.isLoggable("application/xml")).isTrue();
        assertThat(ContentTypeMatcher.isLoggable("text/plain")).isTrue();
        assertThat(ContentTypeMatcher.isLoggable("application/x-www-form-urlencoded")).isTrue();
        assertThat(ContentTypeMatcher.isLoggable("image/png")).isFalse();
        assertThat(ContentTypeMatcher.isLoggable("multipart/form-data")).isFalse();
        assertThat(ContentTypeMatcher.isLoggable(null)).isFalse();
    }

    @Test
    void isJson_whenWhitespaceBeforeParameterSeparator_toleratesIt() {
        assertThat(ContentTypeMatcher.isJson("application/json ; charset=utf-8")).isTrue();
        assertThat(ContentTypeMatcher.isLoggable("application/json ; charset=utf-8")).isTrue();
        assertThat(ContentTypeMatcher.isXml("application/xml ; charset=utf-8")).isTrue();
        assertThat(ContentTypeMatcher.isFormUrlEncoded("application/x-www-form-urlencoded ; x")).isTrue();
    }

    @Test
    void isBinary_whenParameterizedBinaryType_returnsTrue() {
        assertThat(ContentTypeMatcher.isBinary("application/pdf; name=a")).isTrue();
        assertThat(ContentTypeMatcher.isBinary("application/octet-stream;charset=binary")).isTrue();
        assertThat(ContentTypeMatcher.isBinary("application/zip ; x")).isTrue();
        assertThat(ContentTypeMatcher.isLoggable("application/pdf; name=a")).isFalse();
    }
}
