package ru.practicum.ewm.comment.dto;

import lombok.*;
import ru.practicum.ewm.comment.model.CommentStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDto {
    private Long id;
    private Long eventId;
    private Long authorId;
    private String authorName;
    private String text;
    private CommentStatus status;
    private LocalDateTime created;
    private LocalDateTime updated;
}