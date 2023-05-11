package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.service.StatsService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class StatsController {
    private final StatsService statsService;

    @PostMapping("/hit")
    public EndpointHitDto addStatistic(@RequestBody EndpointHitDto endpointHitDto) {
        log.info("Add endpointHitDto {}", endpointHitDto);
        return statsService.addStatistic(endpointHitDto);
    }

    @GetMapping("/stats")
    public List<ViewStatsDto> getStatistic(@RequestParam(name = "start") String start,
                                           @RequestParam(name = "end") String end,
                                           @RequestParam(name = "uris", required = false) List<String> uris,
                                           @RequestParam(name = "unique", defaultValue = "false") Boolean unique) {
        log.info("Get statistic from start {}, end {}, uris {}, unique {}", start, end, uris, unique);
        return statsService.getStatistic(start, end, uris, unique);
    }

}
