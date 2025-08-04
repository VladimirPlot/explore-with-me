package ru.practicum.ewm.compilation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewCompilationDto {
    @NotBlank(message = "Название подборки не может быть пустым")
    private String title;

    @Builder.Default
    private Boolean pinned = false;

    private Set<Long> events;
}