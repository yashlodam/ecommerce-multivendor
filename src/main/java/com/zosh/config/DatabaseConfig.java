package com.zosh.config;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.zaxxer.hikari.HikariDataSource;

/**
 * Production-ready DataSource configuration supporting both standard JDBC URLs
 * and cloud database URI formats (e.g. Render's postgres:// and postgresql://).
 *
 * When deployed on Render, the DATABASE_URL environment variable is provided in the format:
 *   postgres://username:password@hostname:5432/dbname
 *
 * This configuration automatically converts such URLs to valid JDBC format:
 *   jdbc:postgresql://hostname:5432/dbname
 * and extracts the username and password credentials.
 *
 * For local development or standard JDBC URLs starting with "jdbc:", the URL
 * passes through unchanged.
 */
@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource dataSource(DataSourceProperties properties) {
        String rawUrl = properties.getUrl();
        String username = properties.getUsername();
        String password = properties.getPassword();

        if (rawUrl != null && (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://"))) {
            log.info("Detected cloud PostgreSQL URI format. Converting to standard JDBC URL for HikariCP...");
            try {
                // Ensure URI format can be parsed by URI.create
                String cleanUriStr = rawUrl.startsWith("postgres://")
                        ? "postgresql://" + rawUrl.substring("postgres://".length())
                        : rawUrl;
                URI uri = URI.create(cleanUriStr);

                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = uri.getPath(); // includes leading '/'

                String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
                if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
                    jdbcUrl += "?" + uri.getQuery();
                }
                properties.setUrl(jdbcUrl);

                // Extract username and password from URI userInfo if not already set
                if (uri.getUserInfo() != null) {
                    String[] userInfo = uri.getUserInfo().split(":", 2);
                    if ((username == null || username.isBlank()) && userInfo.length > 0) {
                        properties.setUsername(userInfo[0]);
                    }
                    if ((password == null || password.isBlank()) && userInfo.length > 1) {
                        properties.setPassword(userInfo[1]);
                    }
                }
                log.info("Successfully converted cloud database URL to JDBC format (host={}, port={}, db={})", host, port, path);
            } catch (Exception e) {
                log.warn("Could not parse cloud database URI '{}', using URL as-is: {}", rawUrl, e.getMessage());
            }
        }

        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }
}
