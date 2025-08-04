package ru.practicum.ewm.compilation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.dto.UpdateCompilationRequest;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.model.Location;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CompilationServiceImplIntegrationTest {

    @Autowired
    private CompilationService compilationService;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    private User initiator;
    private Category category;
    private Event event1;
    private Event event2;

    @BeforeEach
    void setUp() {
        initiator = userRepository.save(User.builder()
                .name("Alice")
                .email("alice@mail.com")
                .build());

        category = categoryRepository.save(Category.builder()
                .name("Music")
                .build());

        event1 = eventRepository.save(Event.builder()
                .title("Jazz Night")
                .annotation("Evening of Jazz")
                .description("Smooth jazz music")
                .eventDate(LocalDateTime.now().plusDays(3))
                .createdOn(LocalDateTime.now())
                .location(new Location(10.0, 20.0))
                .paid(false)
                .participantLimit(100)
                .requestModeration(true)
                .state(EventState.PENDING)
                .confirmedRequests(0L)
                .views(0L)
                .initiator(initiator)
                .category(category)
                .build());

        event2 = eventRepository.save(Event.builder()
                .title("Rock Concert")
                .annotation("Rock n Roll")
                .description("Loud music event")
                .eventDate(LocalDateTime.now().plusDays(5))
                .createdOn(LocalDateTime.now())
                .location(new Location(11.0, 21.0))
                .paid(true)
                .participantLimit(200)
                .requestModeration(true)
                .state(EventState.PENDING)
                .confirmedRequests(0L)
                .views(0L)
                .initiator(initiator)
                .category(category)
                .build());
    }

    @Test
    void create_shouldReturnSavedCompilation() {
        NewCompilationDto dto = NewCompilationDto.builder()
                .title("Best Events")
                .pinned(true)
                .events(Set.of(event1.getId(), event2.getId()))
                .build();

        CompilationDto result = compilationService.create(dto);

        assertNotNull(result.getId());
        assertEquals("Best Events", result.getTitle());
        assertTrue(result.getPinned());
        assertEquals(2, result.getEvents().size());
    }

    @Test
    void update_shouldModifyCompilation() {
        CompilationDto original = compilationService.create(NewCompilationDto.builder()
                .title("Old Title")
                .pinned(false)
                .events(Set.of(event1.getId()))
                .build());

        UpdateCompilationRequest update = UpdateCompilationRequest.builder()
                .title("New Title")
                .pinned(true)
                .events(Set.of(event2.getId()))
                .build();

        CompilationDto updated = compilationService.update(original.getId(), update);

        assertEquals("New Title", updated.getTitle());
        assertTrue(updated.getPinned());
        assertEquals(1, updated.getEvents().size());
        assertTrue(updated.getEvents().stream()
                .anyMatch(e -> e.getId().equals(event2.getId())));
    }

    @Test
    void delete_shouldRemoveCompilation() {
        CompilationDto created = compilationService.create(NewCompilationDto.builder()
                .title("To Delete")
                .pinned(false)
                .events(Set.of(event1.getId()))
                .build());

        compilationService.delete(created.getId());

        Exception exception = assertThrows(RuntimeException.class, () ->
                compilationService.getById(created.getId()));
        assertTrue(exception.getMessage().contains("Compilation not found"));
    }

    @Test
    void getById_shouldReturnCorrectCompilation() {
        CompilationDto created = compilationService.create(NewCompilationDto.builder()
                .title("Single Compilation")
                .pinned(false)
                .events(Set.of(event2.getId()))
                .build());

        CompilationDto fetched = compilationService.getById(created.getId());

        assertEquals(created.getId(), fetched.getId());
        assertEquals("Single Compilation", fetched.getTitle());
        assertEquals(1, fetched.getEvents().size());
    }

    @Test
    void getAll_shouldReturnAllCompilations() {
        compilationService.create(NewCompilationDto.builder()
                .title("Comp A")
                .pinned(false)
                .events(Set.of(event1.getId()))
                .build());

        compilationService.create(NewCompilationDto.builder()
                .title("Comp B")
                .pinned(true)
                .events(Set.of(event2.getId()))
                .build());

        List<CompilationDto> all = compilationService.getAll(null, 0, 10);
        assertEquals(2, all.size());

        List<CompilationDto> pinnedOnly = compilationService.getAll(true, 0, 10);
        assertEquals(1, pinnedOnly.size());
        assertTrue(pinnedOnly.get(0).getPinned());
    }
}