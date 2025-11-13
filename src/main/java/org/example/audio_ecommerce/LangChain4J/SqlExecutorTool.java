package org.example.audio_ecommerce.LangChain4J;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SqlExecutorTool {

    private final JdbcTemplate jdbc;

    public List<Map<String, Object>> runSelect(String sql) {
        if (!sql.trim().toUpperCase().startsWith("SELECT")) {
            throw new RuntimeException("Only SELECT allowed");
        }
        return jdbc.queryForList(sql);
    }
}
