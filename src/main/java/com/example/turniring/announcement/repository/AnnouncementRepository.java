package com.example.turniring.announcement.repository;

import com.example.turniring.announcement.entity.AnnouncementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<AnnouncementEntity, Long> {
    List<AnnouncementEntity> findAllByTournamentIdOrderByCreatedAtDesc(Long tournamentId);

    void deleteAllByTournamentId(Long tournamentId);
}
