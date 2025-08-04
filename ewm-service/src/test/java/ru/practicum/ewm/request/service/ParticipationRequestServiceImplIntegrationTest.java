package ru.practicum.ewm.request.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.model.Location;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ParticipationRequestServiceImplIntegrationTest {

    @Autowired
    private ParticipationRequestService service;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private ParticipationRequestRepository requestRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    private User requester;
    private User initiator;
    private Event publishedEvent;
    private Event unpublishedEvent;

    @BeforeEach
    void setUp() {
        initiator = userRepository.save(new User(null, "Initiator", "init@mail.com"));
        requester = userRepository.save(new User(null, "User", "user@mail.com"));
        Category category = categoryRepository.save(new Category(null, "Music"));

        publishedEvent = eventRepository.save(Event.builder()
                .title("Published Event")
                .annotation("Fun")
                .description("desc")
                .eventDate(LocalDateTime.now().plusDays(1))
                .createdOn(LocalDateTime.now())
                .location(new Location(0.0, 0.0))
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .state(EventState.PUBLISHED)
                .confirmedRequests(0L)
                .views(0L)
                .initiator(initiator)
                .category(category)
                .build());

        unpublishedEvent = eventRepository.save(Event.builder()
                .title("Draft")
                .annotation("draft")
                .description("not published")
                .eventDate(LocalDateTime.now().plusDays(1))
                .createdOn(LocalDateTime.now())
                .location(new Location(1.0, 1.0))
                .paid(true)
                .participantLimit(0)
                .requestModeration(false)
                .state(EventState.PENDING)
                .confirmedRequests(0L)
                .views(0L)
                .initiator(initiator)
                .category(category)
                .build());
    }

    @Test
    void create_shouldCreateRequest_whenValid() {
        ParticipationRequestDto result = service.create(requester.getId(), publishedEvent.getId());

        assertNotNull(result.getId());
        assertEquals(requester.getId(), result.getRequester());
        assertEquals(publishedEvent.getId(), result.getEvent());
        assertEquals("PENDING", result.getStatus().name());
    }

    @Test
    void create_shouldFail_whenUserIsInitiator() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.create(initiator.getId(), publishedEvent.getId()));

        assertEquals("Cannot request participation in own event", ex.getMessage());
    }

    @Test
    void create_shouldFail_whenEventNotPublished() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.create(requester.getId(), unpublishedEvent.getId()));

        assertEquals("Event is not published", ex.getMessage());
    }

    @Test
    void cancel_shouldSetCanceledStatus() {
        ParticipationRequestDto created = service.create(requester.getId(), publishedEvent.getId());
        ParticipationRequestDto canceled = service.cancel(requester.getId(), created.getId());

        assertEquals("CANCELED", canceled.getStatus().name());
    }

    @Test
    void cancel_shouldFail_whenWrongUser() {
        ParticipationRequestDto created = service.create(requester.getId(), publishedEvent.getId());

        assertThrows(IllegalArgumentException.class,
                () -> service.cancel(initiator.getId(), created.getId()));
    }

    @Test
    void getUserRequests_shouldReturnAllRequestsOfUser() {
        Event secondEvent = eventRepository.save(Event.builder()
                .title("Backup Event")
                .annotation("B")
                .description("D")
                .eventDate(LocalDateTime.now().plusDays(2))
                .createdOn(LocalDateTime.now())
                .location(publishedEvent.getLocation())
                .paid(false)
                .participantLimit(100)
                .requestModeration(true)
                .state(EventState.PUBLISHED)
                .confirmedRequests(0L)
                .views(0L)
                .initiator(initiator)
                .category(publishedEvent.getCategory())
                .build());

        service.create(requester.getId(), publishedEvent.getId());
        service.create(requester.getId(), secondEvent.getId());

        List<ParticipationRequestDto> requests = service.getUserRequests(requester.getId());

        assertEquals(2, requests.size());
    }

    @Test
    void getEventRequests_shouldReturnRequestsForEvent_whenUserIsInitiator() {
        ParticipationRequestDto request = service.create(requester.getId(), publishedEvent.getId());

        List<ParticipationRequestDto> result = service.getEventRequests(initiator.getId(), publishedEvent.getId());

        assertEquals(1, result.size());
        assertEquals(request.getId(), result.get(0).getId());
    }

    @Test
    void getEventRequests_shouldFail_whenUserNotInitiator() {
        service.create(requester.getId(), publishedEvent.getId());

        assertThrows(IllegalArgumentException.class,
                () -> service.getEventRequests(requester.getId(), publishedEvent.getId()));
    }

    @Test
    void confirmRequest_shouldSetStatusConfirmed_whenValid() {
        ParticipationRequestDto request = service.create(requester.getId(), publishedEvent.getId());

        ParticipationRequestDto confirmed = service.confirmRequest(
                initiator.getId(), publishedEvent.getId(), request.getId());

        assertEquals("CONFIRMED", confirmed.getStatus().name());
    }

    @Test
    void confirmRequest_shouldFail_whenLimitReached() {
        publishedEvent.setParticipantLimit(1);
        eventRepository.save(publishedEvent);

        User anotherUser = userRepository.save(new User(null, "Bob", "bob@mail.com"));

        ParticipationRequestDto r1 = service.create(requester.getId(), publishedEvent.getId());
        ParticipationRequestDto r2 = service.create(anotherUser.getId(), publishedEvent.getId());

        service.confirmRequest(initiator.getId(), publishedEvent.getId(), r1.getId());

        assertThrows(IllegalStateException.class,
                () -> service.confirmRequest(initiator.getId(), publishedEvent.getId(), r2.getId()));
    }

    @Test
    void rejectRequest_shouldSetStatusRejected_whenValid() {
        ParticipationRequestDto request = service.create(requester.getId(), publishedEvent.getId());

        ParticipationRequestDto rejected = service.rejectRequest(
                initiator.getId(), publishedEvent.getId(), request.getId());

        assertEquals("REJECTED", rejected.getStatus().name());
    }

    @Test
    void rejectRequest_shouldFail_whenUserNotInitiator() {
        ParticipationRequestDto request = service.create(requester.getId(), publishedEvent.getId());

        assertThrows(IllegalArgumentException.class,
                () -> service.rejectRequest(requester.getId(), publishedEvent.getId(), request.getId()));
    }
}