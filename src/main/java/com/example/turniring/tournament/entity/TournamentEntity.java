package com.example.turniring.tournament.entity;

import com.example.turniring.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tournaments")
public class TournamentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "rules", columnDefinition = "TEXT")
    private String rules;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "registration_start_at", nullable = false)
    private LocalDateTime registrationStartAt;

    @Column(name = "registration_end_at", nullable = false)
    private LocalDateTime registrationEndAt;

    @Column(name = "max_teams")
    private Integer maxTeams;

    @Column(name = "minimum_rounds", nullable = false)
    private Integer minimumRounds;

    @Column(name = "team_min_members", nullable = false)
    private Integer teamMinMembers;

    @Column(name = "team_max_members", nullable = false)
    private Integer teamMaxMembers;

    @Column(name = "hide_teams_until_registration_ends", nullable = false)
    private boolean hideTeamsUntilRegistrationEnds;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TournamentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private UserEntity createdBy;
}
