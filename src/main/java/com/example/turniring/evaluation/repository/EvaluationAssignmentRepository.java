package com.example.turniring.evaluation.repository;

import com.example.turniring.evaluation.entity.EvaluationAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationAssignmentRepository extends JpaRepository<EvaluationAssignmentEntity, Long> {
    List<EvaluationAssignmentEntity> findAllByJuryIdOrderByAssignedAtDesc(Long juryId);

    List<EvaluationAssignmentEntity> findAllBySubmissionTaskIdOrderByAssignedAtAsc(Long taskId);

    Optional<EvaluationAssignmentEntity> findBySubmissionIdAndJuryId(Long submissionId, Long juryId);

    void deleteAllBySubmissionTaskTournamentId(Long tournamentId);
}
