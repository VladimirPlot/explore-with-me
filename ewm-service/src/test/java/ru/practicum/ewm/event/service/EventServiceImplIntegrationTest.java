package ru.practicum.ewm.event.service;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.LocationDto;
import ru.practicum.ewm.event.dto.NewEventDto;
import ru.practicum.ewm.event.dto.UpdateEventUserRequest;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.model.Location;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exceptions.ConflictException;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EventServiceImplIntegrationTest {

    @Autowired
    private EventService eventService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private EventRepository eventRepository;

    private User user;
    private Category category;

    @BeforeEach
    void setup() {
        user = userRepository.save(new User(null, "Test User", "test@example.com"));
        category = categoryRepository.save(new Category(null, "Music"));
    }

    @Test
    void shouldCreateEvent() {
        NewEventDto dto = NewEventDto.builder()
                .title("Concert")
                .annotation("Live concert")
                .description("Amazing live concert event")
                .eventDate(LocalDateTime.now().plusDays(2))
                .location(new LocationDto(55.75, 37.61))
                .paid(false)
                .participantLimit(100)
                .requestModeration(true)
                .category(category.getId())
                .build();

        EventFullDto created = eventService.createEvent(user.getId(), dto);

        assertNotNull(created.getId());
        assertEquals(dto.getTitle(), created.getTitle());
        assertEquals(EventState.PENDING, created.getState());
    }

    @Test
    void shouldUpdateOwnEvent() {
        Event event = eventRepository.save(Event.builder()
                .title("Old Title")
                .annotation("Ann")
                .description("Desc")
                .eventDate(LocalDateTime.now().plusDays(3))
                .location(new Location(1.0, 2.0))
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .createdOn(LocalDateTime.now())
                .views(0L)
                .confirmedRequests(0L)
                .state(EventState.PENDING)
                .initiator(user)
                .category(category)
                .build());

        UpdateEventUserRequest dto = UpdateEventUserRequest.builder()
                .title("New Title")
                .description("Updated description")
                .build();

        EventFullDto updated = eventService.updateOwnEvent(user.getId(), event.getId(), dto);

        assertEquals("New Title", updated.getTitle());
        assertEquals("Updated description", updated.getDescription());
    }

    @Test
    void shouldReturnOwnEvent() {
        Event event = eventRepository.save(Event.builder()
                .title("Title")
                .annotation("A")
                .description("D")
                .eventDate(LocalDateTime.now().plusDays(3))
                .location(new Location(1.0, 2.0))
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .createdOn(LocalDateTime.now())
                .views(0L)
                .confirmedRequests(0L)
                .state(EventState.PENDING)
                .initiator(user)
                .category(category)
                .build());

        EventFullDto result = eventService.getOwnEvent(user.getId(), event.getId());

        assertEquals(event.getTitle(), result.getTitle());
    }

    @Test
    void shouldReturnPaginatedUserEvents() {
        for (int i = 0; i < 5; i++) {
            eventRepository.save(Event.builder()
                    .title("Event " + i)
                    .annotation("Ann " + i)
                    .description("Desc " + i)
                    .eventDate(LocalDateTime.now().plusDays(1))
                    .location(new Location(1.0, 2.0))
                    .paid(false)
                    .participantLimit(0)
                    .requestModeration(true)
                    .createdOn(LocalDateTime.now())
                    .views(0L)
                    .confirmedRequests(0L)
                    .state(EventState.PENDING)
                    .initiator(user)
                    .category(category)
                    .build());
        }

        var events = eventService.findOwnEvents(user.getId(), 0, 10);

        assertEquals(5, events.size());
        assertEquals("Event 0", events.get(0).getTitle());
    }

    @Test
    void shouldPublishPendingEvent() {
        Event event = eventRepository.save(Event.builder()
                .title("To Publish")
                .annotation("A")
                .description("D")
                .eventDate(LocalDateTime.now().plusDays(3))
                .location(new Location(1.0, 2.0))
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .createdOn(LocalDateTime.now())
                .views(0L)
                .confirmedRequests(0L)
                .state(EventState.PENDING)
                .initiator(user)
                .category(category)
                .build());

        EventFullDto result = eventService.publishEvent(event.getId());

        assertEquals(EventState.PUBLISHED, result.getState());
        assertNotNull(result.getPublishedOn());
    }

    @Test
    void shouldRejectPendingEvent() {
        Event event = eventRepository.save(Event.builder()
                .title("To Reject")
                .annotation("A")
                .description("D")
                .eventDate(LocalDateTime.now().plusDays(3))
                .location(new Location(1.0, 2.0))
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .createdOn(LocalDateTime.now())
                .views(0L)
                .confirmedRequests(0L)
                .state(EventState.PENDING)
                .initiator(user)
                .category(category)
                .build());

        EventFullDto result = eventService.rejectEvent(event.getId());

        assertEquals(EventState.CANCELED, result.getState());
    }

    @Test
    void createEvent_shouldFail_whenDateTooSoon() {
        NewEventDto dto = NewEventDto.builder()
                .title("Soon")
                .annotation("A".repeat(20))
                .description("D".repeat(20))
                .eventDate(LocalDateTime.now().plusMinutes(30))
                .location(new LocationDto(1.0, 2.0))
                .category(category.getId())
                .build();

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> eventService.createEvent(user.getId(), dto));
    }

    @Test
    void createEvent_shouldApplyDefaults_whenNullableFields() {
        NewEventDto dto = NewEventDto.builder()
                .title("Defaults")
                .annotation("A".repeat(20))
                .description("D".repeat(20))
                .eventDate(LocalDateTime.now().plusDays(1))
                .location(new LocationDto(1.0, 2.0))
                .category(category.getId())
                .build();

        EventFullDto created = eventService.createEvent(user.getId(), dto);

        assertEquals(Boolean.FALSE, created.getPaid());
        assertEquals(0, created.getParticipantLimit());
        assertEquals(Boolean.TRUE, created.getRequestModeration());
        assertEquals(EventState.PENDING, created.getState());
    }

    @Test
    void createEvent_shouldFail_whenCategoryNotFound() {
        NewEventDto dto = NewEventDto.builder()
                .title("NoCat")
                .annotation("A".repeat(20))
                .description("D".repeat(20))
                .eventDate(LocalDateTime.now().plusDays(1))
                .location(new LocationDto(1.0, 2.0))
                .category(999L)
                .build();

        assertThrows(NoSuchElementException.class,
                () -> eventService.createEvent(user.getId(), dto));
    }

    @Test
    void getOwnEvent_shouldFail_whenNotOwner() {
        User other = userRepository.save(new User(null, "Other", "other@example.com"));
        Event e = eventRepository.save(Event.builder()
                .title("X").annotation("A").description("D")
                .eventDate(LocalDateTime.now().plusDays(3))
                .location(new Location(1.0, 2.0))
                .paid(false).participantLimit(0).requestModeration(true)
                .createdOn(LocalDateTime.now()).views(0L).confirmedRequests(0L)
                .state(EventState.PENDING).initiator(other).category(category)
                .build());

        assertThrows(ConflictException.class,
                () -> eventService.getOwnEvent(user.getId(), e.getId()));
    }

    @Test
    void updateOwnEvent_shouldFail_whenPublished() {
        Event e = eventRepository.save(Event.builder()
                .title("T").annotation("A").description("D")
                .eventDate(LocalDateTime.now().plusDays(3))
                .location(new Location(1.0, 2.0))
                .paid(false).participantLimit(0).requestModeration(true)
                .createdOn(LocalDateTime.now()).views(0L).confirmedRequests(0L)
                .state(EventState.PUBLISHED).initiator(user).category(category)
                .build());

        UpdateEventUserRequest dto = UpdateEventUserRequest.builder()
                .title("New").build();

        assertThrows(ru.practicum.ewm.exceptions.ConflictException.class,
                () -> eventService.updateOwnEvent(user.getId(), e.getId(), dto));
    }

    @Test
    void updateOwnEvent_shouldFail_whenNewDateTooSoon() {
        Event e = eventRepository.save(Event.builder()
                .title("T").annotation("A").description("D")
                .eventDate(LocalDateTime.now().plusDays(3))
                .location(new Location(1.0, 2.0))
                .paid(false).participantLimit(0).requestModeration(true)
                .createdOn(LocalDateTime.now()).views(0L).confirmedRequests(0L)
                .state(EventState.PENDING).initiator(user).category(category)
                .build());

        UpdateEventUserRequest dto = UpdateEventUserRequest.builder()
                .eventDate(LocalDateTime.now().plusMinutes(30)) // < +2h
                .build();

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> eventService.updateOwnEvent(user.getId(), e.getId(), dto));
    }

    @Test
    void updateOwnEvent_shouldSendToReview_fromCanceledOrPending() {
        Event canceled = eventRepository.save(Event.builder()
                .title("T").annotation("A").description("D")
                .eventDate(LocalDateTime.now().plusDays(3))
                .location(new Location(1.0, 2.0))
                .paid(false).participantLimit(0).requestModeration(true)
                .createdOn(LocalDateTime.now()).views(0L).confirmedRequests(0L)
                .state(EventState.CANCELED).initiator(user).category(category)
                .build());

        UpdateEventUserRequest dto = UpdateEventUserRequest.builder()
                .stateAction(ru.practicum.ewm.event.model.StateActionUser.SEND_TO_REVIEW)
                .build();

        EventFullDto updated = eventService.updateOwnEvent(user.getId(), canceled.getId(), dto);
        assertEquals(EventState.PENDING, updated.getState());
    }

    @Test
    void updateOwnEvent_shouldCancelReview_onlyFromPending() {
        Event pending = eventRepository.save(Event.builder()
                .title("T").annotation("A").description("D")
                .eventDate(LocalDateTime.now().plusDays(3))
                .location(new Location(1.0, 2.0))
                .paid(false).participantLimit(0).requestModeration(true)
                .createdOn(LocalDateTime.now()).views(0L).confirmedRequests(0L)
                .state(EventState.PENDING).initiator(user).category(category)
                .build());

        UpdateEventUserRequest dto = UpdateEventUserRequest.builder()
                .stateAction(ru.practicum.ewm.event.model.StateActionUser.CANCEL_REVIEW)
                .build();

        EventFullDto updated = eventService.updateOwnEvent(user.getId(), pending.getId(), dto);
        assertEquals(EventState.CANCELED, updated.getState());
    }

    @Test
    void publishEvent_shouldFail_whenNotPending() {
        Event e = eventRepository.save(Event.builder()
                .title("T").annotation("A").description("D")
                .eventDate(LocalDateTime.now().plusDays(3))
                .location(new Location(1.0, 2.0))
                .paid(false).participantLimit(0).requestModeration(true)
                .createdOn(LocalDateTime.now()).views(0L).confirmedRequests(0L)
                .state(EventState.CANCELED).initiator(user).category(category)
                .build());

        assertThrows(ConflictException.class, () -> eventService.publishEvent(e.getId()));
    }

    @Test
    void rejectEvent_shouldFail_whenNotPending() {
        Event e = eventRepository.save(Event.builder()
                .title("T").annotation("A").description("D")
                .eventDate(LocalDateTime.now().plusDays(3))
                .location(new Location(1.0, 2.0))
                .paid(false).participantLimit(0).requestModeration(true)
                .createdOn(LocalDateTime.now()).views(0L).confirmedRequests(0L)
                .state(EventState.CANCELED).initiator(user).category(category)
                .build());

        assertThrows(ru.practicum.ewm.exceptions.ConflictException.class,
                () -> eventService.rejectEvent(e.getId()));
    }

    @Test
    void cancelEventByUser_shouldCancel_whenPendingAndOwner() {
        Event e = eventRepository.save(Event.builder()
                .title("T").annotation("A").description("D")
                .eventDate(LocalDateTime.now().plusDays(3))
                .location(new Location(1.0, 2.0))
                .paid(false).participantLimit(0).requestModeration(true)
                .createdOn(LocalDateTime.now()).views(0L).confirmedRequests(0L)
                .state(EventState.PENDING).initiator(user).category(category)
                .build());

        EventFullDto dto = eventService.cancelEventByUser(user.getId(), e.getId());
        assertEquals(EventState.CANCELED, dto.getState());
    }

    @Test
    void cancelEventByUser_shouldFail_whenNotOwner() {
        User other = userRepository.save(new User(null, "Other", "other@example.com"));
        Event e = eventRepository.save(Event.builder()
                .title("T").annotation("A").description("D")
                .eventDate(LocalDateTime.now().plusDays(3))
                .location(new Location(1.0, 2.0))
                .paid(false).participantLimit(0).requestModeration(true)
                .createdOn(LocalDateTime.now()).views(0L).confirmedRequests(0L)
                .state(EventState.PENDING).initiator(other).category(category)
                .build());

        assertThrows(ConflictException.class,
                () -> eventService.cancelEventByUser(user.getId(), e.getId()));
    }

    @Test
    void cancelEventByUser_shouldFail_whenNotPending() {
        Event e = eventRepository.save(Event.builder()
                .title("T").annotation("A").description("D")
                .eventDate(LocalDateTime.now().plusDays(3))
                .location(new Location(1.0, 2.0))
                .paid(false).participantLimit(0).requestModeration(true)
                .createdOn(LocalDateTime.now()).views(0L).confirmedRequests(0L)
                .state(EventState.CANCELED).initiator(user).category(category)
                .build());

        assertThrows(ru.practicum.ewm.exceptions.ConflictException.class,
                () -> eventService.cancelEventByUser(user.getId(), e.getId()));
    }

    @Test
    void findOwnEvents_shouldPaginateWithOffset() {
        for (int i = 0; i < 7; i++) {
            eventRepository.save(Event.builder()
                    .title("E" + i).annotation("A").description("D")
                    .eventDate(LocalDateTime.now().plusDays(1))
                    .location(new Location(1.0, 2.0))
                    .paid(false).participantLimit(0).requestModeration(true)
                    .createdOn(LocalDateTime.now()).views(0L).confirmedRequests(0L)
                    .state(EventState.PENDING).initiator(user).category(category)
                    .build());
        }

        var page1 = eventService.findOwnEvents(user.getId(), 0, 3);
        var page2 = eventService.findOwnEvents(user.getId(), 3, 3);
        var page3 = eventService.findOwnEvents(user.getId(), 6, 3);

        assertEquals(3, page1.size());
        assertEquals(3, page2.size());
        assertEquals(1, page3.size());
    }
}