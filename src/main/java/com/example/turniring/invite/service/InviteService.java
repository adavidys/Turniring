package com.example.turniring.invite.service;

import com.example.turniring.invite.dto.InviteAcceptResponse;
import com.example.turniring.invite.dto.InviteLinkResponse;
import com.example.turniring.invite.dto.InvitePublicResponse;
import com.example.turniring.invite.entity.InviteEntity;
import com.example.turniring.invite.entity.InviteType;
import com.example.turniring.invite.repository.InviteRepository;
import com.example.turniring.team.entity.TeamEntity;
import com.example.turniring.team.entity.TeamMemberEntity;
import com.example.turniring.team.repository.TeamMemberRepository;
import com.example.turniring.team.service.TeamService;
import com.example.turniring.user.entity.UserEntity;
import com.example.turniring.user.entity.UserRole;
import com.example.turniring.user.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InviteService {

    private static final String INACTIVE_LINK_MESSAGE = "This link is not active";

    private final InviteRepository inviteRepository;
    private final TeamService teamService;
    private final TeamMemberRepository teamMemberRepository;
    private final CurrentUserService currentUserService;
    private final Clock clock;

    @Value("${app.invites.expiration-hours:1}")
    private long inviteExpirationHours;

    @Transactional
    public InviteLinkResponse createJuryInvite() {
        LocalDateTime now = LocalDateTime.now(clock);
        return toLinkResponse(inviteRepository.save(InviteEntity.builder()
                .token(UUID.randomUUID())
                .type(InviteType.JURY)
                .createdBy(currentUserService.requireCurrentUser())
                .createdAt(now)
                .expiresAt(now.plusHours(inviteExpirationHours))
                .build()));
    }

    @Transactional
    public InviteLinkResponse createTeamInvite(Long teamId) {
        TeamEntity team = teamService.getTeamEntity(teamId);
        LocalDateTime now = LocalDateTime.now(clock);
        return toLinkResponse(inviteRepository.save(InviteEntity.builder()
                .token(UUID.randomUUID())
                .type(InviteType.TEAM)
                .team(team)
                .createdBy(currentUserService.requireCurrentUser())
                .createdAt(now)
                .expiresAt(now.plusHours(inviteExpirationHours))
                .build()));
    }

    @Transactional
    public InvitePublicResponse getInvite(UUID token) {
        LocalDateTime now = LocalDateTime.now(clock);
        InviteEntity invite = getActiveInvite(token, now);
        return new InvitePublicResponse(
                invite.getToken(),
                invite.getType(),
                invite.getTeam() == null ? null : invite.getTeam().getId(),
                invite.getTeam() == null ? null : invite.getTeam().getName(),
                false,
                false,
                invite.getExpiresAt()
        );
    }

    @Transactional
    public InviteAcceptResponse acceptInvite(UUID token) {
        UserEntity user = currentUserService.requireCurrentUser();
        LocalDateTime now = LocalDateTime.now(clock);
        InviteEntity invite = getActiveInvite(token, now);

        if (invite.getType() == InviteType.JURY) {
            acceptJuryInvite(user);
        } else {
            acceptTeamInvite(invite, user);
        }

        inviteRepository.delete(invite);

        return new InviteAcceptResponse(
                invite.getToken(),
                invite.getType(),
                invite.getTeam() == null ? null : invite.getTeam().getId(),
                invite.getTeam() == null ? null : invite.getTeam().getName(),
                user.getRole(),
                now
        );
    }

    private InviteEntity getActiveInvite(UUID token, LocalDateTime now) {
        InviteEntity invite = getInviteEntity(token);
        if (invite.getUsedAt() != null || now.isAfter(invite.getExpiresAt())) {
            throw inactiveLinkException();
        }
        return invite;
    }

    private InviteEntity getInviteEntity(UUID token) {
        return inviteRepository.findByToken(token)
                .orElseThrow(this::inactiveLinkException);
    }

    private ResponseStatusException inactiveLinkException() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, INACTIVE_LINK_MESSAGE);
    }

    private void acceptJuryInvite(UserEntity user) {
        if (teamService.isUserInAnyTeam(user)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User in team cannot accept jury invite");
        }
        user.setRole(UserRole.JURY);
    }

    private void acceptTeamInvite(InviteEntity invite, UserEntity user) {
        TeamEntity team = invite.getTeam();
        if (team == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team invite has no team");
        }

        String normalizedEmail = normalizeEmail(user.getEmail());
        if (teamMemberRepository.existsByTeamIdAndEmail(team.getId(), normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already in this team");
        }

        if (team.getTournament() != null) {
            Integer teamMaxMembers = team.getTournament().getTeamMaxMembers();
            if (teamMaxMembers != null && teamMemberRepository.countByTeamId(team.getId()) >= teamMaxMembers) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Team is full");
            }
        }

        teamMemberRepository.save(TeamMemberEntity.builder()
                .team(team)
                .fullName(user.getName() + " " + user.getLastName())
                .email(normalizedEmail)
                .captain(false)
                .build());

        if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.ORGANIZER) {
            user.setRole(UserRole.USER);
        }
    }

    private InviteLinkResponse toLinkResponse(InviteEntity invite) {
        return new InviteLinkResponse(
                invite.getToken(),
                invite.getType(),
                invite.getTeam() == null ? null : invite.getTeam().getId(),
                invite.getTeam() == null ? null : invite.getTeam().getName(),
                invite.getExpiresAt(),
                "/invite/" + invite.getToken()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
