package com.keepgoing.keepgoing.ai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_note_chunks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiNoteChunk {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "note_id", nullable = false)
	private Long noteId;

	@Column(name = "author_id", nullable = false)
	private Long authorId;

	@Column(name = "chunk_order", nullable = false)
	private int chunkOrder;

	@Column(name = "title", nullable = false, length = 200)
	private String title;

	@Column(name = "content_chunk", nullable = false, columnDefinition = "TEXT")
	private String contentChunk;

	@Column(name = "source_updated_at", nullable = false)
	private LocalDateTime sourceUpdatedAt;

	@Column(name = "indexed_at", nullable = false)
	private LocalDateTime indexedAt;

	private AiNoteChunk(
			Long noteId,
			Long authorId,
			int chunkOrder,
			String title,
			String contentChunk,
			LocalDateTime sourceUpdatedAt,
			LocalDateTime indexedAt
	) {
		this.noteId = noteId;
		this.authorId = authorId;
		this.chunkOrder = chunkOrder;
		this.title = title;
		this.contentChunk = contentChunk;
		this.sourceUpdatedAt = sourceUpdatedAt;
		this.indexedAt = indexedAt;
	}

	public static AiNoteChunk create(
			Long noteId,
			Long authorId,
			int chunkOrder,
			String title,
			String contentChunk,
			LocalDateTime sourceUpdatedAt,
			LocalDateTime indexedAt
	) {
		return new AiNoteChunk(
				noteId,
				authorId,
				chunkOrder,
				title,
				contentChunk,
				sourceUpdatedAt,
				indexedAt
		);
	}
}
