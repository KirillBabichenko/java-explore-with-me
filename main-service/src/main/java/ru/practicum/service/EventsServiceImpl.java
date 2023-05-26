package ru.practicum.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventsShortDto;
import ru.practicum.dto.event.NewEventDto;
import ru.practicum.dto.event.UpdateEvent;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.IdNotFoundException;
import ru.practicum.exception.IncorrectStateException;
import ru.practicum.mapper.MapperEvent;
import ru.practicum.mapper.MapperRequest;
import ru.practicum.model.Category;
import ru.practicum.model.Event;
import ru.practicum.model.Location;
import ru.practicum.model.Request;
import ru.practicum.model.Sort;
import ru.practicum.model.State;
import ru.practicum.model.StateAction;
import ru.practicum.model.User;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.repository.EventsRepository;
import ru.practicum.repository.LocationRepository;
import ru.practicum.repository.RequestRepository;
import ru.practicum.repository.UserRepository;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static ru.practicum.mapper.MapperEvent.toEvent;
import static ru.practicum.mapper.MapperEvent.toEventFullDto;
import static ru.practicum.mapper.MapperRequest.toRequestDto;
import static ru.practicum.utility.UtilityClass.formatter;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@ComponentScan(basePackages = {"ru.practicum.client"})
public class EventsServiceImpl implements EventsService {
    private final EventsRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final RequestRepository requestRepository;
    private final StatsClient statsClient;

    /**
     * Добавление нового события
     */
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto dto) {
        if (dto.getPaid() == null) {
            dto.setPaid(false);
        }
        if (dto.getParticipantLimit() == null) {
            dto.setParticipantLimit(0L);
        }
        if (dto.getRequestModeration() == null) {
            dto.setRequestModeration(true);
        }
        LocalDateTime nowDateTime = LocalDateTime.now();
        checkDateTimeForDto(nowDateTime, dto.getEventDate());
        Category category = categoryRepository.findById(dto.getCategory())
                .orElseThrow(() -> new IdNotFoundException("Категория с таким id  не найдена"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IdNotFoundException("Пользователь с таким id  не найден"));
        locationRepository.save(dto.getLocation());
        Event event = toEvent(dto, category, user, nowDateTime);
        return toEventFullDto(eventRepository.save(event));
    }

    /**
     * Получение событий добавленных текущим пользователем
     */
    public List<EventsShortDto> getEventsFromUser(Long userId, Integer from, Integer size) {
        PageRequest page = PageRequest.of(from, size);
        User user = checkUser(userId);
        List<Event> events = eventRepository.findDByInitiator(user, page);
        Map<Long, Long> hits = getStatisticFromListEvents(events);
        events.forEach(event -> event.setViews(hits.get(event.getId())));
        return events.stream()
                .map(MapperEvent::toEventShortDto)
                .collect(Collectors.toList());
    }

    /**
     * Получение полной информации о событии добавленном текущим пользователем
     */
    public EventFullDto getEventWithOwner(Long userId, Long eventId) {
        checkUser(userId);
        Event event = findEventById(eventId);
        Map<Long, Long> hits = getStatisticFromListEvents(List.of(event));
        event.setViews(hits.get(event.getId()));
        return toEventFullDto(event);
    }

    /**
     * Изменение события добавленном текущим пользователем
     */
    @Transactional
    public EventFullDto updateEvent(Long userId, Long eventId, UpdateEvent dto) {
        Event event = findEventById(eventId);
        checkUser(userId);
        if (dto.getEventDate() != null) {
            checkDateTimeForDto(LocalDateTime.now(), dto.getEventDate());
        }
        if (!(event.getState().equals(State.CANCELED) || event.getState().equals(State.PENDING))) {
            throw new IncorrectStateException("Некорректный статус. Изменить можно только отмененные " +
                    "события или события в состоянии ожидания модерации.");
        }
        if (dto.getStateAction() != null) {
            switch (dto.getStateAction()) {
                case SEND_TO_REVIEW:
                    event.setState(State.PENDING);
                    break;
                case CANCEL_REVIEW:
                    event.setState(State.CANCELED);
                    break;
                default:
                    throw new IncorrectStateException("Некорректный статус dto.");
            }
        }
        Event updatedEvent = updateEventFields(event, dto);
        Event updatedEventFromDB = eventRepository.save(updatedEvent);
        Map<Long, Long> hits = getStatisticFromListEvents(List.of(updatedEventFromDB));
        event.setViews(hits.get(event.getId()));
        return toEventFullDto(updatedEventFromDB);
    }

    /**
     * Поиск событий с фильтрами для администратора
     */
    public List<EventFullDto> getEventsForAdmin(List<Long> users, List<String> states, List<Long> categories,
                                                LocalDateTime rangeStart, LocalDateTime rangeEnd, Integer from, Integer size) {
        PageRequest page = PageRequest.of(from, size);
        List<State> stateList = null;
        LocalDateTime start = null;
        LocalDateTime end = null;
        if (states != null) {
            stateList = states.stream()
                    .map(State::valueOf)
                    .collect(Collectors.toList());
        }
        if (rangeStart != null) {
            start = rangeStart;
        }
        if (rangeEnd != null) {
            end = rangeEnd;
        }
        List<Event> events = eventRepository.getEventsWithUsersStatesCategoriesDateTime(
                users, stateList, categories, start, end, page);
        Map<Long, Long> hits = getStatisticFromListEvents(events);
        events.forEach(event -> event.setViews(hits.get(event.getId())));
        return events.stream()
                .map(MapperEvent::toEventFullDto)
                .collect(Collectors.toList());
    }

    /**
     * Редактирование данных события и его статуса (отклонение/публикация) администратором
     */
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEvent dto) {
        Event event = findEventById(eventId);
        if (dto.getEventDate() != null) {
            if (LocalDateTime.now().plusHours(1).isAfter(dto.getEventDate())) {
                throw new BadRequestException("Ошибка. Дата и время на которые намечено событие " +
                        "не может быть раньше, чем через час от текущего момента");
            }
        } else {
            if (dto.getStateAction() != null) {
                if (dto.getStateAction().equals(StateAction.PUBLISH_EVENT) &&
                        LocalDateTime.now().plusHours(1).isAfter(event.getEventDate())) {
                    throw new IncorrectStateException("Ошибка. Дата и время публикуемого события " +
                            "не может быть раньше, чем через час от текущего момента");
                }
                if (dto.getStateAction().equals(StateAction.PUBLISH_EVENT) && !(event.getState().equals(State.PENDING))) {
                    throw new IncorrectStateException("Некорректный статус. Событие можно публиковать, " +
                            "только если оно в состоянии ожидания публикации.");
                }
                if (dto.getStateAction().equals(StateAction.REJECT_EVENT) && event.getState().equals(State.PUBLISHED)) {
                    throw new IncorrectStateException("Некорректный статус. Событие можно отклонить, " +
                            "только если оно еще не опубликовано.");
                }
            }
        }
        if (dto.getStateAction() != null) {
            switch (dto.getStateAction()) {
                case REJECT_EVENT:
                    event.setState(State.CANCELED);
                    break;
                case PUBLISH_EVENT:
                    event.setState(State.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;
                default:
                    throw new IncorrectStateException("Некорректный статус dto.");
            }
        }
        Event updatedEvent = updateEventFields(event, dto);
        Event updatedEventFromDB = eventRepository.save(updatedEvent);
        Map<Long, Long> hits = getStatisticFromListEvents(List.of(updatedEventFromDB));
        updatedEventFromDB.setViews(hits.get(event.getId()));
        return toEventFullDto(updatedEventFromDB);
    }

    /**
     * Получение событий с возможностью фильтрации (публичный доступ)
     */
    @Transactional
    public List<EventsShortDto> getEventsWithFilters(String text, List<Long> categories, Boolean paid, LocalDateTime rangeStart,
                                                     LocalDateTime rangeEnd, Boolean onlyAvailable, String sort, Integer from,
                                                     Integer size, HttpServletRequest request) {
        PageRequest page = PageRequest.of(from, size);
        List<Event> events = new ArrayList<>();
        checkDateTime(rangeStart, rangeEnd);
        if (onlyAvailable) {
            if (sort == null) {
                events = eventRepository.getAvailableEventsWithFiltersDateSorted(
                        text, State.PUBLISHED, categories, paid, rangeStart, rangeEnd, page);
            } else {
                switch (Sort.valueOf(sort)) {
                    case EVENT_DATE:
                        events = eventRepository.getAvailableEventsWithFiltersDateSorted(
                                text, State.PUBLISHED, categories, paid, rangeStart, rangeEnd, page);
                        addStatistic(request);
                        return events.stream()
                                .map(MapperEvent::toEventShortDto)
                                .collect(Collectors.toList());
                    case VIEWS:
                        events = eventRepository.getAvailableEventsWithFilters(
                                text, State.PUBLISHED, categories, paid, rangeStart, rangeEnd, page);
                        Map<Long, Long> hits = getStatisticFromListEvents(events);
                        events.forEach(event -> event.setViews(hits.get(event.getId())));
                        addStatistic(request);
                        return events.stream()
                                .map(MapperEvent::toEventShortDto)
                                .sorted(Comparator.comparing(EventsShortDto::getViews))
                                .collect(Collectors.toList());
                }
            }
        } else {
            if (sort == null) {
                events = eventRepository.getAllEventsWithFiltersDateSorted(
                        text, State.PUBLISHED, categories, paid, rangeStart, rangeEnd, page);
            } else {
                switch (Sort.valueOf(sort)) {
                    case EVENT_DATE:
                        events = eventRepository.getAllEventsWithFiltersDateSorted(
                                text, State.PUBLISHED, categories, paid, rangeStart, rangeEnd, page);
                        addStatistic(request);
                        return events.stream()
                                .map(MapperEvent::toEventShortDto)
                                .collect(Collectors.toList());
                    case VIEWS:
                        events = eventRepository.getAllEventsWithFilters(
                                text, State.PUBLISHED, categories, paid, rangeStart, rangeEnd, page);
                        Map<Long, Long> hits = getStatisticFromListEvents(events);
                        events.forEach(event -> event.setViews(hits.get(event.getId())));
                        addStatistic(request);
                        return events.stream()
                                .map(MapperEvent::toEventShortDto)
                                .sorted(Comparator.comparing(EventsShortDto::getViews))
                                .collect(Collectors.toList());
                }
            }
        }
        addStatistic(request);
        return events.stream()
                .map(MapperEvent::toEventShortDto)
                .collect(Collectors.toList());
    }

    /**
     * Получение полной информации об опубликованном событии по его идентификатору
     */
    @Transactional
    public EventFullDto getEventWithFullInfoById(Long id, HttpServletRequest request) {
        Event event = findEventById(id);
        if (!event.getState().equals(State.PUBLISHED)) {
            throw new IdNotFoundException("Событие еще не опубликовано");
        }
        eventRepository.save(event);
        addStatistic(request);
        Map<Long, Long> hits = getStatisticFromListEvents(List.of(event));
        event.setViews(hits.get(event.getId()));
        return toEventFullDto(event);
    }

    /**
     * Получение информации о запросах на участие в событии текущего пользователя
     */
    public List<ParticipationRequestDto> getRequestsForUserForThisEvent(Long userId, Long eventId) {
        checkUser(userId);
        findEventById(eventId);
        List<Request> requests = requestRepository.findByEventId(eventId);
        return requests.stream()
                .map(MapperRequest::toRequestDto)
                .collect(Collectors.toList());
    }

    /**
     * Изменение статуса (подтверждение/отмена) заявок на участие в событии текущего пользователя
     */
    @Transactional
    public EventRequestStatusUpdateResult changeRequestsStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest dto) {
        List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();
        List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();
        checkUser(userId);
        Event event = findEventById(eventId);
        if (!event.getRequestModeration() || event.getParticipantLimit().equals(0L)) {
            throw new ConflictException("Подтверждение заявок не требуется");
        }
        long limitBalance = event.getParticipantLimit() - event.getConfirmedRequests();
        if (event.getParticipantLimit() != 0 && limitBalance <= 0) {
            throw new ConflictException("У события достигнут лимит запросов на участие.");
        }
        if (dto.getStatus().equals(State.REJECTED.toString())) {
            for (Long requestId : dto.getRequestIds()) {
                Request request = requestRepository.findById(requestId)
                        .orElseThrow(() -> new IdNotFoundException("Запроса c id = " + requestId + " не найдено."));
                if (request.getStatus().equals(State.PENDING)) {
                    request.setStatus(State.REJECTED);
                    requestRepository.save(request);
                    rejectedRequests.add(toRequestDto(request));
                }
            }
        }
        for (int i = 0; i < dto.getRequestIds().size(); i++) {
            if (limitBalance != 0) {
                int finalI1 = i;
                Request request = requestRepository.findById(dto.getRequestIds().get(i))
                        .orElseThrow(() -> new IdNotFoundException("Запроса c id = " + finalI1 + " не найдено."));
                if (request.getStatus().equals(State.PENDING)) {
                    request.setStatus(State.CONFIRMED);
                    event.setConfirmedRequests(event.getConfirmedRequests() + 1);
                    eventRepository.save(event);
                    requestRepository.save(request);
                    confirmedRequests.add(toRequestDto(request));
                    limitBalance--;
                }
            } else {
                int finalI = i;
                Request request = requestRepository.findById(dto.getRequestIds().get(i))
                        .orElseThrow(() -> new IdNotFoundException("Запроса c id = " + finalI + " не найдено."));
                if (request.getStatus().equals(State.PENDING)) {
                    request.setStatus(State.REJECTED);
                    requestRepository.save(request);
                    rejectedRequests.add(toRequestDto(request));
                }
            }
        }
        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmedRequests)
                .rejectedRequests(rejectedRequests)
                .build();
    }

    /**
     * Добавление информации о просмотре события в сервис статистики
     */
    private void addStatistic(HttpServletRequest request) {
        String app = "main-service";
        statsClient.addStatistic(EndpointHitDto.builder()
                .app(app)
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(LocalDateTime.now().format(formatter))
                .build());
    }

    /**
     * Получение информации о просмотрах из сервиса статистики
     */
    private Map<Long, Long> getStatisticFromListEvents(List<Event> events) {
        List<Long> idEvents = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());
        String start = LocalDateTime.now().minusYears(100).format(formatter);
        String end = LocalDateTime.now().format(formatter);
        String eventsUri = "/events/";
        List<String> uris = idEvents.stream().map(id -> eventsUri + id).collect(Collectors.toList());
        ResponseEntity<Object> response = statsClient.getStatistic(start, end, uris, true);
        ObjectMapper objectMapper = new ObjectMapper();
        List<ViewStatsDto> viewStatsDto = objectMapper.convertValue(response.getBody(), new TypeReference<>() {
        });
        Map<Long, Long> hits = new HashMap<>();
        for (ViewStatsDto statsDto : viewStatsDto) {
            String uri = statsDto.getUri();
            hits.put(Long.parseLong(uri.substring(eventsUri.length())), statsDto.getHits());
        }
        return hits;
    }

    private void checkDateTime(LocalDateTime start, LocalDateTime end) {
        if (start == null) {
            start = LocalDateTime.now().minusYears(100);
        }
        if (end == null) {
            end = LocalDateTime.now();
        }
        if (start.isAfter(end)) {
            throw new BadRequestException("Некорректный запрос. Дата окончания события задана позже даты стартаю");
        }
    }

    private Event updateEventFields(Event event, UpdateEvent dto) {
        ofNullable(dto.getAnnotation()).ifPresent(event::setAnnotation);
        ofNullable(dto.getCategory()).ifPresent(category -> event.setCategory(categoryRepository.findById(category)
                .orElseThrow(() -> new IdNotFoundException("Категория с таким id  не найдена"))));
        ofNullable(dto.getDescription()).ifPresent(event::setDescription);
        ofNullable(dto.getEventDate()).ifPresent(
                event::setEventDate);
        if (dto.getLocation() != null) {
            List<Location> location = locationRepository.findByLatAndLon(dto.getLocation().getLat(), dto.getLocation().getLon());
            if (location.isEmpty()) {
                locationRepository.save(dto.getLocation());
            }
            event.setLocation(dto.getLocation());
        }
        ofNullable(dto.getPaid()).ifPresent(event::setPaid);
        ofNullable(dto.getParticipantLimit()).ifPresent(event::setParticipantLimit);
        ofNullable(dto.getRequestModeration()).ifPresent(event::setRequestModeration);
        ofNullable(dto.getTitle()).ifPresent(event::setTitle);
        return event;
    }

    private void checkDateTimeForDto(LocalDateTime nowDateTime, LocalDateTime dtoDateTime) {
        if (nowDateTime.plusHours(2).isAfter(dtoDateTime)) {
            throw new BadRequestException("Ошибка. Дата и время на которые намечено событие " +
                    "не может быть раньше, чем через два часа от текущего момента");
        }
    }

    private User checkUser(Long idUser) {
        return userRepository.findById(idUser)
                .orElseThrow(() -> new IdNotFoundException("Пользователь с таким id  не найден"));
    }

    private Event findEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new IdNotFoundException("Событие с таким id не найдено"));
    }

}
