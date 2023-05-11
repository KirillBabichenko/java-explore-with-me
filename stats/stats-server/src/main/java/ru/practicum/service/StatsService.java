package ru.practicum.service;

import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.util.List;

public interface StatsService {
    EndpointHitDto addStatistic(EndpointHitDto endpointHitDto);

    List<ViewStatsDto> getStatistic(String start, String end, List<String> uris, Boolean unique);

}
