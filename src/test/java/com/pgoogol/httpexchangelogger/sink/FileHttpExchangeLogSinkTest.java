package com.pgoogol.httpexchangelogger.sink;

import com.pgoogol.httpexchangelogger.model.HttpExchangeLogEvent;
import com.pgoogol.httpexchangelogger.model.HttpLogMode;
import com.pgoogol.httpexchangelogger.serialization.HttpExchangeLogEventJsonWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class FileHttpExchangeLogSinkTest {

    private final HttpExchangeLogEventJsonWriter jsonWriter =
            new HttpExchangeLogEventJsonWriter(JsonMapper.builder().build());

    @TempDir
    Path tempDir;

    private static HttpExchangeLogEvent event(String requestId) {

        return HttpExchangeLogEvent.builder()
                .requestId(requestId)
                .method("GET")
                .path("/api/orders")
                .status(200)
                .durationMs(12)
                .configuredMode(HttpLogMode.BASIC)
                .effectiveMode(HttpLogMode.BASIC)
                .build();
    }

    @Test
    void log_whenEventGiven_writesSingleJsonLine() throws IOException {

        // given
        Path file = tempDir.resolve("logs/http-exchange.log");
        FileHttpExchangeLogSink sink = new FileHttpExchangeLogSink(jsonWriter, file);

        // when
        sink.log(event("req-1"));
        sink.close();

        // then
        List<String> lines = Files.readAllLines(file);
        assertThat(lines).hasSize(1);
        assertThat(lines.getFirst())
                .contains("\"type\":\"http_exchange\"")
                .contains("\"requestId\":\"req-1\"");
    }

    @Test
    void log_whenCalledForManyEvents_appendsOneLinePerEvent() throws IOException {

        // given
        Path file = tempDir.resolve("http-exchange.log");
        FileHttpExchangeLogSink sink = new FileHttpExchangeLogSink(jsonWriter, file);

        // when
        sink.log(event("req-1"));
        sink.log(event("req-2"));
        sink.log(event("req-3"));
        sink.close();

        // then
        List<String> lines = Files.readAllLines(file);
        assertThat(lines).hasSize(3);
        assertThat(lines.get(1)).contains("\"requestId\":\"req-2\"");
    }

    @Test
    void log_whenSinkAlreadyClosed_doesNotThrowAndWritesNothing() throws IOException {

        // given
        Path file = tempDir.resolve("closed.log");
        FileHttpExchangeLogSink sink = new FileHttpExchangeLogSink(jsonWriter, file);
        sink.close();

        // when
        Throwable thrown = catchThrowable(() -> sink.log(event("late")));

        // then
        assertThat(thrown).isNull();
        assertThat(Files.readAllLines(file)).isEmpty();
    }

    @Test
    void log_whenFileCannotBeOpened_degradesToNoopWithoutThrowing() {

        // given the target path is an existing directory, so the writer cannot be opened
        FileHttpExchangeLogSink sink = new FileHttpExchangeLogSink(jsonWriter, tempDir);

        // when
        Throwable thrown = catchThrowable(() -> {
            sink.log(event("req-1"));
            sink.close();
        });

        // then
        assertThat(thrown).isNull();
    }

    @Test
    void close_whenCalledTwice_isIdempotent() {

        // given
        FileHttpExchangeLogSink sink = new FileHttpExchangeLogSink(jsonWriter, tempDir.resolve("x.log"));
        sink.close();

        // when
        Throwable thrown = catchThrowable(sink::close);

        // then
        assertThat(thrown).isNull();
    }

}
