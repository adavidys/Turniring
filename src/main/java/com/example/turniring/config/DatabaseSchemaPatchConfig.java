package com.example.turniring.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class DatabaseSchemaPatchConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSchemaPatchConfig.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationStartedEvent.class)
    public void dropLegacyNotNullConstraintForTeamTournament() {
        if (!isPostgreSql()) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE IF EXISTS teams ALTER COLUMN tournament_id DROP NOT NULL");
        log.info("Applied schema patch: teams.tournament_id is nullable");
        int affectedRows = jdbcTemplate.update("UPDATE users SET role = 'USER' WHERE role IS NULL");
        if (affectedRows > 0) {
            log.info("Applied schema patch: users.role defaulted to USER for {} row(s)", affectedRows);
        }
    }

    private boolean isPostgreSql() {
        try (Connection connection = dataSource.getConnection()) {
            String databaseName = connection.getMetaData().getDatabaseProductName();
            return databaseName != null && databaseName.toLowerCase(Locale.ROOT).contains("postgresql");
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to detect database type for schema patch", exception);
        }
    }
}
