package com.example.turniring.tournament.dto;

import com.example.turniring.schedule.entity.ScheduleEventEntity;

import java.time.LocalDateTime;

public record ScheduleEventResponse(
        Long id,
        Long tournamentId,
        String title,
        String description,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String link
) {
    public static ScheduleEventResponse from(ScheduleEventEntity event) {
        return new ScheduleEventResponse(
                event.getId(),
                event.getTournament().getId(),
                event.getTitle(),
                event.getDescription(),
                event.getStartAt(),
                event.getEndAt(),
                event.getLink()
        );
    }
}
