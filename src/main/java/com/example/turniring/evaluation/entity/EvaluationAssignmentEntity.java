package com.example.turniring.evaluation.entity;

import com.example.turniring.submission.entity.SubmissionEntity;
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
        name = "evaluation_assignments",
        uniqueConstraints = @UniqueConstraint(name = "uk_submission_jury", columnNames = {"submission_id", "jury_id"})
)
public class EvaluationAssignmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private SubmissionEntity submission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jury_id", nullable = false)
    private UserEntity jury;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EvaluationAssignmentStatus status;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;
}
