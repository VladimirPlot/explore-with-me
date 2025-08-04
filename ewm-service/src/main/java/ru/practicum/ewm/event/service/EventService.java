package ru.practicum.ewm.event.service;

import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventSort;
import ru.practicum.ewm.user.model.User;

import java.util.List;

public interface EventService {


    EventFullDto publishEvent(Long eventId);

    EventFullDto rejectEvent(Long eventId);

    EventFullDto updateByAdmin(Long eventId, UpdateEventAdminRequest dto);

    List<EventFullDto> findAllAdmin(String text, List<Long> categories, Boolean paid,
                                    String rangeStart, String rangeEnd, EventSort sort,
                                    int from, int size);


    EventFullDto createEvent(Long userId, NewEventDto dto);

    EventFullDto updateOwnEvent(Long userId, Long eventId, UpdateEventUserRequest dto);

    List<EventShortDto> findOwnEvents(Long userId, int from, int size);

    EventFullDto getOwnEvent(Long userId, Long eventId);


    List<EventShortDto> findPublic(String text, List<Long> categories, Boolean paid,
                                   String rangeStart, String rangeEnd, Boolean onlyAvailable,
                                   EventSort sort, int from, int size, String ip, String uri);

    EventFullDto getPublicEvent(Long eventId, String ip, String uri);


    Event getEventEntity(Long id);

    User getUserEntity(Long userId);
}