package ru.practicum.ewm.comment.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.service.CommentService;

import java.util.List;

@RestController
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
@Validated
public class AdminCommentController {

    private final CommentService commentService;

    @GetMapping("/pending")
    public List<CommentDto> pending(
            @PositiveOrZero @RequestParam(defaultValue = "0") Integer from,
            @Positive @RequestParam(defaultValue = "20") Integer size
    ) {
        return commentService.getPending(from, size);
    }

    @PatchMapping("/{commentId}/approve")
    public CommentDto approve(
            @PathVariable Long commentId,
            @RequestParam(required = false) Long moderatorId
    ) {
        return commentService.approve(commentId, moderatorId);
    }

    @PatchMapping("/{commentId}/reject")
    public CommentDto reject(
            @PathVariable Long commentId,
            @RequestParam(required = false) Long moderatorId,
            @RequestParam(required = false) String reason
    ) {
        return commentService.reject(commentId, moderatorId, reason);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long commentId) {
        commentService.deleteAsAdmin(commentId);
    }
}