package com.keepgoing.keepgoing.support;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatabaseCleaner {

	private final JdbcTemplate jdbcTemplate;

	public void clean() {
		List<String> tables = jdbcTemplate.queryForList("""
							select tablename
							from pg_tables
							where schemaname = 'public'
								and tablename not in ('flyway_schema_history')
				""", String.class);

		if (tables.isEmpty()) {
			return;
		}

		String tableNames = tables.stream()
				.map(table -> "\"public\".\"" + table + "\"")
				.collect(Collectors.joining(", "));

		jdbcTemplate.execute("truncate table " + tableNames + " restart identity cascade");
	}
}
