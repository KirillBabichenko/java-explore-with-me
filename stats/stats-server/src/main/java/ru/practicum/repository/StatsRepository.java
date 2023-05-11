package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.model.EndpointHit;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsRepository extends JpaRepository<EndpointHit, Long> {

    @Query(" select new ru.practicum.dto.ViewStatsDto(app, uri, count(distinct ip) as hits) from EndpointHit " +
            "where timestamp between ?1 and ?2 and uri in(?3) " +
            "group by app, uri order by hits desc")
    List<ViewStatsDto> getStatisticsWithUniqueIpAndUris(LocalDateTime start, LocalDateTime end, List<String> uri);

    @Query(" select new ru.practicum.dto.ViewStatsDto(app, uri, count(ip) as hits) from EndpointHit " +
            "where timestamp between ?1 and ?2 and uri in(?3) " +
            "group by app, uri order by hits desc")
    List<ViewStatsDto> getAllStatisticsWithUris(LocalDateTime start, LocalDateTime end, List<String> uri);

    @Query(" select new ru.practicum.dto.ViewStatsDto(app, uri, count(distinct ip) as hits) from EndpointHit " +
            "where timestamp between ?1 and ?2 " +
            "group by app, uri order by hits desc")
    List<ViewStatsDto> getStatisticsWithUniqueIp(LocalDateTime start, LocalDateTime end);

    @Query(" select new ru.practicum.dto.ViewStatsDto(app, uri, count(ip) as hits) from EndpointHit " +
            "where timestamp between ?1 and ?2 " +
            "group by app, uri order by hits desc")
    List<ViewStatsDto> getAllStatistics(LocalDateTime start, LocalDateTime end);

}
