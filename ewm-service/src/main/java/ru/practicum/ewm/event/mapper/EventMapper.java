package ru.practicum.ewm.event.mapper;

import ru.practicum.ewm.category.mapper.CategoryMapper;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.Location;
import ru.practicum.ewm.user.mapper.UserMapper;
import ru.practicum.ewm.user.model.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EventMapper {
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static Event toEntity(NewEventDto dto, User initiator, Category category) {
        return Event.builder()
                .title(dto.getTitle())
                .annotation(dto.getAnnotation())
                .description(dto.getDescription())
                .eventDate(dto.getEventDate()) // Уже правильно - LocalDateTime
                .location(toLocation(dto.getLocation()))
                .paid(dto.getPaid())
                .participantLimit(dto.getParticipantLimit())
                .requestModeration(dto.getRequestModeration() != null ? dto.getRequestModeration() : true)
                .initiator(initiator)
                .category(category)
                .state(null)
                .createdOn(LocalDateTime.now())
                .publishedOn(null)
                .confirmedRequests(0L)
                .views(0L)
                .build();
    }

    public static EventFullDto toFullDto(Event event) {
        return EventFullDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .description(event.getDescription())
                .eventDate(event.getEventDate()) // LocalDateTime -> LocalDateTime (корректно)
                .createdOn(event.getCreatedOn())
                .publishedOn(event.getPublishedOn())
                .location(toLocationDto(event.getLocation()))
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.getRequestModeration())
                .confirmedRequests(event.getConfirmedRequests())
                .views(event.getViews())
                .category(CategoryMapper.toDto(event.getCategory()))
                .initiator(UserMapper.toDto(event.getInitiator()))
                .state(event.getState())
                .build();
    }

    public static EventShortDto toShortDto(Event event) {
        return EventShortDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .eventDate(formatDateTime(event.getEventDate())) // Форматируем в строку
                .paid(event.getPaid())
                .confirmedRequests(event.getConfirmedRequests())
                .views(event.getViews())
                .category(CategoryMapper.toDto(event.getCategory()))
                .initiator(UserMapper.toDto(event.getInitiator()))
                .build();
    }

    private static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : null;
    }

    public static Location toLocation(LocationDto dto) {
        return dto == null ? null : Location.builder()
                .lat(dto.getLat())
                .lon(dto.getLon())
                .build();
    }

    public static LocationDto toLocationDto(Location loc) {
        return loc == null ? null : LocationDto.builder()
                .lat(loc.getLat())
                .lon(loc.getLon())
                .build();
    }
}