package com.pgoogol.httpexchangelogger.sink;

import com.pgoogol.httpexchangelogger.model.HttpExchangeLogEvent;
import com.pgoogol.httpexchangelogger.serialization.HttpExchangeLogEventJsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * Appends each HTTP exchange event as a single JSON line to a dedicated file,
 * independently of the application logging configuration.
 *
 * <p>The sink never breaks request handling: when the file cannot be opened it
 * degrades to a no-op (with an ERROR log), and write failures are counted and
 * reported with rate-limited WARN logs.
 */
public class FileHttpExchangeLogSink implements HttpExchangeLogSink, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(FileHttpExchangeLogSink.class);

    private static final int FAILURE_LOG_INTERVAL = 100;

    private final HttpExchangeLogEventJsonWriter jsonWriter;
    private final Path filePath;
    private final Object lock = new Object();

    private BufferedWriter writer;
    private boolean closed;
    private long failureCount;

    public FileHttpExchangeLogSink(HttpExchangeLogEventJsonWriter jsonWriter, Path filePath) {

        this.jsonWriter = jsonWriter;
        this.filePath = filePath;
        this.writer = openWriter(filePath);
    }

    private static BufferedWriter openWriter(Path filePath) {

        try {

            Path parent = filePath.toAbsolutePath().getParent();
            if (Objects.nonNull(parent)) {

                Files.createDirectories(parent);
            }
            return Files.newBufferedWriter(filePath, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } catch (IOException | RuntimeException ex) {

            // The application must keep working without exchange logs; the sink degrades to a no-op.
            LOG.error("Cannot open HTTP exchange log file {}; file sink stays disabled", filePath, ex);
            return null;
        }
    }

    @Override
    public void log(HttpExchangeLogEvent event) {

        if (Objects.isNull(event)) {

            return;
        }
        String json = jsonWriter.write(event);
        synchronized (lock) {

            if (closed || Objects.isNull(writer)) {

                return;
            }
            try {

                writer.write(json);
                writer.newLine();
                writer.flush();
            } catch (IOException ex) {

                failureCount++;
                if (failureCount % FAILURE_LOG_INTERVAL == 1) {

                    LOG.warn("Failed to write HTTP exchange log to {} ({} failures so far)",
                            filePath, failureCount, ex);
                }
            }
        }
    }

    @Override
    public void close() {

        synchronized (lock) {

            if (closed) {

                return;
            }
            closed = true;
            if (Objects.isNull(writer)) {

                return;
            }
            try {

                writer.close();
            } catch (IOException ex) {

                LOG.warn("Failed to close HTTP exchange log file {}", filePath, ex);
            }
        }
    }

    public Path getFilePath() {

        return filePath;
    }

}
