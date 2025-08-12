package ru.practicum.ewm.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.dto.NewCategoryDto;
import ru.practicum.ewm.category.mapper.CategoryMapper;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.exceptions.NotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public CategoryDto create(NewCategoryDto dto) {
        Category entity = CategoryMapper.toEntity(dto);
        Category saved = categoryRepository.save(entity);
        return CategoryMapper.toDto(saved);
    }

    @Override
    @Transactional
    public CategoryDto update(Long id, NewCategoryDto dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        category.setName(dto.getName());
        return CategoryMapper.toDto(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        categoryRepository.deleteById(id);
    }

    @Override
    public List<CategoryDto> findAll(int from, int size) {
        PageRequest page = PageRequest.of(from / size, size);
        return categoryRepository.findAll(page)
                .getContent()
                .stream()
                .map(CategoryMapper::toDto)
                .toList();
    }

    @Override
    public CategoryDto findById(Long id) {
        return categoryRepository.findById(id)
                .map(CategoryMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Category not found"));
    }
}