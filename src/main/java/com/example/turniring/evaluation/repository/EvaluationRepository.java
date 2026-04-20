package com.example.turniring.evaluation.repository;

import com.example.turniring.evaluation.entity.EvaluationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationRepository extends JpaRepository<EvaluationEntity, Long> {
    Optional<EvaluationEntity> findByAssignmentId(Long assignmentId);

    List<EvaluationEntity> findAllByAssignmentSubmissionTaskId(Long taskId);

    List<EvaluationEntity> findAllByAssignmentSubmissionTaskTournamentId(Long tournamentId);

    void deleteAllByAssignmentSubmissionTaskTournamentId(Long tournamentId);
}
