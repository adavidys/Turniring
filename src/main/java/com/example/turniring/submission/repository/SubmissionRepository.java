package com.example.turniring.submission.repository;

import com.example.turniring.submission.entity.SubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<SubmissionEntity, Long> {
    Optional<SubmissionEntity> findByTaskIdAndTeamId(Long taskId, Long teamId);

    List<SubmissionEntity> findAllByTaskIdOrderByUpdatedAtDesc(Long taskId);

    List<SubmissionEntity> findAllByTaskTournamentIdOrderByUpdatedAtDesc(Long tournamentId);

    List<SubmissionEntity> findAllByTeamCaptainIdOrderByUpdatedAtDesc(Long captainId);

    boolean existsByTeamId(Long teamId);

    void deleteAllByTaskTournamentId(Long tournamentId);
}
