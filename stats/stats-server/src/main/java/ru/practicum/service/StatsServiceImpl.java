package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.exception.StartEndRangeException;
import ru.practicum.model.EndpointHit;
import ru.practicum.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

import static ru.practicum.model.MapperEndpointHit.formatter;
import static ru.practicum.model.MapperEndpointHit.toEndpointHit;
import static ru.practicum.model.MapperEndpointHit.toEndpointHitDto;

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

    public List<ViewStatsDto> getStatistic(String start, String end, List<String> uris, Boolean unique) {
        LocalDateTime startTime = LocalDateTime.parse(start, formatter);
        LocalDateTime endTime = LocalDateTime.parse(end, formatter);
        checkDate(startTime, endTime);
        if (uris == null) {
            if (unique) {
                return repository.getStatisticsWithUniqueIp(startTime, endTime);
            } else {
                return repository.getAllStatistics(startTime, endTime);
            }
        } else {
            if (unique) {
                return repository.getStatisticsWithUniqueIpAndUris(startTime, endTime, uris);
            } else {
                return repository.getAllStatisticsWithUris(startTime, endTime, uris);
            }
        }
    }

    private void checkDate(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime.isAfter(endTime) ||
                startTime.isEqual(endTime)) {
            throw new StartEndRangeException("Ошибка времени начала и конца диапазона");
        }
    }
}
