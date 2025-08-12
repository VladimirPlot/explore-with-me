package ru.practicum.ewm.compilation.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCompilationRequest {

    @Size(max = 50, message = "Title must not exceed 50 characters")
    private String title;

    private Boolean pinned;
    private Set<Long> events;
}