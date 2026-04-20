package com.example.turniring.evaluation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "evaluations")
public class EvaluationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false, unique = true)
    private EvaluationAssignmentEntity assignment;

    @Column(name = "backend_score", nullable = false)
    private Integer backendScore;

    @Column(name = "database_score", nullable = false)
    private Integer databaseScore;

    @Column(name = "frontend_score", nullable = false)
    private Integer frontendScore;

    @Column(name = "must_have_score", nullable = false)
    private Integer mustHaveScore;

    @Column(name = "functionality_score", nullable = false)
    private Integer functionalityScore;

    @Column(name = "usability_score", nullable = false)
    private Integer usabilityScore;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "total_score", nullable = false)
    private Double totalScore;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;
}
