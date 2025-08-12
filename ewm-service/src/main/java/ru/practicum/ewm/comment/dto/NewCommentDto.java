package ru.practicum.ewm.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NewCommentDto {

    @NotBlank
    @Size(min = 1, max = 5000, message = "text length must be 1..5000")
    private String text;
}