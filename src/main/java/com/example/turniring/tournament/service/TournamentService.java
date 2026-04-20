package com.example.turniring.tournament.service;

import com.example.turniring.announcement.entity.AnnouncementEntity;
import com.example.turniring.announcement.repository.AnnouncementRepository;
import com.example.turniring.evaluation.repository.EvaluationAssignmentRepository;
import com.example.turniring.evaluation.repository.EvaluationRepository;
import com.example.turniring.schedule.entity.ScheduleEventEntity;
import com.example.turniring.schedule.repository.ScheduleEventRepository;
import com.example.turniring.submission.repository.SubmissionRepository;
import com.example.turniring.task.repository.TaskRepository;
import com.example.turniring.team.entity.TeamEntity;
import com.example.turniring.team.repository.TeamRepository;
import com.example.turniring.tournament.dto.*;
import com.example.turniring.tournament.entity.TournamentEntity;
import com.example.turniring.tournament.entity.TournamentStatus;
import com.example.turniring.tournament.repository.TournamentRepository;
import com.example.turniring.user.entity.UserEntity;
import com.example.turniring.user.entity.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final TeamRepository teamRepository;
    private final AnnouncementRepository announcementRepository;
    private final ScheduleEventRepository scheduleEventRepository;
    private final TaskRepository taskRepository;
    private final SubmissionRepository submissionRepository;
    private final EvaluationAssignmentRepository evaluationAssignmentRepository;
    private final EvaluationRepository evaluationRepository;
    private final Clock clock;

    @Transactional
    public TournamentResponse createTournament(TournamentRequest request, UserEntity creator) {
        validateTournamentRequest(request);
        enforceAdminSingleTournamentLimit(creator);

        TournamentEntity tournament = TournamentEntity.builder()
                .title(request.title())
                .description(request.description())
                .rules(request.rules())
                .startAt(request.startAt())
                .registrationStartAt(request.registrationStartAt())
                .registrationEndAt(request.registrationEndAt())
                .maxTeams(request.maxTeams())
                .minimumRounds(request.minimumRounds())
                .teamMinMembers(request.teamMinMembers())
                .teamMaxMembers(request.teamMaxMembers())
                .hideTeamsUntilRegistrationEnds(request.hideTeamsUntilRegistrationEnds())
                .status(TournamentStatus.DRAFT)
                .createdBy(creator)
                .build();

        return toResponse(tournamentRepository.save(tournament));
    }

    @Transactional
    public TournamentResponse updateTournament(Long tournamentId, TournamentRequest request) {
        validateTournamentRequest(request);

        TournamentEntity tournament = getTournamentEntity(tournamentId);
        tournament.setTitle(request.title());
        tournament.setDescription(request.description());
        tournament.setRules(request.rules());
        tournament.setStartAt(request.startAt());
        tournament.setRegistrationStartAt(request.registrationStartAt());
        tournament.setRegistrationEndAt(request.registrationEndAt());
        tournament.setMaxTeams(request.maxTeams());
        tournament.setMinimumRounds(request.minimumRounds());
        tournament.setTeamMinMembers(request.teamMinMembers());
        tournament.setTeamMaxMembers(request.teamMaxMembers());
        tournament.setHideTeamsUntilRegistrationEnds(request.hideTeamsUntilRegistrationEnds());

        return toResponse(tournamentRepository.save(tournament));
    }

    @Transactional
    public TournamentResponse updateStatus(Long tournamentId, TournamentStatus status) {
        TournamentEntity tournament = getTournamentEntity(tournamentId);
        tournament.setStatus(status);
        return toResponse(tournamentRepository.save(tournament));
    }

    @Transactional
    public void deleteTournament(Long tournamentId, String confirmationText) {
        TournamentEntity tournament = getTournamentEntity(tournamentId);
        String confirmation = confirmationText == null ? "" : confirmationText.trim();
        if (!tournament.getTitle().equals(confirmation)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Confirmation text must exactly match tournament title"
            );
        }

        List<TeamEntity> teams = teamRepository.findAllByTournamentIdOrderByCreatedAtAsc(tournamentId);
        teams.forEach(team -> team.setTournament(null));
        teamRepository.saveAll(teams);

        evaluationRepository.deleteAllByAssignmentSubmissionTaskTournamentId(tournamentId);
        evaluationAssignmentRepository.deleteAllBySubmissionTaskTournamentId(tournamentId);
        submissionRepository.deleteAllByTaskTournamentId(tournamentId);
        taskRepository.deleteAllByTournamentId(tournamentId);
        announcementRepository.deleteAllByTournamentId(tournamentId);
        scheduleEventRepository.deleteAllByTournamentId(tournamentId);
        tournamentRepository.delete(tournament);
    }

    @Transactional(readOnly = true)
    public TournamentEntity getTournamentEntity(Long tournamentId) {
        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tournament not found"));
    }

    @Transactional(readOnly = true)
    public TournamentResponse getTournament(Long tournamentId) {
        return toResponse(getTournamentEntity(tournamentId));
    }

    @Transactional(readOnly = true)
    public List<TournamentResponse> listTournaments(TournamentStatus status) {
        List<TournamentEntity> tournaments = status == null
                ? tournamentRepository.findAll()
                : tournamentRepository.findAllByStatusOrderByStartAtAsc(status);
        return tournaments.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TournamentResponse> listManagedTournaments(Long managerId) {
        return tournamentRepository.findAllByCreatedByIdOrderByStartAtDesc(managerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public HomeResponse buildHomeResponse() {
        return new HomeResponse(
                listTournaments(TournamentStatus.REGISTRATION).stream()
                        .filter(TournamentResponse::registrationOpen)
                        .toList(),
                listTournaments(TournamentStatus.RUNNING),
                listTournaments(TournamentStatus.FINISHED)
        );
    }

    @Transactional
    public AnnouncementResponse createAnnouncement(Long tournamentId, AnnouncementRequest request, UserEntity author) {
        TournamentEntity tournament = getTournamentEntity(tournamentId);
        AnnouncementEntity announcement = AnnouncementEntity.builder()
                .tournament(tournament)
                .author(author)
                .title(request.title())
                .content(request.content())
                .createdAt(LocalDateTime.now(clock))
                .build();

        return AnnouncementResponse.from(announcementRepository.save(announcement));
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> listAnnouncements(Long tournamentId) {
        getTournamentEntity(tournamentId);
        return announcementRepository.findAllByTournamentIdOrderByCreatedAtDesc(tournamentId).stream()
                .map(AnnouncementResponse::from)
                .toList();
    }

    @Transactional
    public ScheduleEventResponse createScheduleEvent(Long tournamentId, ScheduleEventRequest request) {
        TournamentEntity tournament = getTournamentEntity(tournamentId);
        if (request.endAt().isBefore(request.startAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Schedule event end must be after start");
        }

        ScheduleEventEntity event = ScheduleEventEntity.builder()
                .tournament(tournament)
                .title(request.title())
                .description(request.description())
                .startAt(request.startAt())
                .endAt(request.endAt())
                .link(request.link())
                .build();

        return ScheduleEventResponse.from(scheduleEventRepository.save(event));
    }

    @Transactional(readOnly = true)
    public List<ScheduleEventResponse> listScheduleEvents(Long tournamentId) {
        getTournamentEntity(tournamentId);
        return scheduleEventRepository.findAllByTournamentIdOrderByStartAtAsc(tournamentId).stream()
                .map(ScheduleEventResponse::from)
                .toList();
    }

    public boolean isRegistrationOpen(TournamentEntity tournament) {
        LocalDateTime now = LocalDateTime.now(clock);
        return tournament.getStatus() == TournamentStatus.REGISTRATION
                && !now.isBefore(tournament.getRegistrationStartAt())
                && !now.isAfter(tournament.getRegistrationEndAt());
    }

    public boolean areTeamsVisible(TournamentEntity tournament) {
        return !tournament.isHideTeamsUntilRegistrationEnds()
                || LocalDateTime.now(clock).isAfter(tournament.getRegistrationEndAt())
                || tournament.getStatus() == TournamentStatus.RUNNING
                || tournament.getStatus() == TournamentStatus.FINISHED;
    }

    public TournamentResponse toResponse(TournamentEntity tournament) {
        long registeredTeams = teamRepository.countByTournamentId(tournament.getId());
        return TournamentResponse.from(
                tournament,
                registeredTeams,
                isRegistrationOpen(tournament),
                areTeamsVisible(tournament)
        );
    }

    private void validateTournamentRequest(TournamentRequest request) {
        if (request.registrationEndAt().isBefore(request.registrationStartAt())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Registration end must be after registration start"
            );
        }
        if (request.startAt() != null && request.startAt().isBefore(request.registrationEndAt())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tournament start must be after registration ends"
            );
        }
        if (request.teamMaxMembers() < request.teamMinMembers()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Team max members must be greater than or equal to team min members"
            );
        }
    }

    private void enforceAdminSingleTournamentLimit(UserEntity creator) {
        if (creator.getRole() != UserRole.ADMIN) {
            return;
        }
        if (tournamentRepository.existsByCreatedById(creator.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Admin can create only one olympiad"
            );
        }
    }
}
