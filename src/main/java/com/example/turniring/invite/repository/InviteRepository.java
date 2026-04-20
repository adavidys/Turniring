package com.example.turniring.invite.repository;

import com.example.turniring.invite.entity.InviteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InviteRepository extends JpaRepository<InviteEntity, Long> {
    Optional<InviteEntity> findByToken(UUID token);

    void deleteAllByTeamId(Long teamId);
}
