package ru.practicum.ewm.event.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/events")
public class AdminEventController {

    private final EventService eventService;

    @GetMapping
    public List<EventFullDto> findAllAdmin(
            @RequestParam(required = false) List<Long> users,
            @RequestParam(required = false) List<EventState> states,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size
    ) {
        return eventService.findAllAdmin(users, states, categories, rangeStart, rangeEnd, null, from, size);
    }

    @PatchMapping("/{eventId}/publish")
    public EventFullDto publish(@PathVariable @Positive Long eventId) {
        return eventService.publishEvent(eventId);
    }

    @PatchMapping("/{eventId}/reject")
    public EventFullDto reject(@PathVariable @Positive Long eventId) {
        return eventService.rejectEvent(eventId);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto update(@PathVariable @Positive Long eventId,
                               @RequestBody @Valid UpdateEventAdminRequest dto) {
        return eventService.updateByAdmin(eventId, dto);
    }
}