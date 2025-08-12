package ru.practicum.ewm.event.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import ru.practicum.client.StatsClient;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventSort;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.model.Location;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureTestDatabase
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EventServiceImplAdminSearchTest {

    @Autowired
    private EventService eventService;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ParticipationRequestRepository requestRepository;

    @MockBean
    private StatsClient statsClient; // ✅ Мокаем StatsClient

    private User user;
    private Category cat1;
    private Category cat2;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now().withNano(0);

        when(statsClient.getStats(any(), any(), any(), anyBoolean()))
                .thenReturn(List.of());

        user = userRepository.save(new User(null, "Admin", "admin@example.com"));
        cat1 = categoryRepository.save(new Category(null, "Tech"));
        cat2 = categoryRepository.save(new Category(null, "Music"));

        eventRepository.save(Event.builder()
                .title("Java Meetup")
                .annotation("Java devs")
                .description("Description here")
                .eventDate(now.plusDays(5))
                .createdOn(now)
                .confirmedRequests(0L)
                .views(0L)
                .state(EventState.PENDING)
                .paid(true)
                .participantLimit(10)
                .requestModeration(true)
                .location(new Location(1.0, 2.0))
                .initiator(user)
                .category(cat1)
                .build());

        eventRepository.save(Event.builder()
                .title("Jazz Night")
                .annotation("Jazz music")
                .description("Smooth jazz")
                .eventDate(now.plusDays(5))
                .createdOn(now)
                .confirmedRequests(0L)
                .views(0L)
                .state(EventState.PENDING)
                .paid(false)
                .participantLimit(50)
                .requestModeration(true)
                .location(new Location(1.0, 2.0))
                .initiator(user)
                .category(cat2)
                .build());

        eventRepository.save(Event.builder()
                .title("Spring Boot Conf")
                .annotation("Spring devs")
                .description("Spring all day")
                .eventDate(now.plusDays(5))
                .createdOn(now)
                .confirmedRequests(0L)
                .views(0L)
                .state(EventState.PUBLISHED)
                .paid(true)
                .participantLimit(100)
                .requestModeration(true)
                .location(new Location(1.0, 2.0))
                .initiator(user)
                .category(cat1)
                .build());
    }

    @Test
    void findAllAdmin_shouldReturnAllPendingEventsByDefault() {
        List<EventFullDto> result = eventService.findAllAdmin(
                null,
                List.of(EventState.PENDING),
                null,
                now, now.plusDays(10),
                EventSort.EVENT_DATE, 0, 10
        );

        assertEquals(2, result.size());
    }

    @Test
    void findAllAdmin_shouldFilterByCategoryOnly() {
        List<EventFullDto> result = eventService.findAllAdmin(
                null,
                null,
                List.of(cat2.getId()),
                now, now.plusDays(10),
                EventSort.EVENT_DATE, 0, 10
        );

        assertEquals(1, result.size());
        assertEquals("Jazz Night", result.get(0).getTitle());
    }

    @Test
    void findAllAdmin_shouldFilterByDateRange() {
        LocalDateTime rangeStart = now.plusDays(1);
        LocalDateTime rangeEnd = now.plusDays(2);

        List<EventFullDto> result = eventService.findAllAdmin(
                null, null, null,
                rangeStart, rangeEnd,
                EventSort.EVENT_DATE, 0, 10
        );

        assertEquals(0, result.size());
    }

    @Test
    void findAllAdmin_shouldFilterByUsers() {
        User other = userRepository.save(new User(null, "Other", "o@example.com"));
        Category c = categoryRepository.save(new Category(null, "Extra"));
        eventRepository.save(Event.builder()
                .title("Other's event")
                .annotation("a").description("d")
                .eventDate(now.plusDays(3)).createdOn(now)
                .state(EventState.PENDING).paid(false)
                .participantLimit(0).requestModeration(true)
                .location(new Location(1.0, 2.0))
                .initiator(other).category(c).views(0L).confirmedRequests(0L)
                .build());

        var res = eventService.findAllAdmin(
                List.of(user.getId()),
                null, null,
                now.minusDays(1), now.plusDays(10),
                EventSort.EVENT_DATE, 0, 10
        );

        assertEquals(3, res.size());
        assertTrue(res.stream().allMatch(e -> e.getInitiator().getId().equals(user.getId())));
    }

    @Test
    void findAllAdmin_shouldFilterByStates() {
        var onlyPublished = eventService.findAllAdmin(
                null, List.of(EventState.PUBLISHED), null,
                now.minusDays(1), now.plusDays(10),
                EventSort.EVENT_DATE, 0, 10
        );
        assertEquals(1, onlyPublished.size());
        assertEquals(EventState.PUBLISHED, onlyPublished.get(0).getState());
    }

    @Test
    void findAllAdmin_shouldReturnEmpty_whenCategoriesEmptyList() {
        var res = eventService.findAllAdmin(
                null, null, List.of(),
                now.minusDays(1), now.plusDays(10),
                EventSort.EVENT_DATE, 0, 10
        );
        assertEquals(0, res.size());
    }

    @Test
    void findAllAdmin_shouldIncludeBoundaryDates() {
        var res = eventService.findAllAdmin(
                null, null, null,
                now.plusDays(5), now.plusDays(5),
                EventSort.EVENT_DATE, 0, 10
        );

        assertEquals(3, res.size());
    }

    @Test
    void findAllAdmin_shouldSortByEventDateAsc() {
        eventRepository.save(Event.builder()
                .title("Earliest")
                .annotation("a").description("d")
                .eventDate(now.plusDays(1)).createdOn(now)
                .state(EventState.PENDING).paid(false)
                .participantLimit(0).requestModeration(true)
                .location(new Location(1.0, 2.0))
                .initiator(user).category(cat1).views(0L).confirmedRequests(0L)
                .build());

        var res = eventService.findAllAdmin(
                null, null, null,
                now.minusDays(1), now.plusDays(10),
                EventSort.EVENT_DATE, 0, 10
        );

        assertFalse(res.isEmpty());
        assertEquals("Earliest", res.get(0).getTitle());
    }

    @Test
    void findAllAdmin_shouldSortByViewsDesc() {

        var events = eventRepository.findAll();

        for (int i = 0; i < events.size(); i++) {
            String uri = "/events/" + events.get(i).getId();
            long hits = (i == 0) ? 5L : 1L;
            when(statsClient.getStats(any(), any(), eq(List.of(uri)), anyBoolean()))
                    .thenReturn(List.of(new ru.practicum.dto.ViewStatsDto("ewm-service", uri, hits)));
        }

        var res = eventService.findAllAdmin(
                null, null, null,
                now.minusDays(1), now.plusDays(10),
                EventSort.VIEWS, 0, 10
        );

        assertFalse(res.isEmpty());
        assertEquals(5L, res.get(0).getViews());
    }

    @Test
    void findAllAdmin_shouldCallStatsPerEvent() {

        reset(statsClient);
        when(statsClient.getStats(any(), any(), any(), anyBoolean())).thenReturn(List.of());

        var res = eventService.findAllAdmin(
                null, null, null,
                now.minusDays(1), now.plusDays(10),
                EventSort.EVENT_DATE, 0, 10
        );

        verify(statsClient, times(res.size()))
                .getStats(any(), any(), any(), anyBoolean());
    }

    @Test
    void findAllAdmin_shouldRefreshConfirmedRequests() {

        Event published = eventRepository.findAll().stream()
                .filter(e -> e.getState() == EventState.PUBLISHED)
                .findFirst().orElseThrow();


        requestRepository.save(ru.practicum.ewm.request.model.ParticipationRequest.builder()
                .event(published)
                .requester(user)
                .created(now)
                .status(ru.practicum.ewm.request.model.RequestStatus.CONFIRMED)
                .build());

        when(statsClient.getStats(any(), any(), any(), anyBoolean()))
                .thenReturn(List.of());

        var res = eventService.findAllAdmin(
                null, List.of(EventState.PUBLISHED), null,
                now.minusDays(1), now.plusDays(10),
                EventSort.EVENT_DATE, 0, 10
        );

        assertEquals(1, res.size());
        assertEquals(1L, res.get(0).getConfirmedRequests());
    }
}