package ru.practicum.ewm.comment.service;

import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.NewCommentDto;
import ru.practicum.ewm.comment.dto.UpdateCommentDto;

import java.util.List;

public interface CommentService {
    List<CommentDto> getApprovedForEvent(Long eventId, int from, int size);

    CommentDto create(Long userId, Long eventId, NewCommentDto dto);

    CommentDto updateOwnPending(Long userId, Long commentId, UpdateCommentDto dto);

    void deleteOwnPending(Long userId, Long commentId);

    List<CommentDto> getPending(int from, int size);

    CommentDto approve(Long commentId, Long moderatorId);

    CommentDto reject(Long commentId, Long moderatorId, String reason);

    void deleteAsAdmin(Long commentId);
}