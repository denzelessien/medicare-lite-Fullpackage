package com.example.MEDICARE.service;

import com.example.MEDICARE.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Sends transactional email through Brevo's REST API (https://developers.brevo.com).
 * No extra libraries needed. When no API key is configured the message is only logged,
 * so you can develop and test the whole sign-up flow without an email account.
 */
@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";

    @Value("${app.mail.brevo.api-key:}") private String apiKey;
    @Value("${app.mail.sender-email:}") private String senderEmail;
    @Value("${app.mail.sender-name:MediCare Lite}") private String senderName;
    @Value("${app.verification.code-ttl-minutes:10}") private int ttlMinutes;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && senderEmail != null && !senderEmail.isBlank();
    }

    /** @return true if a real email was sent, false if we are in dev mode (code logged to console). */
    public boolean sendVerificationCode(String toEmail, String toName, String code) {
        if (!isConfigured()) {
            log.warn("\n==============================================================\n"
                   + "  EMAIL NOT CONFIGURED (dev mode) - verification code for {}:  {}\n"
                   + "  Set BREVO_API_KEY and BREVO_SENDER_EMAIL to send real emails.\n"
                   + "==============================================================", toEmail, code);
            return false;
        }

        String html = "<div style=\"font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:24px;"
                + "border:1px solid #e3e8e6;border-radius:12px\">"
                + "<h2 style=\"color:#0e4749;margin-top:0\">Verify your email</h2>"
                + "<p>Hi " + escapeHtml(toName) + ",</p>"
                + "<p>Use this code to finish creating your MediCare Lite account:</p>"
                + "<p style=\"font-size:34px;letter-spacing:8px;font-weight:700;color:#0e4749;"
                + "background:#f1f6f4;padding:14px;text-align:center;border-radius:8px\">" + code + "</p>"
                + "<p style=\"color:#667\">The code expires in " + ttlMinutes + " minutes. "
                + "If you didn't sign up, you can ignore this email.</p></div>";

        String json = "{"
                + "\"sender\":{\"name\":" + q(senderName) + ",\"email\":" + q(senderEmail) + "},"
                + "\"to\":[{\"email\":" + q(toEmail) + ",\"name\":" + q(toName) + "}],"
                + "\"subject\":" + q("Your MediCare Lite verification code: " + code) + ","
                + "\"htmlContent\":" + q(html)
                + "}";

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(BREVO_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .header("api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> res = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                log.error("Brevo rejected the email ({}): {}", res.statusCode(), res.body());
                throw new ApiException(HttpStatus.BAD_GATEWAY,
                        "We couldn't send the verification email right now. Please try again shortly.");
            }
            return true;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Could not reach Brevo", e);
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "We couldn't send the verification email right now. Please try again shortly.");
        }
    }

    // ---- tiny JSON / HTML escaping helpers (keeps this class dependency-free) ----
    private static String q(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : (s == null ? "" : s).toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.append('"').toString();
    }

    private static String escapeHtml(String s) {
        return (s == null ? "" : s).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
