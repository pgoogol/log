package com.pgoogol.httpexchangelogger.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContentTypeMatcherTest {

    @Test
    void recognizesJsonAndVariants() {
        assertThat(ContentTypeMatcher.isJson("application/json")).isTrue();
        assertThat(ContentTypeMatcher.isJson("application/json;charset=UTF-8")).isTrue();
        assertThat(ContentTypeMatcher.isJson("application/hal+json")).isTrue();
        assertThat(ContentTypeMatcher.isJson("application/xml")).isFalse();
        assertThat(ContentTypeMatcher.isJson(null)).isFalse();
    }

    @Test
    void recognizesXmlAndVariants() {
        assertThat(ContentTypeMatcher.isXml("application/xml")).isTrue();
        assertThat(ContentTypeMatcher.isXml("application/atom+xml")).isTrue();
        assertThat(ContentTypeMatcher.isXml("text/xml")).isFalse();
    }

    @Test
    void recognizesTextTypes() {
        assertThat(ContentTypeMatcher.isText("text/plain")).isTrue();
        assertThat(ContentTypeMatcher.isText("text/html;charset=utf-8")).isTrue();
        assertThat(ContentTypeMatcher.isText("application/json")).isFalse();
    }

    @Test
    void recognizesFormUrlEncoded() {
        assertThat(ContentTypeMatcher.isFormUrlEncoded("application/x-www-form-urlencoded")).isTrue();
        assertThat(ContentTypeMatcher.isFormUrlEncoded("application/x-www-form-urlencoded;charset=utf-8")).isTrue();
        assertThat(ContentTypeMatcher.isFormUrlEncoded("application/json")).isFalse();
    }

    @Test
    void detectsBinaryAndMultipart() {
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
    void isLoggableForTextualTypes() {
        assertThat(ContentTypeMatcher.isLoggable("application/json")).isTrue();
        assertThat(ContentTypeMatcher.isLoggable("application/xml")).isTrue();
        assertThat(ContentTypeMatcher.isLoggable("text/plain")).isTrue();
        assertThat(ContentTypeMatcher.isLoggable("application/x-www-form-urlencoded")).isTrue();
        assertThat(ContentTypeMatcher.isLoggable("image/png")).isFalse();
        assertThat(ContentTypeMatcher.isLoggable("multipart/form-data")).isFalse();
        assertThat(ContentTypeMatcher.isLoggable(null)).isFalse();
    }
}
