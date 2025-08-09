package ru.practicum.ewm.category.mapper;

import org.junit.jupiter.api.Test;
import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.dto.NewCategoryDto;
import ru.practicum.ewm.category.model.Category;

import static org.junit.jupiter.api.Assertions.*;

class CategoryMapperTest {

    @Test
    void toDto_shouldMapCorrectly() {
        Category category = Category.builder()
                .id(1L)
                .name("Technology")
                .build();

        CategoryDto dto = CategoryMapper.toDto(category);

        assertEquals(1L, dto.getId());
        assertEquals("Technology", dto.getName());
    }

    @Test
    void toEntity_shouldMapCorrectly() {
        NewCategoryDto dto = new NewCategoryDto("Music");

        Category category = CategoryMapper.toEntity(dto);

        assertNull(category.getId());
        assertEquals("Music", category.getName());
    }
}