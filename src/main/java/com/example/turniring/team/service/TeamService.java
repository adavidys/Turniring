package com.example.turniring.team.service;

import com.example.turniring.invite.repository.InviteRepository;
import com.example.turniring.submission.repository.SubmissionRepository;
import com.example.turniring.team.dto.TeamCreateRequest;
import com.example.turniring.team.dto.TeamMemberRequest;
import com.example.turniring.team.dto.TeamMemberResponse;
import com.example.turniring.team.dto.TeamRegistrationRequest;
import com.example.turniring.team.dto.TeamResponse;
import com.example.turniring.team.entity.TeamEntity;
import com.example.turniring.team.entity.TeamMemberEntity;
import com.example.turniring.team.repository.TeamMemberRepository;
import com.example.turniring.team.repository.TeamRepository;
import com.example.turniring.tournament.entity.TournamentEntity;
import com.example.turniring.tournament.service.TournamentService;
import com.example.turniring.user.entity.UserEntity;
import com.example.turniring.user.entity.UserRole;
import com.example.turniring.user.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final InviteRepository inviteRepository;
    private final SubmissionRepository submissionRepository;
    private final TournamentService tournamentService;
    private final CurrentUserService currentUserService;
    private final Clock clock;

    @Transactional
    public TeamResponse createTeam(TeamCreateRequest request) {
        UserEntity captain = currentUserService.requireCurrentUser();
        String teamName = sanitizeTeamName(request.name());
        if (teamRepository.existsByNameIgnoreCase(teamName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Team name already exists");
        }

        TeamEntity team = teamRepository.save(TeamEntity.builder()
                .name(teamName)
                .captain(captain)
                .city(request.city())
                .organization(request.organization())
                .contactHandle(request.contactHandle())
                .createdAt(LocalDateTime.now(clock))
                .build());

        replaceMembers(team, captain, List.of());
        return toResponse(team);
    }

    @Transactional
    public TeamResponse joinTeam(Long teamId, Long tournamentId) {
        UserEntity user = currentUserService.requireCurrentUser();
        TeamEntity team = getManageableTeamEntity(teamId, user);
        if (team.getTournament() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Team is already joined to an olympiad");
        }

        TournamentEntity tournament = tournamentService.getTournamentEntity(tournamentId);
        if (!tournamentService.isRegistrationOpen(tournament)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tournament registration is closed");
        }
        if (teamRepository.existsByTournamentIdAndCaptainId(tournamentId, team.getCaptain().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Captain has already registered a team");
        }
        if (teamRepository.existsByTournamentIdAndName(tournamentId, team.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Team name already registered in this tournament");
        }
        if (tournament.getMaxTeams() != null && teamRepository.countByTournamentId(tournamentId) >= tournament.getMaxTeams()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tournament team limit has been reached");
        }

        validateMembers(team.getCaptain(), listAdditionalMembers(team), tournament);

        team.setTournament(tournament);
        return toResponse(teamRepository.save(team));
    }

    @Transactional
    public TeamResponse leaveTournament(Long teamId) {
        UserEntity user = currentUserService.requireCurrentUser();
        TeamEntity team = getManageableTeamEntity(teamId, user);
        TournamentEntity tournament = team.getTournament();
        if (tournament == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team is not joined to any olympiad");
        }
        if (!tournamentService.isRegistrationOpen(tournament)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Team can leave olympiad only while registration is open"
            );
        }
        if (submissionRepository.existsByTeamId(team.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team with submissions cannot leave olympiad");
        }

        team.setTournament(null);
        return toResponse(teamRepository.save(team));
    }

    @Transactional
    public void deleteTeam(Long teamId) {
        UserEntity user = currentUserService.requireCurrentUser();
        TeamEntity team = getManageableTeamEntity(teamId, user);
        TournamentEntity tournament = team.getTournament();
        if (tournament != null && !tournamentService.isRegistrationOpen(tournament)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Team can be deleted only while registration is open"
            );
        }
        if (submissionRepository.existsByTeamId(team.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team with submissions cannot be deleted");
        }

        inviteRepository.deleteAllByTeamId(team.getId());
        teamMemberRepository.deleteAllByTeamId(team.getId());
        teamRepository.delete(team);
    }

    @Transactional
    public TeamResponse updateTeam(Long teamId, TeamRegistrationRequest request) {
        TeamEntity team = getTeamEntity(teamId);
        UserEntity user = currentUserService.requireCurrentUser();
        boolean adminOverride = user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.ORGANIZER;
        if (!adminOverride && !team.getCaptain().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the captain can edit this team");
        }

        TournamentEntity tournament = team.getTournament();
        if (!adminOverride && tournament != null && LocalDateTime.now(clock).isAfter(tournament.getRegistrationEndAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team editing is closed after registration");
        }

        List<TeamMemberRequest> members = sanitizeMembers(request.members());
        validateMembers(team.getCaptain(), members, tournament);
        String teamName = sanitizeTeamName(request.name());
        if (teamRepository.existsByNameIgnoreCaseAndIdNot(teamName, teamId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Team name already exists");
        }

        team.setName(teamName);
        team.setCity(request.city());
        team.setOrganization(request.organization());
        team.setContactHandle(request.contactHandle());
        teamRepository.save(team);
        replaceMembers(team, team.getCaptain(), members);
        return toResponse(team);
    }

    @Transactional(readOnly = true)
    public TeamEntity getTeamEntity(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));
    }

    @Transactional(readOnly = true)
    public TeamEntity getCaptainTeamForTournament(Long tournamentId, Long captainId) {
        return teamRepository.findByTournamentIdAndCaptainId(tournamentId, captainId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Captain is not registered in this tournament"
                ));
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> listMyTeams() {
        UserEntity user = currentUserService.requireCurrentUser();
        return teamRepository.findAllByCaptainIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isUserInAnyTeam(UserEntity user) {
        return teamRepository.existsByCaptainId(user.getId())
                || teamMemberRepository.existsByEmail(normalizeEmail(user.getEmail()));
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> listTeamsForTournament(Long tournamentId) {
        TournamentEntity tournament = tournamentService.getTournamentEntity(tournamentId);
        if (!tournamentService.areTeamsVisible(tournament)) {
            return List.of();
        }
        return teamRepository.findAllByTournamentIdOrderByCreatedAtAsc(tournamentId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> listTeamsForTournamentAdmin(Long tournamentId) {
        tournamentService.getTournamentEntity(tournamentId);
        return teamRepository.findAllByTournamentIdOrderByCreatedAtAsc(tournamentId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamEntity> listTeamEntitiesForTournament(Long tournamentId) {
        return teamRepository.findAllByTournamentIdOrderByCreatedAtAsc(tournamentId);
    }

    @Transactional(readOnly = true)
    public TeamResponse toResponse(TeamEntity team) {
        List<TeamMemberResponse> members = teamMemberRepository.findAllByTeamId(team.getId()).stream()
                .sorted(Comparator.comparing(TeamMemberEntity::isCaptain).reversed().thenComparing(TeamMemberEntity::getFullName))
                .map(TeamMemberResponse::from)
                .toList();
        return TeamResponse.from(team, members);
    }

    private TeamEntity getManageableTeamEntity(Long teamId, UserEntity user) {
        TeamEntity team = getTeamEntity(teamId);
        boolean adminOverride = user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.ORGANIZER;
        if (!adminOverride && !team.getCaptain().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the captain can manage this team");
        }
        return team;
    }

    private void replaceMembers(TeamEntity team, UserEntity captain, List<TeamMemberRequest> members) {
        teamMemberRepository.deleteAllByTeamId(team.getId());

        teamMemberRepository.save(TeamMemberEntity.builder()
                .team(team)
                .fullName(captain.getName() + " " + captain.getLastName())
                .email(normalizeEmail(captain.getEmail()))
                .captain(true)
                .build());

        for (TeamMemberRequest member : members) {
            teamMemberRepository.save(TeamMemberEntity.builder()
                    .team(team)
                    .fullName(member.fullName())
                    .email(normalizeEmail(member.email()))
                    .captain(false)
                    .build());
        }
    }

    private List<TeamMemberRequest> listAdditionalMembers(TeamEntity team) {
        return teamMemberRepository.findAllByTeamId(team.getId()).stream()
                .filter(member -> !member.isCaptain())
                .map(member -> new TeamMemberRequest(member.getFullName(), member.getEmail()))
                .collect(Collectors.toList());
    }

    private void validateMembers(UserEntity captain, List<TeamMemberRequest> members, TournamentEntity tournament) {
        int totalMembers = validateUniqueMembers(captain, members);
        if (tournament == null) {
            return;
        }

        if (totalMembers < tournament.getTeamMinMembers()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Team must have at least " + tournament.getTeamMinMembers() + " members"
            );
        }
        if (totalMembers > tournament.getTeamMaxMembers()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Team must have no more than " + tournament.getTeamMaxMembers() + " members"
            );
        }
    }

    private int validateUniqueMembers(UserEntity captain, List<TeamMemberRequest> members) {
        Set<String> uniqueEmails = new LinkedHashSet<>();
        uniqueEmails.add(normalizeEmail(captain.getEmail()));

        for (TeamMemberRequest member : members) {
            String normalizedEmail = normalizeEmail(member.email());
            if (!uniqueEmails.add(normalizedEmail)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team member emails must be unique");
            }
        }
        return uniqueEmails.size();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String sanitizeTeamName(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team name is required");
        }
        return name.trim();
    }

    private List<TeamMemberRequest> sanitizeMembers(List<TeamMemberRequest> members) {
        return members == null ? List.of() : members;
    }
}
