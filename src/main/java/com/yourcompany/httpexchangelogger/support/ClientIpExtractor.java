package com.yourcompany.httpexchangelogger.support;

import jakarta.servlet.http.HttpServletRequest;

public interface ClientIpExtractor {

    String extract(HttpServletRequest request);
}
