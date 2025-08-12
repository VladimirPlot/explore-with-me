package ru.practicum.ewm.comment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.comment.model.Comment;
import ru.practicum.ewm.comment.model.CommentStatus;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findAllByEventIdAndStatusOrderByCreatedAtDesc(Long eventId,
                                                                CommentStatus status,
                                                                Pageable pageable);

    Page<Comment> findAllByAuthorIdOrderByCreatedAtDesc(Long authorId, Pageable pageable);

    Page<Comment> findAllByStatusOrderByCreatedAtAsc(CommentStatus status, Pageable pageable);
}