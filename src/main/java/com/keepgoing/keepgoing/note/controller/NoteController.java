package com.keepgoing.keepgoing.note.controller;

import com.keepgoing.keepgoing.global.api.response.ApiResponse;
import com.keepgoing.keepgoing.global.api.response.PagedResponse;
import com.keepgoing.keepgoing.note.controller.dto.NoteCreateRequest;
import com.keepgoing.keepgoing.note.controller.dto.NoteDetailResponse;
import com.keepgoing.keepgoing.note.controller.dto.NoteSummaryResponse;
import com.keepgoing.keepgoing.note.controller.dto.NoteUpdateRequest;
import com.keepgoing.keepgoing.note.service.NoteService;
import com.keepgoing.keepgoing.note.service.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private static final int MAX_PAGE_SIZE = 100;

    private final NoteService noteService;

    /**
     * 노트 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<NoteDetailResponse>> createNote(
            @RequestBody @Valid NoteCreateRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        NoteCreateCommand command = request.toCommand(userId);

        NoteDetailResult created = noteService.createNote(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(NoteDetailResponse.from(created)));
    }

    /**
     * 단일 노트 조회
     */
    @GetMapping("/{noteId}")
    public ResponseEntity<ApiResponse<NoteDetailResponse>> getNote(@PathVariable Long noteId) {
        NoteDetailResult note = noteService.getNote(noteId);

        return ResponseEntity.ok(ApiResponse.success(NoteDetailResponse.from(note)));
    }

    /**
     * 노트 목록 조회
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PagedResponse<NoteSummaryResponse>>> getNotes(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            @AuthenticationPrincipal Long userId
    ) {
        Pageable safePageable = safePageable(pageable);

        Page<NoteSummaryResult> page = noteService.getNotes(userId, safePageable);

        List<NoteSummaryResponse> contents = page.getContent().stream()
                .map(NoteSummaryResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(page, contents)));
    }

    /**
     * 노트 수정
     */
    @PutMapping("/{noteId}")
    public ResponseEntity<ApiResponse<NoteDetailResponse>> updateNote(
            @PathVariable Long noteId,
            @RequestBody @Valid NoteUpdateRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        NoteUpdateCommand command = request.toCommand(noteId, userId);

        NoteDetailResult updated = noteService.updateNote(command);

        return ResponseEntity.ok(ApiResponse.success(NoteDetailResponse.from(updated)));
    }

    /**
     * 노트 삭제
     */
    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> deleteNote(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long noteId
    ) {
        noteService.deleteNote(userId, noteId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 내 노트 검색
     */
    @GetMapping("/me/search")
    public ResponseEntity<ApiResponse<PagedResponse<NoteSummaryResponse>>> searchMyNotes(
            @RequestParam String keyword,
            @PageableDefault(size = 10)
            Pageable pageable,
            @AuthenticationPrincipal Long userId
    ) {
        Pageable safePageable = safePageableUnsorted(pageable);

        Page<NoteSummaryResult> page = noteService.searchMyNotes(
                userId,
                new NoteSearchQuery(keyword, safePageable)
        );

        List<NoteSummaryResponse> contents = page.getContent().stream()
                .map(NoteSummaryResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(page, contents)));
    }

    /**
     * 노트 검색
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedResponse<NoteSummaryResponse>>> search(
            @RequestParam String keyword,
            @PageableDefault(size = 10)
            Pageable pageable
    ) {
        Pageable safePageable = safePageableUnsorted(pageable);

        Page<NoteSummaryResult> page = noteService.searchNote(
                new NoteSearchQuery(keyword, safePageable)
        );

        List<NoteSummaryResponse> contents = page.getContent().stream()
                .map(NoteSummaryResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(page, contents)));
    }

    private Pageable safePageable(Pageable pageable) {
        int safeSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        return PageRequest.of(
                pageable.getPageNumber(),
                safeSize,
                pageable.getSort()
        );
    }

    private Pageable safePageableUnsorted(Pageable pageable) {
        int safeSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        return PageRequest.of(
                pageable.getPageNumber(),
                safeSize
        );
    }
}
