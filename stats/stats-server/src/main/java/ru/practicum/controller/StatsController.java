package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.service.StatsService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class StatsController {

    private final StatsService service;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void saveHit(@RequestBody EndpointHitDto hitDto) {
        service.saveHit(hitDto);
    }

    @GetMapping("/stats")
    public List<ViewStatsDto> getStats(
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") Boolean unique) {

        try {
            LocalDateTime from = LocalDateTime.parse(start, FORMATTER);
            LocalDateTime to   = LocalDateTime.parse(end, FORMATTER);
            return service.getStats(from, to, uris, unique);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid date format. Expected yyyy-MM-dd HH:mm:ss");
        }
    }
}