package ru.practicum.ewm.comment.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.model.Comment;

@Component
public class CommentMapper {

    public CommentDto toDto(Comment c) {
        return CommentDto.builder()
                .id(c.getId())
                .eventId(c.getEvent().getId())
                .authorId(c.getAuthor().getId())
                .authorName(c.getAuthor().getName()) // предполагается поле name в User
                .text(c.getText())
                .status(c.getStatus())
                .created(c.getCreatedAt())
                .updated(c.getUpdatedAt())
                .build();
    }
}