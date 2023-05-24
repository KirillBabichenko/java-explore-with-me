package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.exception.StartEndRangeException;
import ru.practicum.model.EndpointHit;
import ru.practicum.model.MapperDto;
import ru.practicum.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static ru.practicum.model.MapperDto.toEndpointHit;
import static ru.practicum.model.MapperDto.toEndpointHitDto;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {
    private final StatsRepository repository;

    @Transactional
    public EndpointHitDto addStatistic(EndpointHitDto endpointHitDto) {
        EndpointHit endpointHit = toEndpointHit(endpointHitDto);
        return toEndpointHitDto(repository.save(endpointHit));
    }

    public List<ViewStatsDto> getStatistic(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        checkDate(start, end);
        if (uris == null) {
            if (unique) {
                return repository.getStatisticsWithUniqueIp(start, end).stream()
                        .map(MapperDto::toViewStatsDto)
                        .collect(Collectors.toList());
            } else {
                return repository.getAllStatistics(start, end).stream()
                        .map(MapperDto::toViewStatsDto)
                        .collect(Collectors.toList());
            }
        } else {
            if (unique) {
                return repository.getStatisticsWithUniqueIpAndUris(start, end, uris).stream()
                        .map(MapperDto::toViewStatsDto)
                        .collect(Collectors.toList());
            } else {
                return repository.getAllStatisticsWithUris(start, end, uris).stream()
                        .map(MapperDto::toViewStatsDto)
                        .collect(Collectors.toList());
            }
        }
    }

    private void checkDate(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new StartEndRangeException("Ошибка времени начала и конца диапазона");
        }
        if (startTime.isAfter(endTime)) {
            throw new StartEndRangeException("Ошибка времени начала и конца диапазона");
        }
    }
}
