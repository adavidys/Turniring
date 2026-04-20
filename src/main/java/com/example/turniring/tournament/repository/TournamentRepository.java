package com.example.turniring.tournament.repository;

import com.example.turniring.tournament.entity.TournamentEntity;
import com.example.turniring.tournament.entity.TournamentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TournamentRepository extends JpaRepository<TournamentEntity, Long> {
    List<TournamentEntity> findAllByStatusOrderByStartAtAsc(TournamentStatus status);

    List<TournamentEntity> findAllByCreatedByIdOrderByStartAtDesc(Long createdById);

    boolean existsByCreatedById(Long createdById);
}
