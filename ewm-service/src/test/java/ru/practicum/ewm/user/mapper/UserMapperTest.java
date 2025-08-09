package ru.practicum.ewm.user.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.user.dto.NewUserRequest;
import ru.practicum.ewm.user.dto.UserDto;
import ru.practicum.ewm.user.model.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
class UserMapperTest {

    @Test
    void toDto_shouldMapCorrectly() {
        User user = User.builder()
                .id(1L)
                .name("Test")
                .email("test@mail.com")
                .build();
        UserDto dto = UserMapper.toDto(user);

        assertEquals(user.getId(), dto.getId());
        assertEquals(user.getName(), dto.getName());
        assertEquals(user.getEmail(), dto.getEmail());
    }

    @Test
    void toEntity_shouldMapCorrectly() {
        NewUserRequest req = NewUserRequest.builder()
                .name("Test")
                .email("test@mail.com")
                .build();
        User user = UserMapper.toEntity(req);

        assertNull(user.getId());
        assertEquals(req.getName(), user.getName());
        assertEquals(req.getEmail(), user.getEmail());
    }
}