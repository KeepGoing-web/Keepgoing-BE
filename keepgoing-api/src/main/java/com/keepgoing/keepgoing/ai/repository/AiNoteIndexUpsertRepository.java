package com.keepgoing.keepgoing.ai.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AiNoteIndexUpsertRepository {

	private static final String POSTGRESQL_UPSERT = """
			insert into ai_note_indexes (
				note_id,
				author_id,
				status,
				attempt_count,
				last_requested_at,
				last_error
			)
			values (?, ?, 'PENDING', 0, ?, null)
			on conflict (note_id) do update
			set author_id = excluded.author_id,
				status = 'PENDING',
				last_requested_at = excluded.last_requested_at,
				last_error = null
			""";

	private static final String H2_UPSERT = """
			merge into ai_note_indexes i
			using (
				select
					cast(? as bigint) as note_id,
					cast(? as bigint) as author_id,
					cast(? as timestamp) as last_requested_at
			) v
			on i.note_id = v.note_id
			when matched then update
				set author_id = v.author_id,
					status = 'PENDING',
					last_requested_at = v.last_requested_at,
					last_error = null
			when not matched then insert (
				note_id,
				author_id,
				status,
				attempt_count,
				last_requested_at,
				last_error
			)
			values (
				v.note_id,
				v.author_id,
				'PENDING',
				0,
				v.last_requested_at,
				null
			)
			""";

	private final JdbcTemplate jdbcTemplate;
	private final DataSource dataSource;

	public void upsertPending(Long noteId, Long authorId, LocalDateTime requestedAt) {
		jdbcTemplate.update(upsertSql(), noteId, authorId, requestedAt);
	}

	private String upsertSql() {
		if (isH2()) {
			return H2_UPSERT;
		}
		return POSTGRESQL_UPSERT;
	}

	private boolean isH2() {
		try (Connection connection = dataSource.getConnection()) {
			return connection.getMetaData().getDatabaseProductName().equalsIgnoreCase("H2");
		} catch (SQLException exception) {
			throw new IllegalStateException("Failed to detect database product for AI note index upsert.", exception);
		}
	}
}
