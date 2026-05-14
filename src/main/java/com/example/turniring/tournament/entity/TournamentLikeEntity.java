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
@Table(
        name = "tournament_likes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tournament_likes_tournament_user",
                columnNames = {"tournament_id", "user_id"}
        ),
        indexes = {
                @Index(name = "idx_tournament_likes_tournament", columnList = "tournament_id"),
                @Index(name = "idx_tournament_likes_user", columnList = "user_id")
        }
)
public class TournamentLikeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private TournamentEntity tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
