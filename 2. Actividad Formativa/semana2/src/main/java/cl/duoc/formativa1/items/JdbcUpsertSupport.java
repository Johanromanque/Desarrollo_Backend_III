package cl.duoc.formativa1.items;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcUpsertSupport {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcUpsertSupport(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void updateThenInsert(
            String updateSql,
            String insertSql,
            MapSqlParameterSource parameters) {

        if (jdbcTemplate.update(updateSql, parameters) > 0) {
            return;
        }

        try {
            jdbcTemplate.update(insertSql, parameters);
        } catch (DuplicateKeyException raceCondition) {
            if (jdbcTemplate.update(updateSql, parameters) == 0) {
                throw raceCondition;
            }
        }
    }
}
