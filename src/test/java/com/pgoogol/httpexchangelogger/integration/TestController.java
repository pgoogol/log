package com.pgoogol.httpexchangelogger.integration;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping
public class TestController {

    @PostMapping(value = "/api/orders", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> createOrder(@RequestBody Map<String, Object> body) {
        return Map.of(
                "orderId", "ord_456",
                "status", "created",
                "echoedPassword", body.getOrDefault("password", "none")
        );
    }

    @GetMapping(value = "/api/orders/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getOrder(@org.springframework.web.bind.annotation.PathVariable String id,
                                        @RequestParam(name = "source", required = false) String source) {
        return Map.of("orderId", id, "source", source == null ? "unknown" : source);
    }

    @PostMapping(value = "/api/auth/login", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> login(@RequestBody Map<String, Object> body) {
        return Map.of("token", "fake-token");
    }

    @GetMapping("/actuator/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @GetMapping("/api/boom")
    public Map<String, String> boom() {
        throw new IllegalStateException("Cannot create order");
    }

    @PostMapping(value = "/api/binary", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] binary(@RequestBody byte[] payload) {
        return payload;
    }
}
