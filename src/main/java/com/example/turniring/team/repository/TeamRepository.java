package com.example.turniring.team.repository;

import com.example.turniring.team.entity.TeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<TeamEntity, Long> {
    boolean existsByTournamentIdAndCaptainId(Long tournamentId, Long captainId);

    boolean existsByTournamentIdAndName(Long tournamentId, String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByCaptainId(Long captainId);

    long countByTournamentId(Long tournamentId);

    List<TeamEntity> findAllByTournamentIdOrderByCreatedAtAsc(Long tournamentId);

    List<TeamEntity> findAllByCaptainIdOrderByCreatedAtDesc(Long captainId);

    Optional<TeamEntity> findByTournamentIdAndCaptainId(Long tournamentId, Long captainId);
}
