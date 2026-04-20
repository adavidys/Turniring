package com.example.turniring.team.entity;

import com.example.turniring.tournament.entity.TournamentEntity;
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
@Table(
        name = "teams",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_team_tournament_name", columnNames = {"tournament_id", "name"}),
                @UniqueConstraint(name = "uk_team_tournament_captain", columnNames = {"tournament_id", "captain_id"})
        }
)
public class TeamEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id")
    private TournamentEntity tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "captain_id", nullable = false)
    private UserEntity captain;

    @Column(name = "city")
    private String city;

    @Column(name = "organization")
    private String organization;

    @Column(name = "contact_handle")
    private String contactHandle;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
