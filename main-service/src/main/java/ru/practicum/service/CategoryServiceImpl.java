package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.NewCategoryDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.IdNotFoundException;
import ru.practicum.mapper.MapperCategory;
import ru.practicum.model.Category;
import ru.practicum.model.Event;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.repository.EventsRepository;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static ru.practicum.mapper.MapperCategory.toCategory;
import static ru.practicum.mapper.MapperCategory.toCategoryDto;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final EventsRepository eventsRepository;

    /**
     * Добавление новой категории
     */
    @Transactional
    public NewCategoryDto createCategory(NewCategoryDto newCategoryDto) {
        Category category = toCategory(newCategoryDto);
        return toCategoryDto(categoryRepository.save(category));
    }

    /**
     * Удаление категории
     */
    @Transactional
    public void deleteCategory(Long id) {
        Category category = checkCategory(id);
        List<Event> events = eventsRepository.findByCategory(category);
        if (!events.isEmpty()) {
            throw new ConflictException("Нельзя удалить категорию. Существуют события, связанные с категорией.");
        }
        categoryRepository.deleteById(id);

    }

    /**
     * Изменение категории
     */
    @Transactional
    public NewCategoryDto updateCategory(Long id, NewCategoryDto newCategoryDto) {
        Category category = checkCategory(id);
        ofNullable(newCategoryDto.getName()).ifPresent(category::setName);
        return toCategoryDto(categoryRepository.save(category));
    }

    /**
     * Получение категорий
     */
    public List<NewCategoryDto> getCategories(Integer from, Integer size) {
        PageRequest page = PageRequest.of(from, size);
        return categoryRepository.findAll(page).stream()
                .map(MapperCategory::toCategoryDto)
                .collect(Collectors.toList());
    }

    /**
     * Получение информации о категории по ее идентификатору
     */
    public NewCategoryDto getCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IdNotFoundException("Пользователь с таким id  не найден"));
        return toCategoryDto(category);
    }

    private Category checkCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IdNotFoundException("Категории с таким id не найдено"));
    }

}
