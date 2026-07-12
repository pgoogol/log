package com.pgoogol.httpexchangelogger.autoconfigure;

import com.pgoogol.httpexchangelogger.model.HttpLogMode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "http-exchange-logger")
public class HttpExchangeLoggerProperties {

    private boolean enabled = true;

    private HttpLogMode defaultMode = HttpLogMode.BASIC;

    private boolean requireTtlForFullLogging = false;

    private int maxBodyLength = 10_000;

    /**
     * Body length cap applied in LIMITED mode. Always bounded by maxBodyLength as an absolute ceiling.
     */
    private int limitedMaxBodyLength = 2_000;

    /**
     * Servlet filter order; runs early by default so the full exchange is captured.
     */
    private int filterOrder = Ordered.HIGHEST_PRECEDENCE + 10;

    private Sink sink = new Sink();

    private Include include = new Include();

    private Mask mask = new Mask();

    private Sampling sampling = new Sampling();

    private Async async = new Async();

    private List<Endpoint> endpoints = new ArrayList<>();

    public boolean isEnabled() {

        return enabled;
    }

    public void setEnabled(boolean enabled) {

        this.enabled = enabled;
    }

    public HttpLogMode getDefaultMode() {

        return defaultMode;
    }

    public void setDefaultMode(HttpLogMode defaultMode) {

        this.defaultMode = defaultMode;
    }

    public boolean isRequireTtlForFullLogging() {

        return requireTtlForFullLogging;
    }

    public void setRequireTtlForFullLogging(boolean requireTtlForFullLogging) {

        this.requireTtlForFullLogging = requireTtlForFullLogging;
    }

    public int getMaxBodyLength() {

        return maxBodyLength;
    }

    public void setMaxBodyLength(int maxBodyLength) {

        this.maxBodyLength = maxBodyLength;
    }

    public int getLimitedMaxBodyLength() {

        return limitedMaxBodyLength;
    }

    public void setLimitedMaxBodyLength(int limitedMaxBodyLength) {

        this.limitedMaxBodyLength = limitedMaxBodyLength;
    }

    public int getFilterOrder() {

        return filterOrder;
    }

    public void setFilterOrder(int filterOrder) {

        this.filterOrder = filterOrder;
    }

    public Sink getSink() {

        return sink;
    }

    public void setSink(Sink sink) {

        this.sink = sink;
    }

    public Include getInclude() {

        return include;
    }

    public void setInclude(Include include) {

        this.include = include;
    }

    public Mask getMask() {

        return mask;
    }

    public void setMask(Mask mask) {

        this.mask = mask;
    }

    public Sampling getSampling() {

        return sampling;
    }

    public void setSampling(Sampling sampling) {

        this.sampling = sampling;
    }

    public Async getAsync() {

        return async;
    }

    public void setAsync(Async async) {

        this.async = async;
    }

    public List<Endpoint> getEndpoints() {

        return endpoints;
    }

    public void setEndpoints(List<Endpoint> endpoints) {

        this.endpoints = endpoints;
    }

    public static class Sink {

        private boolean console = true;
        private boolean file = false;
        private boolean observability = false;

        /**
         * Target file for the file sink; parent directories are created on startup.
         */
        private String filePath = "logs/http-exchange.log";

        public boolean isConsole() {

            return console;
        }

        public void setConsole(boolean console) {

            this.console = console;
        }

        public boolean isFile() {

            return file;
        }

        public void setFile(boolean file) {

            this.file = file;
        }

        public String getFilePath() {

            return filePath;
        }

        public void setFilePath(String filePath) {

            this.filePath = filePath;
        }

        public boolean isObservability() {

            return observability;
        }

        public void setObservability(boolean observability) {

            this.observability = observability;
        }

    }

    public static class Include {

        private boolean headers = true;
        private boolean queryString = true;
        private boolean clientIp = true;

        public boolean isHeaders() {

            return headers;
        }

        public void setHeaders(boolean headers) {

            this.headers = headers;
        }

        public boolean isQueryString() {

            return queryString;
        }

        public void setQueryString(boolean queryString) {

            this.queryString = queryString;
        }

        public boolean isClientIp() {

            return clientIp;
        }

        public void setClientIp(boolean clientIp) {

            this.clientIp = clientIp;
        }

    }

    public static class Mask {

        private boolean enabled = true;

        private List<String> fields = new ArrayList<>(List.of(
                "password",
                "token",
                "accessToken",
                "refreshToken",
                "authorization",
                "cookie",
                "pesel",
                "email"
        ));

        public boolean isEnabled() {

            return enabled;
        }

        public void setEnabled(boolean enabled) {

            this.enabled = enabled;
        }

        public List<String> getFields() {

            return fields;
        }

        public void setFields(List<String> fields) {

            this.fields = fields;
        }

    }

    public static class Sampling {

        /**
         * Fraction of exchanges that are logged (0.0 - 1.0). 1.0 logs everything.
         */
        private double rate = 1.0d;

        public double getRate() {

            return rate;
        }

        public void setRate(double rate) {

            this.rate = rate;
        }

    }

    public static class Async {

        private boolean enabled = false;

        /**
         * Bounded queue between request threads and the sink worker; when full,
         * events are dropped (never blocking the request).
         */
        private int queueCapacity = 1_000;

        /**
         * How long shutdown waits for queued events to be flushed to the sinks.
         */
        private long shutdownTimeoutMs = 2_000;

        public boolean isEnabled() {

            return enabled;
        }

        public void setEnabled(boolean enabled) {

            this.enabled = enabled;
        }

        public int getQueueCapacity() {

            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {

            this.queueCapacity = queueCapacity;
        }

        public long getShutdownTimeoutMs() {

            return shutdownTimeoutMs;
        }

        public void setShutdownTimeoutMs(long shutdownTimeoutMs) {

            this.shutdownTimeoutMs = shutdownTimeoutMs;
        }

    }

    public static class Endpoint {

        private String pattern;
        private HttpLogMode mode;

        /**
         * Optional per-endpoint sampling rate overriding {@code sampling.rate}.
         */
        private Double sampleRate;

        public String getPattern() {

            return pattern;
        }

        public void setPattern(String pattern) {

            this.pattern = pattern;
        }

        public HttpLogMode getMode() {

            return mode;
        }

        public void setMode(HttpLogMode mode) {

            this.mode = mode;
        }

        public Double getSampleRate() {

            return sampleRate;
        }

        public void setSampleRate(Double sampleRate) {

            this.sampleRate = sampleRate;
        }

    }

}
