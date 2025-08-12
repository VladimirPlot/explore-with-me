package ru.practicum.ewm.compilation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewCompilationDto {

    @NotBlank(message = "Title must not be blank")
    @Size(max = 50, message = "Title must not exceed 50 characters")
    private String title;

    @Builder.Default
    private Boolean pinned = false;

    private Set<Long> events;
}