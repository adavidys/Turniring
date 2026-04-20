package com.example.turniring.task.repository;

import com.example.turniring.task.entity.TaskEntity;
import com.example.turniring.task.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, Long> {
    List<TaskEntity> findAllByTournamentIdOrderByStartAtAsc(Long tournamentId);

    List<TaskEntity> findAllByTournamentIdAndStatusOrderByStartAtAsc(Long tournamentId, TaskStatus status);

    void deleteAllByTournamentId(Long tournamentId);
}
