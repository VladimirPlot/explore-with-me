package ru.practicum.ewm.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventSort;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final StatsClient statsClient;

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto dto) {
        User user = getUserEntity(userId);
        Category category = categoryRepository.findById(dto.getCategory())
                .orElseThrow(() -> new NoSuchElementException("Category not found"));

        Event event = EventMapper.toEntity(dto, user, category);
        event.setState(EventState.PENDING);

        return EventMapper.toFullDto(eventRepository.save(event));
    }

    @Override
    public List<EventShortDto> findOwnEvents(Long userId, int from, int size) {
        User user = getUserEntity(userId);
        PageRequest page = PageRequest.of(from / size, size);
        return eventRepository.findAllByInitiator(user, page).stream()
                .map(EventMapper::toShortDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getOwnEvent(Long userId, Long eventId) {
        User user = getUserEntity(userId);
        Event event = getEventEntity(eventId);
        if (!event.getInitiator().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You are not the owner of this event.");
        }
        return EventMapper.toFullDto(event);
    }

    @Override
    public Event getEventEntity(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Event not found"));
    }

    @Override
    public User getUserEntity(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
    }

    @Override
    @Transactional
    public EventFullDto getPublicEvent(Long eventId, String ip, String uri) {
        Event event = getEventEntity(eventId);

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new IllegalArgumentException("Event is not published");
        }

        statsClient.hit(EndpointHitDto.builder()
                .app("ewm-service")
                .uri(uri)
                .ip(ip)
                .timestamp(java.time.LocalDateTime.now())
                .build());

        List<ViewStatsDto> stats = statsClient.getStats(
                java.time.LocalDateTime.now().minusYears(1),
                java.time.LocalDateTime.now().plusMinutes(1),
                List.of(uri), true);

        long views = stats.isEmpty() ? 0 : stats.get(0).getHits();
        event.setViews(views);

        return EventMapper.toFullDto(event);
    }

    @Override
    public List<EventShortDto> findPublic(String text, List<Long> categories, Boolean paid,
                                          String rangeStart, String rangeEnd, Boolean onlyAvailable,
                                          EventSort sort, int from, int size,
                                          String ip, String uri) {
        LocalDateTime start = rangeStart != null ? LocalDateTime.parse(rangeStart) : LocalDateTime.now();
        LocalDateTime end = rangeEnd != null ? LocalDateTime.parse(rangeEnd) : LocalDateTime.now().plusYears(100);
        PageRequest page = PageRequest.of(from / size, size);

        statsClient.hit(EndpointHitDto.builder()
                .app("ewm-service")
                .uri(uri)
                .ip(ip)
                .timestamp(LocalDateTime.now())
                .build());

        List<Event> events = eventRepository.findAllByEventDateBetweenAndState(start, end, EventState.PUBLISHED, page);

        return events.stream()
                .filter(e -> (text == null || e.getAnnotation().contains(text) || e.getDescription().contains(text)) &&
                        (categories == null || categories.contains(e.getCategory().getId())) &&
                        (paid == null || e.getPaid().equals(paid)) &&
                        (!onlyAvailable || e.getParticipantLimit() == 0 ||
                                e.getConfirmedRequests() < e.getParticipantLimit()))
                .map(EventMapper::toShortDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateOwnEvent(Long userId, Long eventId, UpdateEventUserRequest dto) {
        Event event = getEventEntity(eventId);
        if (!event.getInitiator().getId().equals(userId)) {
            throw new IllegalArgumentException("User is not the initiator");
        }
        if (event.getState() == EventState.PUBLISHED) {
            throw new IllegalStateException("Cannot update published event");
        }

        if (dto.getTitle() != null) event.setTitle(dto.getTitle());
        if (dto.getAnnotation() != null) event.setAnnotation(dto.getAnnotation());
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getEventDate() != null) event.setEventDate(dto.getEventDate());
        if (dto.getLocation() != null) event.setLocation(EventMapper.toLocation(dto.getLocation()));
        if (dto.getPaid() != null) event.setPaid(dto.getPaid());
        if (dto.getParticipantLimit() != null) event.setParticipantLimit(dto.getParticipantLimit());
        if (dto.getRequestModeration() != null) event.setRequestModeration(dto.getRequestModeration());
        if (dto.getCategory() != null) {
            Category category = categoryRepository.findById(dto.getCategory())
                    .orElseThrow(() -> new NoSuchElementException("Category not found"));
            event.setCategory(category);
        }

        return EventMapper.toFullDto(event);
    }

    @Override
    @Transactional
    public EventFullDto publishEvent(Long eventId) {
        Event event = getEventEntity(eventId);

        if (event.getState() != EventState.PENDING) {
            throw new IllegalStateException("Only pending events can be published");
        }

        event.setState(EventState.PUBLISHED);
        event.setPublishedOn(LocalDateTime.now());

        return EventMapper.toFullDto(event);
    }

    @Override
    @Transactional
    public EventFullDto rejectEvent(Long eventId) {
        Event event = getEventEntity(eventId);

        if (event.getState() == EventState.PUBLISHED) {
            throw new IllegalStateException("Cannot reject published event");
        }

        event.setState(EventState.CANCELED);
        return EventMapper.toFullDto(event);
    }

    @Override
    public List<EventFullDto> findAllAdmin(String text, List<Long> categories, Boolean paid,
                                           String rangeStart, String rangeEnd, EventSort sort,
                                           int from, int size) {
        LocalDateTime start = rangeStart != null ? LocalDateTime.parse(rangeStart) : LocalDateTime.MIN;
        LocalDateTime end = rangeEnd != null ? LocalDateTime.parse(rangeEnd) : LocalDateTime.now().plusYears(100);

        PageRequest page = PageRequest.of(from / size, size);

        return eventRepository.findAllByEventDateBetweenAndState(start, end, EventState.PENDING, page)
                .stream()
                .filter(e -> (text == null || e.getAnnotation().contains(text) || e.getDescription().contains(text)) &&
                        (categories == null || categories.contains(e.getCategory().getId())) &&
                        (paid == null || e.getPaid().equals(paid)))
                .map(EventMapper::toFullDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateByAdmin(Long eventId, UpdateEventAdminRequest dto) {
        Event event = getEventEntity(eventId);

        if (event.getState() == EventState.PUBLISHED) {
            throw new IllegalStateException("Published events can't be updated");
        }

        if (dto.getTitle() != null) event.setTitle(dto.getTitle());
        if (dto.getAnnotation() != null) event.setAnnotation(dto.getAnnotation());
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getEventDate() != null) event.setEventDate(dto.getEventDate());
        if (dto.getLocation() != null) event.setLocation(EventMapper.toLocation(dto.getLocation()));
        if (dto.getPaid() != null) event.setPaid(dto.getPaid());
        if (dto.getParticipantLimit() != null) event.setParticipantLimit(dto.getParticipantLimit());
        if (dto.getRequestModeration() != null) event.setRequestModeration(dto.getRequestModeration());
        if (dto.getCategory() != null) {
            Category category = categoryRepository.findById(dto.getCategory())
                    .orElseThrow(() -> new NoSuchElementException("Category not found"));
            event.setCategory(category);
        }
        if (dto.getState() != null) {
            event.setState(dto.getState());
        }

        return EventMapper.toFullDto(event);
    }
}