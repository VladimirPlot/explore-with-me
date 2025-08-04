package ru.practicum.ewm.event.dto;

import lombok.*;
import ru.practicum.ewm.event.model.EventState;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateEventAdminRequest {
    private String title;
    private String annotation;
    private String description;
    private Long category;
    private LocalDateTime eventDate;
    private LocationDto location;
    private Boolean paid;
    private Integer participantLimit;
    private Boolean requestModeration;
    private EventState state;
}