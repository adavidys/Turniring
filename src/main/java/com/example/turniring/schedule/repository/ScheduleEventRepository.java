package com.example.turniring.schedule.repository;

import com.example.turniring.schedule.entity.ScheduleEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleEventRepository extends JpaRepository<ScheduleEventEntity, Long> {
    List<ScheduleEventEntity> findAllByTournamentIdOrderByStartAtAsc(Long tournamentId);

    void deleteAllByTournamentId(Long tournamentId);
}
