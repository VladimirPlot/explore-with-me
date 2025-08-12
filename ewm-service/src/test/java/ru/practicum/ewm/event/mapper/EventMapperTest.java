package ru.practicum.ewm.event.mapper;

import org.junit.jupiter.api.Test;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.model.Location;
import ru.practicum.ewm.user.model.User;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EventMapperTest {

    private final User user = new User(1L, "John", "john@example.com");
    private final Category category = new Category(1L, "Tech");

    @Test
    void toEntity_shouldMapNewEventDtoToEvent() {
        NewEventDto dto = NewEventDto.builder()
                .title("Title")
                .annotation("Annotation")
                .description("Description")
                .eventDate(LocalDateTime.now().plusDays(1))
                .location(new LocationDto(55.75, 37.61))
                .paid(true)
                .participantLimit(100)
                .requestModeration(false)
                .category(category.getId())
                .build();

        Event event = EventMapper.toEntity(dto, user, category);

        assertEquals(dto.getTitle(), event.getTitle());
        assertEquals(dto.getAnnotation(), event.getAnnotation());
        assertEquals(dto.getDescription(), event.getDescription());
        assertEquals(dto.getEventDate(), event.getEventDate());
        assertNotNull(event.getLocation());
        assertEquals(dto.getPaid(), event.getPaid());
        assertEquals(dto.getParticipantLimit(), event.getParticipantLimit());
        assertEquals(dto.getRequestModeration(), event.getRequestModeration());
        assertEquals(user, event.getInitiator());
        assertEquals(category, event.getCategory());
        assertEquals(0L, event.getViews());
        assertEquals(0L, event.getConfirmedRequests());
        assertNull(event.getState());
    }

    @Test
    void toFullDto_shouldMapEventToEventFullDto() {
        Event event = buildSampleEvent();

        EventFullDto dto = EventMapper.toFullDto(event);

        assertEquals(event.getId(), dto.getId());
        assertEquals(event.getTitle(), dto.getTitle());
        assertEquals(event.getAnnotation(), dto.getAnnotation());
        assertEquals(event.getDescription(), dto.getDescription());
        assertEquals(event.getEventDate(), dto.getEventDate());
        assertEquals(event.getCreatedOn(), dto.getCreatedOn());
        assertEquals(event.getPublishedOn(), dto.getPublishedOn());
        assertNotNull(dto.getLocation());
        assertEquals(event.getPaid(), dto.getPaid());
        assertEquals(event.getParticipantLimit(), dto.getParticipantLimit());
        assertEquals(event.getRequestModeration(), dto.getRequestModeration());
        assertEquals(event.getConfirmedRequests(), dto.getConfirmedRequests());
        assertEquals(event.getViews(), dto.getViews());
        assertEquals(event.getCategory().getId(), dto.getCategory().getId());
        assertEquals(event.getInitiator().getId(), dto.getInitiator().getId());
        assertEquals(event.getState(), dto.getState());
    }

    @Test
    void toLocation_shouldMapLocationDtoToLocation() {
        LocationDto dto = new LocationDto(12.34, 56.78);

        Location location = EventMapper.toLocation(dto);

        assertNotNull(location);
        assertEquals(dto.getLat(), location.getLat());
        assertEquals(dto.getLon(), location.getLon());
    }

    @Test
    void toLocationDto_shouldMapLocationToLocationDto() {
        Location location = new Location(12.34, 56.78);

        LocationDto dto = EventMapper.toLocationDto(location);

        assertNotNull(dto);
        assertEquals(location.getLat(), dto.getLat());
        assertEquals(location.getLon(), dto.getLon());
    }

    private Event buildSampleEvent() {
        return Event.builder()
                .id(1L)
                .title("Title")
                .annotation("Annotation")
                .description("Description")
                .eventDate(LocalDateTime.now().plusDays(1))
                .createdOn(LocalDateTime.now())
                .publishedOn(LocalDateTime.now())
                .location(new Location(55.75, 37.61))
                .paid(true)
                .participantLimit(100)
                .requestModeration(true)
                .confirmedRequests(5L)
                .views(123L)
                .category(category)
                .initiator(user)
                .state(EventState.PUBLISHED)
                .build();
    }
}