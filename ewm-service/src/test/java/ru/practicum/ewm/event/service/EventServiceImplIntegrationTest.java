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
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
    void shouldThrowWhenUserTriesToAccessOthersEvent() {
        User otherUser = userRepository.save(new User(null, "Other User", "other@example.com"));
        Event event = eventRepository.save(Event.builder()
                .title("Foreign Event")
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
                .initiator(otherUser)
                .category(category)
                .build());

        var exception = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> eventService.getOwnEvent(user.getId(), event.getId()));

        assertEquals("You are not the owner of this event.", exception.getMessage());
    }

    @Test
    void shouldThrowWhenUpdatingPublishedEvent() {
        Event publishedEvent = eventRepository.save(Event.builder()
                .title("Published")
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
                .state(EventState.PUBLISHED)
                .initiator(user)
                .category(category)
                .build());

        UpdateEventUserRequest update = UpdateEventUserRequest.builder()
                .title("New title")
                .build();

        var exception = org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> eventService.updateOwnEvent(user.getId(), publishedEvent.getId(), update));

        assertEquals("Cannot update published event", exception.getMessage());
    }

    @Test
    void shouldThrowWhenUserTriesToUpdateOthersEvent() {
        User otherUser = userRepository.save(new User(null, "Other", "other@example.com"));

        Event event = eventRepository.save(Event.builder()
                .title("Foreign")
                .annotation("Ann")
                .description("Desc")
                .eventDate(LocalDateTime.now().plusDays(1))
                .location(new Location(1.0, 2.0))
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .createdOn(LocalDateTime.now())
                .views(0L)
                .confirmedRequests(0L)
                .state(EventState.PENDING)
                .initiator(otherUser)
                .category(category)
                .build());

        UpdateEventUserRequest update = UpdateEventUserRequest.builder().title("Hack").build();

        var exception = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> eventService.updateOwnEvent(user.getId(), event.getId(), update));

        assertEquals("User is not the initiator", exception.getMessage());
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
    void shouldThrowWhenPublishingNonPendingEvent() {
        Event event = eventRepository.save(Event.builder()
                .title("Already Published")
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
                .state(EventState.CANCELED) // не PENDING
                .initiator(user)
                .category(category)
                .build());

        var ex = org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> eventService.publishEvent(event.getId()));

        assertEquals("Only pending events can be published", ex.getMessage());
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
    void shouldThrowWhenRejectingPublishedEvent() {
        Event event = eventRepository.save(Event.builder()
                .title("Live Event")
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
                .state(EventState.PUBLISHED)
                .initiator(user)
                .category(category)
                .build());

        var ex = org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> eventService.rejectEvent(event.getId()));

        assertEquals("Cannot reject published event", ex.getMessage());
    }
}