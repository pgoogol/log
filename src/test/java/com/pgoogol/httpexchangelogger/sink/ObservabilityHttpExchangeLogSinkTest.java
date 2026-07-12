package com.pgoogol.httpexchangelogger.sink;

import com.pgoogol.httpexchangelogger.model.HttpExchangeLogEvent;
import com.pgoogol.httpexchangelogger.model.HttpLogMode;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ObservabilityHttpExchangeLogSinkTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    private static HttpExchangeLogEvent.Builder eventBuilder() {

        return HttpExchangeLogEvent.builder()
                .requestId("req-1")
                .method("POST")
                .path("/api/orders")
                .status(201)
                .durationMs(42)
                .configuredMode(HttpLogMode.BASIC)
                .effectiveMode(HttpLogMode.FULL);
    }

    @Test
    void log_whenEventGiven_recordsTimerWithLowCardinalityTags() {

        // given
        ObservabilityHttpExchangeLogSink sink = new ObservabilityHttpExchangeLogSink(() -> registry);

        // when
        sink.log(eventBuilder().build());

        // then
        Timer timer = registry.get(ObservabilityHttpExchangeLogSink.TIMER_NAME)
                .tag("method", "POST")
                .tag("status", "201")
                .tag("outcome", "SUCCESS")
                .tag("mode", "FULL")
                .tag("exception", "none")
                .timer();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(42.0d);
    }

    @Test
    void log_whenExceptionPresent_tagsSimpleExceptionClassName() {

        // given
        ObservabilityHttpExchangeLogSink sink = new ObservabilityHttpExchangeLogSink(() -> registry);

        // when
        sink.log(eventBuilder()
                .status(500)
                .exceptionClass("com.example.OrderException")
                .exceptionMessage("boom")
                .build());

        // then
        Timer timer = registry.get(ObservabilityHttpExchangeLogSink.TIMER_NAME)
                .tag("outcome", "SERVER_ERROR")
                .tag("exception", "OrderException")
                .timer();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void log_whenClientErrorStatus_mapsOutcomeToClientError() {

        // given
        ObservabilityHttpExchangeLogSink sink = new ObservabilityHttpExchangeLogSink(() -> registry);

        // when
        sink.log(eventBuilder().status(404).build());

        // then
        Timer timer = registry.get(ObservabilityHttpExchangeLogSink.TIMER_NAME)
                .tag("outcome", "CLIENT_ERROR")
                .timer();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void log_whenNoRegistryAvailable_staysNoopWithoutThrowing() {

        // given
        ObservabilityHttpExchangeLogSink sink = new ObservabilityHttpExchangeLogSink(() -> null);

        // when
        Throwable thrown = catchThrowable(() -> sink.log(eventBuilder().build()));

        // then
        assertThat(thrown).isNull();
    }

    @Test
    void log_whenEventIsNull_ignoresIt() {

        // given
        ObservabilityHttpExchangeLogSink sink = new ObservabilityHttpExchangeLogSink(() -> registry);

        // when
        sink.log(null);

        // then
        assertThat(registry.getMeters()).isEmpty();
    }

}
