package com.zentro.server.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private final AtomicLong requestCount = new AtomicLong(0);
    private final AtomicLong totalBytesIn = new AtomicLong(0);
    private final AtomicLong totalBytesOut = new AtomicLong(0);
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long requestId = requestCount.incrementAndGet();
        long startTime = System.currentTimeMillis();

        String clientIp = getClientIp(request);
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String fullUrl = query != null ? uri + "?" + query : uri;
        int contentLength = request.getContentLength();

        // Track bytes in
        if (contentLength > 0) {
            totalBytesIn.addAndGet(contentLength);
        }

        // Print incoming request
        System.out.println();
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("  [REQ #" + requestId + "] " + method + " " + fullUrl);
        System.out.println("  Time: " + LocalDateTime.now().format(formatter));
        System.out.println("  From: " + clientIp);
        System.out.println("  Content-Type: " + (request.getContentType() != null ? request.getContentType() : "N/A"));
        System.out.println("  Content-Length: " + (contentLength > 0 ? contentLength + " bytes" : "0 bytes"));
        System.out.println("  User-Agent: " + request.getHeader("User-Agent"));
        if (query != null) {
            System.out.println("  Query: " + query);
        }
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // Wrap response to capture output
        ResponseCaptureWrapper capturedResponse = new ResponseCaptureWrapper(response);

        try {
            filterChain.doFilter(request, capturedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            int responseSize = capturedResponse.getOutputSize();

            // Track bytes out
            if (responseSize > 0) {
                totalBytesOut.addAndGet(responseSize);
            }

            // Color code by status
            String statusColor;
            if (status >= 200 && status < 300) {
                statusColor = "✓ " + status;
            } else if (status >= 300 && status < 400) {
                statusColor = "→ " + status;
            } else if (status >= 400 && status < 500) {
                statusColor = "✗ " + status;
            } else {
                statusColor = "✗✗ " + status;
            }

            // Print response
            System.out.println("  [RES #" + requestId + "] " + statusColor + " | " + duration + "ms | " + responseSize + " bytes");
            System.out.println("  Packets ↑: " + formatBytes(totalBytesOut.get()) + " total | ↓: " + formatBytes(totalBytesIn.get()) + " total | Requests: " + requestCount.get());
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    public long getRequestCount() {
        return requestCount.get();
    }

    public long getTotalBytesIn() {
        return totalBytesIn.get();
    }

    public long getTotalBytesOut() {
        return totalBytesOut.get();
    }

    // Inner class to capture response output
    private static class ResponseCaptureWrapper extends HttpServletResponseWrapper {
        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        private final PrintWriter writer;
        private boolean usingWriter = false;

        public ResponseCaptureWrapper(HttpServletResponse response) {
            super(response);
            StringWriter stringWriter = new StringWriter();
            this.writer = new PrintWriter(stringWriter);
        }

        @Override
        public PrintWriter getWriter() {
            usingWriter = true;
            return writer;
        }

        @Override
        public jakarta.servlet.ServletOutputStream getOutputStream() {
            return new jakarta.servlet.ServletOutputStream() {
                @Override
                public void write(int b) {
                    outputStream.write(b);
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setWriteListener(jakarta.servlet.WriteListener listener) {
                }
            };
        }

        public int getOutputSize() {
            if (usingWriter) {
                return writer.toString().getBytes(StandardCharsets.UTF_8).length;
            }
            return outputStream.size();
        }
    }
}
