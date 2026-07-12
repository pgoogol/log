package com.pgoogol.httpexchangelogger.sink;

import com.pgoogol.httpexchangelogger.model.HttpExchangeLogEvent;
import com.pgoogol.httpexchangelogger.model.HttpLogMode;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.awaitility.Awaitility.await;

class AsyncHttpExchangeLogSinkTest {

    private static HttpExchangeLogEvent event(String requestId) {

        return HttpExchangeLogEvent.builder()
                .requestId(requestId)
                .method("GET")
                .path("/api/orders")
                .status(200)
                .durationMs(1)
                .configuredMode(HttpLogMode.BASIC)
                .effectiveMode(HttpLogMode.BASIC)
                .build();
    }

    @Test
    void log_whenEventQueued_deliversItToDelegateAsynchronously() {

        // given
        RecordingSink delegate = new RecordingSink();
        AsyncHttpExchangeLogSink sink = new AsyncHttpExchangeLogSink(delegate, 10, 2_000);

        // when
        sink.log(event("req-1"));

        // then
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(delegate.getEvents()).hasSize(1));
        sink.close();
    }

    @Test
    void log_whenQueueIsFull_dropsEventInsteadOfBlocking() throws InterruptedException {

        // given a delegate that blocks the worker, and a queue of capacity 1
        BlockingSink delegate = new BlockingSink();
        AsyncHttpExchangeLogSink sink = new AsyncHttpExchangeLogSink(delegate, 1, 2_000);

        // when the worker is busy with the first event and the queue is filled up
        sink.log(event("req-1"));
        boolean workerBusy = delegate.entered.await(5, TimeUnit.SECONDS);
        sink.log(event("req-2"));
        sink.log(event("req-3"));

        // then the overflowing event was dropped without blocking the caller
        assertThat(workerBusy).isTrue();
        assertThat(sink.getDroppedCount()).isEqualTo(1);

        delegate.release.countDown();
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(delegate.delivered.get()).isEqualTo(2));
        sink.close();
    }

    @Test
    void close_whenEventsPending_drainsQueueBeforeReturning() {

        // given
        RecordingSink delegate = new RecordingSink();
        AsyncHttpExchangeLogSink sink = new AsyncHttpExchangeLogSink(delegate, 100, 5_000);
        IntStream.range(0, 50).forEach(i -> sink.log(event("req-" + i)));

        // when
        sink.close();

        // then every queued event reached the delegate before close returned
        assertThat(delegate.getEvents()).hasSize(50);
    }

    @Test
    void log_whenCalledAfterClose_isSilentlyIgnored() {

        // given
        RecordingSink delegate = new RecordingSink();
        AsyncHttpExchangeLogSink sink = new AsyncHttpExchangeLogSink(delegate, 10, 2_000);
        sink.close();

        // when
        Throwable thrown = catchThrowable(() -> sink.log(event("late")));

        // then
        assertThat(thrown).isNull();
        assertThat(delegate.getEvents()).isEmpty();
        assertThat(sink.getDroppedCount()).isZero();
    }

    @Test
    void close_whenCalledTwice_isIdempotent() {

        // given
        AsyncHttpExchangeLogSink sink = new AsyncHttpExchangeLogSink(new RecordingSink(), 10, 2_000);
        sink.close();

        // when
        Throwable thrown = catchThrowable(sink::close);

        // then
        assertThat(thrown).isNull();
    }

    private static class RecordingSink implements HttpExchangeLogSink {

        private final List<HttpExchangeLogEvent> events = new CopyOnWriteArrayList<>();

        @Override
        public void log(HttpExchangeLogEvent event) {

            events.add(event);
        }

        List<HttpExchangeLogEvent> getEvents() {

            return List.copyOf(events);
        }

    }

    private static class BlockingSink implements HttpExchangeLogSink {

        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final AtomicInteger delivered = new AtomicInteger();

        @Override
        public void log(HttpExchangeLogEvent event) {

            entered.countDown();
            try {

                boolean released = release.await(5, TimeUnit.SECONDS);
                if (!released) {

                    throw new IllegalStateException("BlockingSink was never released");
                }
            } catch (InterruptedException ex) {

                Thread.currentThread().interrupt();
            }
            delivered.incrementAndGet();
        }

    }

}
