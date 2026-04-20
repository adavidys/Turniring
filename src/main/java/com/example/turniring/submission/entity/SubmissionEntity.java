package com.example.turniring.submission.entity;

import com.example.turniring.task.entity.TaskEntity;
import com.example.turniring.team.entity.TeamEntity;
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
        name = "submissions",
        uniqueConstraints = @UniqueConstraint(name = "uk_submission_task_team", columnNames = {"task_id", "team_id"})
)
public class SubmissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private TaskEntity task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private TeamEntity team;

    @Column(name = "github_url", nullable = false)
    private String githubUrl;

    @Column(name = "demo_video_url", nullable = false)
    private String demoVideoUrl;

    @Column(name = "live_demo_url")
    private String liveDemoUrl;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SubmissionStatus status;
}
