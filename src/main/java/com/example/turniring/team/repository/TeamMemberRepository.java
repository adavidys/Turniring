package com.example.turniring.team.repository;

import com.example.turniring.team.entity.TeamMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMemberEntity, Long> {
    List<TeamMemberEntity> findAllByTeamId(Long teamId);

    void deleteAllByTeamId(Long teamId);

    boolean existsByEmail(String email);

    boolean existsByTeamIdAndEmail(Long teamId, String email);

    long countByTeamId(Long teamId);
}
