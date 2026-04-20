package com.example.turniring.tournament.dto;

import com.example.turniring.announcement.entity.AnnouncementEntity;

import java.time.LocalDateTime;

public record AnnouncementResponse(
        Long id,
        Long tournamentId,
        String title,
        String content,
        String authorEmail,
        LocalDateTime createdAt
) {
    public static AnnouncementResponse from(AnnouncementEntity announcement) {
        return new AnnouncementResponse(
                announcement.getId(),
                announcement.getTournament().getId(),
                announcement.getTitle(),
                announcement.getContent(),
                announcement.getAuthor().getEmail(),
                announcement.getCreatedAt()
        );
    }
}
