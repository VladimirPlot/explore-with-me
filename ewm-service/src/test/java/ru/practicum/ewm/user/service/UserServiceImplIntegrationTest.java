package ru.practicum.ewm.user.service;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import ru.practicum.ewm.user.dto.NewUserRequest;
import ru.practicum.ewm.user.dto.UserDto;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
class UserServiceImplIntegrationTest {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;

    @Test
    void create_shouldSaveUserAndReturnDto() {
        NewUserRequest req = NewUserRequest.builder().name("Alice").email("alice@mail.com").build();
        UserDto dto = userService.create(req);

        assertNotNull(dto.getId());
        assertEquals(req.getName(), dto.getName());
        assertEquals(req.getEmail(), dto.getEmail());
        assertTrue(userRepository.findById(dto.getId()).isPresent());
    }

    @Test
    void delete_shouldRemoveUser() {
        User user = userRepository.save(User.builder().name("Bob").email("bob@mail.com").build());
        userService.delete(user.getId());

        assertFalse(userRepository.existsById(user.getId()));
    }

    @Test
    void delete_shouldNotThrow_whenUserDoesNotExist() {
        assertDoesNotThrow(() -> userService.delete(999L));
    }

    @Test
    void getAll_shouldReturnAllUsers_whenIdsNull() {
        userRepository.save(User.builder().name("A").email("a@mail.com").build());
        userRepository.save(User.builder().name("B").email("b@mail.com").build());

        List<UserDto> users = userService.getAll(null, 0, 10);

        assertEquals(2, users.size());
    }

    @Test
    void getAll_shouldReturnFilteredUsers_whenIdsProvided() {
        User user1 = userRepository.save(User.builder().name("X").email("x@mail.com").build());
        userRepository.save(User.builder().name("Y").email("y@mail.com").build());

        List<UserDto> users = userService.getAll(List.of(user1.getId()), 0, 10);

        assertEquals(1, users.size());
        assertEquals("X", users.get(0).getName());
    }

    @Test
    void getAll_shouldReturnEmptyPage_whenFromExceedsTotal() {
        userRepository.save(User.builder().name("A").email("a@mail.com").build());
        userRepository.save(User.builder().name("B").email("b@mail.com").build());
        userRepository.save(User.builder().name("C").email("c@mail.com").build());

        List<UserDto> page = userService.getAll(null, 3, 1); // page = 3

        assertTrue(page.isEmpty());
    }
}