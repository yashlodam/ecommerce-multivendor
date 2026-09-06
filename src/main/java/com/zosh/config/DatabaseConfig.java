package com.zosh.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.zaxxer.hikari.HikariDataSource;

/**
 * Production-ready DataSource configuration supporting standard JDBC URLs,
 * cloud database URI formats (Supabase, Neon, Render), and connection poolers (PgBouncer/Supavisor).
 *
 * Automatically handles:
 * 1. Supabase Connection Pooler & Direct URIs (e.g. postgresql://postgres.ref:pass@host:5432/postgres)
 * 2. Supabase JDBC format with embedded credentials (?user=postgres.ref&password=pass)
 * 3. Mandatory SSL (sslmode=require) for cloud databases
 * 4. Special characters and percent-encoded database passwords
 * 5. Prepared-statement threshold tuning (prepareThreshold=0) for PgBouncer port 6543
 * 6. Explicit credential injection for HikariCP
 */
@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    // Resilient fallback regex parser for database URIs with special characters
    private static final Pattern DB_URI_PATTERN = Pattern.compile(
            "^(?:postgres(?:ql)?://)(?:([^:]+):(.*)@)?([^:/?#]+)(?::(\\d+))?(/[^?#]*)?(?:\\?(.*))?$"
    );

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource dataSource(DataSourceProperties properties) {
        String rawUrl = properties.getUrl();
        String username = properties.getUsername();
        String password = properties.getPassword();

        if (rawUrl != null && (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://"))) {
            log.info("Detected cloud PostgreSQL URI format (Supabase/Neon/Render). Converting for HikariCP...");

            String host = null;
            int port = 5432;
            String path = "/postgres";
            String query = null;
            String extractedUser = null;
            String extractedPass = null;

            // Attempt standard java.net.URI parse first
            try {
                String cleanUriStr = rawUrl.startsWith("postgres://")
                        ? "postgresql://" + rawUrl.substring("postgres://".length())
                        : rawUrl;
                URI uri = URI.create(cleanUriStr);
                host = uri.getHost();
                port = uri.getPort() > 0 ? uri.getPort() : 5432;
                path = (uri.getPath() != null && !uri.getPath().isEmpty()) ? uri.getPath() : "/postgres";
                query = uri.getQuery();

                if (uri.getUserInfo() != null) {
                    String[] userInfo = uri.getUserInfo().split(":", 2);
                    if (userInfo.length > 0) extractedUser = decode(userInfo[0]);
                    if (userInfo.length > 1) extractedPass = decode(userInfo[1]);
                }
            } catch (Exception e) {
                log.warn("Standard URI parse failed ({}); using resilient regex parser...", e.getMessage());
                Matcher matcher = DB_URI_PATTERN.matcher(rawUrl);
                if (matcher.matches()) {
                    extractedUser = decode(matcher.group(1));
                    extractedPass = decode(matcher.group(2));
                    host = matcher.group(3);
                    String portStr = matcher.group(4);
                    if (portStr != null && !portStr.isEmpty()) {
                        try {
                            port = Integer.parseInt(portStr);
                        } catch (NumberFormatException ignored) {}
                    }
                    path = matcher.group(5) != null ? matcher.group(5) : "/postgres";
                    query = matcher.group(6);
                }
            }

            if (host != null) {
                StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                        .append(host)
                        .append(":")
                        .append(port)
                        .append(path.startsWith("/") ? path : "/" + path);

                // Build query parameters — ensure SSL and handle poolers
                StringBuilder queryParams = new StringBuilder();
                if (query != null && !query.isBlank()) {
                    queryParams.append(query);
                }

                // Supabase and cloud databases require SSL
                boolean isRemote = !host.equalsIgnoreCase("localhost") && !host.equals("127.0.0.1");
                if (isRemote && !queryParams.toString().contains("sslmode")) {
                    if (queryParams.length() > 0) queryParams.append("&");
                    queryParams.append("sslmode=require");
                }

                // If connecting via PgBouncer / Supavisor transaction pooler (port 6543), disable prepared statements
                if (port == 6543 && !queryParams.toString().contains("prepareThreshold")) {
                    if (queryParams.length() > 0) queryParams.append("&");
                    queryParams.append("prepareThreshold=0");
                }

                if (queryParams.length() > 0) {
                    jdbcUrl.append("?").append(queryParams);
                }

                properties.setUrl(jdbcUrl.toString());

                if (extractedUser != null && !extractedUser.isBlank()) {
                    properties.setUsername(extractedUser);
                }
                if (extractedPass != null && !extractedPass.isBlank()) {
                    properties.setPassword(extractedPass);
                }

                log.info("Successfully configured cloud database: host={}, port={}, db={}, user={}, ssl={}",
                        host, port, path, properties.getUsername(), queryParams.toString().contains("sslmode"));
            }
        } else if (rawUrl != null && rawUrl.startsWith("jdbc:postgresql://")) {
            log.info("Inspecting JDBC PostgreSQL connection URL for Supabase/Cloud parameters...");

            // Check if user/password are embedded in the JDBC query string (e.g. Supabase format)
            int questionMarkIdx = rawUrl.indexOf('?');
            if (questionMarkIdx != -1) {
                String baseUrl = rawUrl.substring(0, questionMarkIdx);
                String queryString = rawUrl.substring(questionMarkIdx + 1);

                String[] pairs = queryString.split("&");
                Map<String, String> remainingParams = new LinkedHashMap<>();
                String extractedUser = null;
                String extractedPass = null;

                for (String pair : pairs) {
                    int eqIdx = pair.indexOf('=');
                    if (eqIdx != -1) {
                        String key = pair.substring(0, eqIdx);
                        String val = pair.substring(eqIdx + 1);
                        if ("user".equalsIgnoreCase(key)) {
                            extractedUser = decode(val);
                        } else if ("password".equalsIgnoreCase(key)) {
                            extractedPass = decode(val);
                        } else {
                            remainingParams.put(key, val);
                        }
                    } else if (!pair.isBlank()) {
                        remainingParams.put(pair, "");
                    }
                }

                if (extractedUser != null && !extractedUser.isBlank()) {
                    properties.setUsername(extractedUser);
                    log.info("Extracted database username from JDBC query string: {}", extractedUser);
                }
                if (extractedPass != null && !extractedPass.isBlank()) {
                    properties.setPassword(extractedPass);
                    log.info("Extracted database password from JDBC query string.");
                }

                // Ensure sslmode=require for Supabase / remote databases
                boolean isRemote = !baseUrl.contains("localhost") && !baseUrl.contains("127.0.0.1");
                if (isRemote && !remainingParams.containsKey("sslmode")) {
                    remainingParams.put("sslmode", "require");
                }
                if (baseUrl.contains(":6543") && !remainingParams.containsKey("prepareThreshold")) {
                    remainingParams.put("prepareThreshold", "0");
                }

                // Reconstruct clean JDBC URL without visible plaintext password in query string
                StringBuilder cleanJdbc = new StringBuilder(baseUrl);
                if (!remainingParams.isEmpty()) {
                    cleanJdbc.append("?");
                    boolean first = true;
                    for (Map.Entry<String, String> entry : remainingParams.entrySet()) {
                        if (!first) cleanJdbc.append("&");
                        cleanJdbc.append(entry.getKey());
                        if (!entry.getValue().isEmpty()) {
                            cleanJdbc.append("=").append(entry.getValue());
                        }
                        first = false;
                    }
                }
                properties.setUrl(cleanJdbc.toString());
            } else {
                // No query string — append sslmode=require if remote
                boolean isRemote = !rawUrl.contains("localhost") && !rawUrl.contains("127.0.0.1");
                if (isRemote) {
                    properties.setUrl(rawUrl + "?sslmode=require");
                    log.info("Appended sslmode=require to JDBC URL");
                }
            }
        }

        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    private static String decode(String value) {
        if (value == null) return null;
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }
}
