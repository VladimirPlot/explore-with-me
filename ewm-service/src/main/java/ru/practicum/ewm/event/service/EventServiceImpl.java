package ru.practicum.ewm.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.*;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exceptions.ConflictException;
import ru.practicum.ewm.exceptions.NotFoundException;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final StatsClient statsClient;
    private final ParticipationRequestRepository requestRepository;

    private void requireInitiator(Event event, Long userId) {
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("User is not the initiator of this event");
        }
    }

    private void requireState(Event event, EventState expected, String messageIfNot) {
        if (event.getState() != expected) {
            throw new ConflictException(messageIfNot + ". Current state: " + event.getState());
        }
    }

    private void requireFutureAtLeast(LocalDateTime when, long hours) {
        if (when.isBefore(LocalDateTime.now().plusHours(hours))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Event date must be at least " + hours + " hours from now"
            );
        }
    }

    private void refreshConfirmedCount(Event event) {
        long confirmed = requestRepository.countByEventAndStatus(
                event, ru.practicum.ewm.request.model.RequestStatus.CONFIRMED
        );
        event.setConfirmedRequests(confirmed);
    }

    private long resolveViewsForUri(String uri) {
        List<ViewStatsDto> stats = statsClient.getStats(
                LocalDateTime.now().minusYears(1),
                LocalDateTime.now().plusMinutes(1),
                List.of(uri),
                true
        );
        return stats.isEmpty() ? 0 : stats.get(0).getHits();
    }

    private Sort getSort(EventSort sort) {
        return sort == EventSort.VIEWS
                ? Sort.by(Sort.Direction.DESC, "views")
                : Sort.by(Sort.Direction.ASC, "eventDate");
    }

    private boolean matchesText(Event event, String text) {
        return text == null
                || event.getAnnotation().toLowerCase().contains(text.toLowerCase())
                || event.getDescription().toLowerCase().contains(text.toLowerCase());
    }

    private boolean isAvailable(Event event, Boolean onlyAvailable) {
        return !Boolean.TRUE.equals(onlyAvailable)
                || event.getParticipantLimit() == 0
                || event.getConfirmedRequests() < event.getParticipantLimit();
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
    public EventFullDto createEvent(Long userId, NewEventDto dto) {
        if (dto.getPaid() == null) dto.setPaid(false);
        if (dto.getParticipantLimit() == null) dto.setParticipantLimit(0);
        if (dto.getRequestModeration() == null) dto.setRequestModeration(true);

        requireFutureAtLeast(dto.getEventDate(), 2);

        User user = getUserEntity(userId);
        Category category = categoryRepository.findById(dto.getCategory())
                .orElseThrow(() -> new NoSuchElementException("Category not found"));

        Event event = EventMapper.toEntity(dto, user, category);
        event.setState(EventState.PENDING);

        Event saved = eventRepository.save(event);
        return EventMapper.toFullDto(saved);
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
        Event event = getEventEntity(eventId);
        requireInitiator(event, userId);
        return EventMapper.toFullDto(event);
    }

    @Override
    @Transactional
    public EventFullDto getPublicEvent(Long eventId, String ip, String uri) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event is not published");
        }

        statsClient.hit(EndpointHitDto.builder()
                .app("ewm-service")
                .uri(uri)
                .ip(ip)
                .timestamp(LocalDateTime.now())
                .build());

        long views = resolveViewsForUri(uri);
        event.setViews(views);

        return EventMapper.toFullDto(event);
    }

    @Override
    public List<EventShortDto> findPublic(String text, List<Long> categories, Boolean paid,
                                          LocalDateTime rangeStart, LocalDateTime rangeEnd, Boolean onlyAvailable,
                                          EventSort sort, int from, int size,
                                          String ip, String uri) {
        if (rangeStart == null) rangeStart = LocalDateTime.now();
        if (rangeEnd == null) rangeEnd = LocalDateTime.now().plusYears(100);
        if (rangeStart.isAfter(rangeEnd)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rangeStart must be before rangeEnd");
        }

        statsClient.hit(EndpointHitDto.builder()
                .app("ewm-service")
                .uri(uri)
                .ip(ip)
                .timestamp(LocalDateTime.now())
                .build());

        if (categories != null && categories.isEmpty()) {
            categories = null;
        }

        PageRequest page = PageRequest.of(from / size, size, getSort(sort));
        Page<Event> pageResult = eventRepository.findPublicEvents(
                EventState.PUBLISHED,
                rangeStart,
                rangeEnd,
                categories,
                paid,
                page
        );

        return pageResult.getContent().stream()
                .filter(e -> matchesText(e, text))
                .filter(e -> isAvailable(e, onlyAvailable))
                .map(EventMapper::toShortDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateOwnEvent(Long userId, Long eventId, UpdateEventUserRequest dto) {
        log.debug("Updating event {} for user {}. DTO: {}", eventId, userId, dto);
        Event event = getEventEntity(eventId);
        log.debug("Current event state: {}", event.getState());

        requireInitiator(event, userId);

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Published events cannot be modified");
        }

        if (dto.getStateAction() != null) {
            switch (dto.getStateAction()) {
                case SEND_TO_REVIEW -> {
                    if (event.getState() != EventState.CANCELED && event.getState() != EventState.PENDING) {
                        throw new ConflictException("Only canceled or pending events can be sent to review.");
                    }
                    event.setState(EventState.PENDING);
                }
                case CANCEL_REVIEW -> {
                    if (event.getState() != EventState.PENDING) {
                        throw new ConflictException("Only pending events can be canceled.");
                    }
                    event.setState(EventState.CANCELED);
                }
            }
        }

        applyUserUpdates(event, dto);

        event = eventRepository.save(event);
        return EventMapper.toFullDto(event);
    }

    private void applyUserUpdates(Event event, UpdateEventUserRequest dto) {
        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            event.setTitle(dto.getTitle());
        }
        if (dto.getAnnotation() != null && !dto.getAnnotation().isBlank()) {
            event.setAnnotation(dto.getAnnotation());
        }
        if (dto.getDescription() != null && !dto.getDescription().isBlank()) {
            event.setDescription(dto.getDescription());
        }
        if (dto.getEventDate() != null) {
            requireFutureAtLeast(dto.getEventDate(), 2);
            event.setEventDate(dto.getEventDate());
        }
        if (dto.getLocation() != null) {
            event.setLocation(EventMapper.toLocation(dto.getLocation()));
        }
        if (dto.getPaid() != null) {
            event.setPaid(dto.getPaid());
        }
        if (dto.getParticipantLimit() != null) {
            event.setParticipantLimit(dto.getParticipantLimit());
        }
        if (dto.getRequestModeration() != null) {
            event.setRequestModeration(dto.getRequestModeration());
        }
        if (dto.getCategory() != null) {
            Category category = categoryRepository.findById(dto.getCategory())
                    .orElseThrow(() -> new NoSuchElementException("Category not found"));
            event.setCategory(category);
        }
    }

    @Override
    @Transactional
    public EventFullDto publishEvent(Long eventId) {
        Event event = getEventEntity(eventId);
        requireState(event, EventState.PENDING, "Only pending events can be published");
        event.setState(EventState.PUBLISHED);
        event.setPublishedOn(LocalDateTime.now());
        return EventMapper.toFullDto(event);
    }

    @Override
    @Transactional
    public EventFullDto rejectEvent(Long eventId) {
        Event event = getEventEntity(eventId);
        requireState(event, EventState.PENDING, "Only pending events can be rejected");
        event.setState(EventState.CANCELED);
        return EventMapper.toFullDto(eventRepository.save(event));
    }

    @Override
    public List<EventFullDto> findAllAdmin(List<Long> users, List<EventState> states, List<Long> categories,
                                           LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                           EventSort sort, int from, int size) {
        PageRequest page = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findAll(page).getContent();

        List<Event> filtered = events.stream()
                .filter(e -> users == null || users.contains(e.getInitiator().getId()))
                .filter(e -> states == null || states.contains(e.getState()))
                .filter(e -> categories == null || categories.contains(e.getCategory().getId()))
                .filter(e -> rangeStart == null || !e.getEventDate().isBefore(rangeStart))
                .filter(e -> rangeEnd == null || !e.getEventDate().isAfter(rangeEnd))
                .collect(Collectors.toList());

        Comparator<Event> cmp = (sort == EventSort.VIEWS)
                ? Comparator.comparing(Event::getViews, Comparator.nullsFirst(Long::compareTo)).reversed()
                : Comparator.comparing(Event::getEventDate, Comparator.nullsFirst(LocalDateTime::compareTo));

        return filtered.stream()
                .sorted(cmp)
                .map(event -> {
                    refreshConfirmedCount(event);
                    long views = resolveViewsForUri("/events/" + event.getId());
                    event.setViews(views);
                    return EventMapper.toFullDto(event);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateByAdmin(Long eventId, UpdateEventAdminRequest dto) {
        Event event = getEventEntity(eventId);
        log.debug("Admin updating event {}. Current state: {}", eventId, event.getState());
        log.debug("Update data: {}", dto);

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Published events can't be updated");
        }

        applyAdminUpdates(event, dto);

        if (dto.getStateAction() != null) {
            switch (dto.getStateAction()) {
                case REJECT_EVENT -> {
                    requireState(event, EventState.PENDING, "Cannot reject event");
                    event.setState(EventState.CANCELED);
                }
                case PUBLISH_EVENT -> {
                    requireState(event, EventState.PENDING, "Cannot publish event");
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
                }
            }
        }

        log.debug("Event after update: {}", event);
        return EventMapper.toFullDto(eventRepository.save(event));
    }

    private void applyAdminUpdates(Event event, UpdateEventAdminRequest dto) {
        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            event.setTitle(dto.getTitle());
        }
        if (dto.getAnnotation() != null && !dto.getAnnotation().isBlank()) {
            event.setAnnotation(dto.getAnnotation());
        }
        if (dto.getDescription() != null && !dto.getDescription().isBlank()) {
            event.setDescription(dto.getDescription());
        }
        if (dto.getEventDate() != null) {
            requireFutureAtLeast(dto.getEventDate(), 1);
            event.setEventDate(dto.getEventDate());
        }
        if (dto.getLocation() != null) {
            event.setLocation(EventMapper.toLocation(dto.getLocation()));
        }
        if (dto.getPaid() != null) {
            event.setPaid(dto.getPaid());
        }
        if (dto.getParticipantLimit() != null) {
            event.setParticipantLimit(dto.getParticipantLimit());
        }
        if (dto.getRequestModeration() != null) {
            event.setRequestModeration(dto.getRequestModeration());
        }
        if (dto.getCategory() != null) {
            Category category = categoryRepository.findById(dto.getCategory())
                    .orElseThrow(() -> new NoSuchElementException("Category not found"));
            event.setCategory(category);
        }
    }

    @Override
    @Transactional
    public EventFullDto cancelEventByUser(Long userId, Long eventId) {
        Event event = getEventEntity(eventId);
        requireInitiator(event, userId);
        requireState(event, EventState.PENDING, "Only events in PENDING state can be canceled");
        event.setState(EventState.CANCELED);
        return EventMapper.toFullDto(event);
    }
}