package ru.practicum.ewm.category.service;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.dto.NewCategoryDto;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
class CategoryServiceImplTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void create_shouldSaveCategoryAndReturnDto() {
        NewCategoryDto dto = new NewCategoryDto("Tech");
        CategoryDto saved = categoryService.create(dto);

        assertNotNull(saved.getId());
        assertEquals(dto.getName(), saved.getName());
    }

    @Test
    void update_shouldChangeCategoryName() {
        Category saved = categoryRepository.save(new Category(null, "Old"));
        NewCategoryDto update = new NewCategoryDto("New");

        CategoryDto updated = categoryService.update(saved.getId(), update);

        assertEquals("New", updated.getName());
        assertEquals(saved.getId(), updated.getId());
    }

    @Test
    void update_shouldThrow_whenCategoryNotFound() {
        NewCategoryDto update = new NewCategoryDto("New");

        assertThrows(NoSuchElementException.class,
                () -> categoryService.update(999L, update));
    }

    @Test
    void delete_shouldRemoveCategory() {
        Category saved = categoryRepository.save(new Category(null, "ToDelete"));

        categoryService.delete(saved.getId());

        assertFalse(categoryRepository.existsById(saved.getId()));
    }

    @Test
    void findAll_shouldReturnPaginatedCategories() {
        categoryRepository.save(new Category(null, "One"));
        categoryRepository.save(new Category(null, "Two"));

        List<CategoryDto> result = categoryService.findAll(0, 10);

        assertEquals(2, result.size());
    }

    @Test
    void findById_shouldReturnCategory() {
        Category saved = categoryRepository.save(new Category(null, "FindMe"));

        CategoryDto result = categoryService.findById(saved.getId());

        assertEquals("FindMe", result.getName());
    }

    @Test
    void findById_shouldThrow_whenNotFound() {
        assertThrows(NoSuchElementException.class,
                () -> categoryService.findById(404L));
    }
}