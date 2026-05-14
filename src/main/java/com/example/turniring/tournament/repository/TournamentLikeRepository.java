package com.example.turniring.tournament.repository;

import com.example.turniring.tournament.entity.TournamentLikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TournamentLikeRepository extends JpaRepository<TournamentLikeEntity, Long> {
    long countByTournamentId(Long tournamentId);

    boolean existsByTournamentIdAndUserId(Long tournamentId, Long userId);

    Optional<TournamentLikeEntity> findByTournamentIdAndUserId(Long tournamentId, Long userId);

    void deleteAllByTournamentId(Long tournamentId);
}
