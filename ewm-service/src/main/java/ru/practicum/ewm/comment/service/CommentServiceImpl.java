package ru.practicum.ewm.comment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.NewCommentDto;
import ru.practicum.ewm.comment.dto.UpdateCommentDto;
import ru.practicum.ewm.comment.mapper.CommentMapper;
import ru.practicum.ewm.comment.model.Comment;
import ru.practicum.ewm.comment.model.CommentStatus;
import ru.practicum.ewm.comment.repository.CommentRepository;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exceptions.ConflictException;
import ru.practicum.ewm.exceptions.NotFoundException;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CommentMapper mapper;

    private static PageRequest pageOf(int from, int size) {
        int page = from / size;
        return PageRequest.of(page, size);
    }

    private Event getPublishedEvent(Long eventId) {
        Event e = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found: " + eventId));
        if (e.getState() != EventState.PUBLISHED) {
            // Публичные комментарии допустимы только для опубликованных событий
            throw new NotFoundException("Event not published: " + eventId);
        }
        return e;
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }

    private Comment getComment(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Comment not found: " + id));
    }

    // ===== public =====

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getApprovedForEvent(Long eventId, int from, int size) {
        // Спрячем не-публикованные события из публичного API
        getPublishedEvent(eventId);
        return commentRepository
                .findAllByEventIdAndStatusOrderByCreatedAtDesc(eventId, CommentStatus.APPROVED, pageOf(from, size))
                .map(mapper::toDto)
                .getContent();
    }

    // ===== user =====

    @Override
    public CommentDto create(Long userId, Long eventId, NewCommentDto dto) {
        Event event = getPublishedEvent(eventId);
        User author = getUser(userId);

        Comment c = new Comment();
        c.setEvent(event);
        c.setAuthor(author);
        c.setText(dto.getText().trim());
        c.setStatus(CommentStatus.PENDING);
        c.setCreatedAt(LocalDateTime.now());

        return mapper.toDto(commentRepository.save(c));
    }

    @Override
    public CommentDto updateOwnPending(Long userId, Long commentId, UpdateCommentDto dto) {
        Comment c = getComment(commentId);
        if (!c.getAuthor().getId().equals(userId)) {
            // маскируем чужой ресурс под not found
            throw new NotFoundException("Comment not found");
        }
        if (c.getStatus() != CommentStatus.PENDING) {
            throw new ConflictException("Only PENDING comments can be edited by author");
        }
        c.setText(dto.getText().trim());
        c.setUpdatedAt(LocalDateTime.now());
        return mapper.toDto(commentRepository.save(c));
    }

    @Override
    public void deleteOwnPending(Long userId, Long commentId) {
        Comment c = getComment(commentId);
        if (!c.getAuthor().getId().equals(userId)) {
            throw new NotFoundException("Comment not found");
        }
        if (c.getStatus() != CommentStatus.PENDING) {
            throw new ConflictException("Only PENDING comments can be deleted by author");
        }
        commentRepository.deleteById(commentId);
    }

    // ===== admin =====

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getPending(int from, int size) {
        return commentRepository
                .findAllByStatusOrderByCreatedAtAsc(CommentStatus.PENDING, pageOf(from, size))
                .map(mapper::toDto)
                .getContent();
    }

    @Override
    public CommentDto approve(Long commentId, Long moderatorId) {
        Comment c = getComment(commentId);
        if (c.getStatus() != CommentStatus.PENDING) {
            throw new ConflictException("Only PENDING comment can be approved");
        }
        c.setStatus(CommentStatus.APPROVED);
        c.setUpdatedAt(LocalDateTime.now());
        if (moderatorId != null) {
            User mod = getUser(moderatorId);
            c.setModerator(mod);
        }
        c.setModerationReason(null);
        return mapper.toDto(commentRepository.save(c));
    }

    @Override
    public CommentDto reject(Long commentId, Long moderatorId, String reason) {
        Comment c = getComment(commentId);
        if (c.getStatus() != CommentStatus.PENDING) {
            throw new ConflictException("Only PENDING comment can be rejected");
        }
        c.setStatus(CommentStatus.REJECTED);
        c.setUpdatedAt(LocalDateTime.now());
        if (moderatorId != null) {
            User mod = getUser(moderatorId);
            c.setModerator(mod);
        }
        c.setModerationReason(reason);
        return mapper.toDto(commentRepository.save(c));
    }

    @Override
    public void deleteAsAdmin(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new NotFoundException("Comment not found: " + commentId);
        }
        commentRepository.deleteById(commentId);
    }
}