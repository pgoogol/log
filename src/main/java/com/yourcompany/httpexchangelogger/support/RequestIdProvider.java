package com.yourcompany.httpexchangelogger.support;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface RequestIdProvider {

    String getOrCreateRequestId(HttpServletRequest request, HttpServletResponse response);
}
