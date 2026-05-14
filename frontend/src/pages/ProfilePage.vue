<template>
  <div class="page-grid">
    <section class="hero-card stack" v-if="profile">
      <span class="eyebrow">{{ tx("Профіль", "Profile") }}</span>
      <div class="toolbar">
        <div class="stack-sm">
          <h1 class="title-lg">{{ profile.name }} {{ profile.lastName }}</h1>
          <p class="text-soft">{{ profile.email }}</p>
        </div>
        <div class="btn-row">
          <StatusBadge :status="profile.role" />
          <RouterLink class="btn-tonal" to="/profile/data">
            {{ t("profileData.openEdit") }}
          </RouterLink>
          <button class="btn-ghost" type="button" @click="handleLogout">{{ t("nav.logout") }}</button>
        </div>
      </div>
    </section>

    <div v-if="loading" class="panel stack-sm">
      <div>{{ tx("Завантаження профілю…", "Loading profile…") }}</div>
      <div class="btn-row">
        <button class="btn-ghost" type="button" @click="handleLogout">{{ t("nav.logout") }}</button>
      </div>
    </div>
    <div v-else-if="errorMessage" class="stack">
      <div class="error-box">{{ errorMessage }}</div>
      <div class="btn-row">
        <button class="btn-ghost" type="button" @click="handleLogout">{{ t("nav.logout") }}</button>
      </div>
    </div>
    <template v-else-if="profile">
      <SectionBlock
        :title="tx('Керування роллю', 'Role management')"
        :description="tx('Змінюйте роль свого акаунта через це меню. Зміна недоступна, якщо ви в команді або маєте незавершені олімпіади.', 'Change your account role from this panel. Role change is disabled while you are in a team or have unfinished olympiads.')"
        :eyebrow="tx('Роль', 'Role')"
      >
        <div class="stack">
          <div v-if="profile.inTeam" class="error-box">
            {{ tx("Ви є учасником команди. Щоб змінити роль, спочатку вийдіть із командного складу.", "You are a team member. Leave the team roster first to change role.") }}
          </div>
          <div v-if="hasUnfinishedManagedTournaments" class="error-box">
            {{ tx("У вас є незавершені олімпіади. Завершіть їх перед зміною ролі.", "You have unfinished olympiads. Finish them before changing role.") }}
          </div>
          <div class="field stack-sm">
            <label>{{ tx("Нова роль", "New role") }}</label>
            <div class="grid-auto">
              <button
                v-for="roleOption in roleOptions"
                :key="roleOption.value"
                class="btn-ghost profile-role-option"
                :class="{ 'is-active': selectedRole === roleOption.value }"
                type="button"
                :aria-pressed="selectedRole === roleOption.value"
                :disabled="roleChangeBlocked"
                @click="selectedRole = roleOption.value"
              >
                <strong>{{ roleOption.label }}</strong>
                <span class="text-soft">{{ roleOption.description }}</span>
              </button>
            </div>
          </div>

          <div class="panel stack-sm">
            <h3 class="title-sm">{{ tx("Команда та підключення до турніру", "Team and tournament access") }}</h3>
            <p class="text-soft">
              {{ tx("Щоб працювати як команда, використовуйте сторінки команд: створення, приєднання та робочий простір.", "To work as a team, use team pages: create, join, and workspace.") }}
            </p>
            <div class="btn-row">
              <RouterLink class="btn-tonal" to="/team">{{ t("nav.team") }}</RouterLink>
              <RouterLink class="btn-ghost" to="/teams/create">{{ tx("Створити", "Create") }}</RouterLink>
              <RouterLink class="btn-ghost" to="/teams/join">{{ tx("Переглянути команди", "Browse teams") }}</RouterLink>
            </div>
          </div>
        </div>
        <div class="btn-row">
          <button
            class="btn"
            type="button"
            :disabled="roleChangeBlocked || changingRole || selectedRole === profile.role"
            @click="handleRoleChange"
          >
            {{ changingRole ? tx("Зміна ролі…", "Changing role…") : tx("Змінити роль", "Change role") }}
          </button>
        </div>
        <div v-if="roleErrorMessage" class="error-box">{{ roleErrorMessage }}</div>
      </SectionBlock>

      <SectionBlock :title="tx('Підсумок акаунта', 'Account summary')" :description="tx('Поточна роль і доступ до робочих просторів.', 'Current role and workspace access.')" :eyebrow="tx('Доступ', 'Access')">
        <div class="stat-row">
          <span class="stat-chip">{{ tx("Роль", "Role") }}: {{ formatRole(profile.role) }}</span>
          <span class="stat-chip">{{ tx("Команд", "Teams") }}: {{ profile.teams.length }}</span>
          <span class="stat-chip">{{ tx("Керованих турнірів", "Managed tournaments") }}: {{ profile.managedTournaments.length }}</span>
          <span class="stat-chip">{{ tx("Призначень журі", "Jury assignments") }}: {{ profile.juryAssignments.length }}</span>
        </div>
      </SectionBlock>

      <section class="split" v-if="profile.teams.length || profile.managedTournaments.length">
        <SectionBlock :title="tx('Історія команд', 'Team history')" :description="tx('Склади, якими ви керували в турнірах.', 'Rosters you managed in tournaments.')" :eyebrow="t('nav.teams')">
          <div v-if="profile.teams.length" class="stack">
            <article v-for="team in profile.teams" :key="team.id" class="panel stack-sm">
              <div class="toolbar">
                <div>
                  <h3 class="title-sm">{{ team.name }}</h3>
                  <p class="text-soft">{{ team.tournamentId ? `${tx("Турнір", "Tournament")} #${team.tournamentId}` : tx("Без олімпіади", "Without olympiad") }}</p>
                </div>
                <RouterLink class="btn-tonal" to="/team">{{ t("home.openWorkspace") }}</RouterLink>
              </div>
              <div class="member-list">
                <div v-for="member in team.members" :key="member.id || member.email" class="member-row">
                  <span>{{ member.fullName }}</span>
                  <span class="text-soft">{{ member.email }}</span>
                </div>
              </div>
            </article>
          </div>
          <div v-else class="empty-box">{{ tx("Поки що немає історії команд.", "No team history yet.") }}</div>
        </SectionBlock>

        <SectionBlock :title="tx('Керовані турніри', 'Managed tournaments')" :description="tx('Події, створені або контрольовані вашою роллю.', 'Events created or controlled by your role.')" :eyebrow="tx('Адмін', 'Admin')">
          <div v-if="profile.managedTournaments.length" class="grid-auto">
            <TournamentCard v-for="tournament in profile.managedTournaments" :key="tournament.id" :tournament="tournament" />
          </div>
          <div v-else class="empty-box">{{ tx("До цього акаунта не прив’язано керованих турнірів.", "No managed tournaments are linked to this account.") }}</div>
        </SectionBlock>
      </section>

      <SectionBlock
        :title="tx('Діяльність журі', 'Jury activity')"
        :description="tx('Призначені подання та статус оцінювання.', 'Assigned submissions and evaluation status.')"
        :eyebrow="tx('Огляд', 'Overview')"
        v-if="profile.role === 'JURY'"
      >
        <div v-if="profile.juryAssignments.length" class="stack">
          <article v-for="assignment in profile.juryAssignments" :key="assignment.assignmentId" class="panel stack-sm">
            <div class="toolbar">
              <div>
                <h3 class="title-sm">{{ assignment.submission.teamName }}</h3>
                <p class="text-soft">{{ assignment.submission.taskTitle }}</p>
              </div>
              <StatusBadge :status="assignment.status" />
            </div>
            <p class="text-soft">{{ assignment.submission.githubUrl }}</p>
          </article>
        </div>
        <div v-else class="empty-box">{{ tx("Поки що немає призначень журі.", "No jury assignments yet.") }}</div>
      </SectionBlock>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import SectionBlock from "../components/SectionBlock.vue";
import StatusBadge from "../components/StatusBadge.vue";
import TournamentCard from "../components/TournamentCard.vue";
import { authStore } from "../services/auth";
import { notifier } from "../services/notify";
import { formatRole, getErrorMessage } from "../services/formatters";
import { t, tx } from "../services/i18n";

const router = useRouter();
const loading = ref(true);
const errorMessage = ref("");
const profile = ref(null);
const selectedRole = ref("TEAM");
const changingRole = ref(false);
const roleErrorMessage = ref("");
const roleOptions = computed(() => [
  { value: "USER", label: formatRole("USER"), description: tx("Базовий доступ користувача.", "Basic user access.") },
  { value: "ADMIN", label: formatRole("ADMIN"), description: tx("Повне адміністрування турнірів.", "Full tournament administration.") }
]);
const hasUnfinishedManagedTournaments = computed(() =>
  Boolean(profile.value?.managedTournaments?.some((tournament) => tournament.status !== "FINISHED"))
);
const roleChangeBlocked = computed(() => Boolean(profile.value?.inTeam || hasUnfinishedManagedTournaments.value));

async function loadProfile() {
  loading.value = true;
  errorMessage.value = "";
  try {
    profile.value = await authStore.refreshProfile();
    selectedRole.value = normalizeRoleSelection(profile.value.role);
  } catch (error) {
    errorMessage.value = getErrorMessage(error);
    notifier.pushNotification(errorMessage.value, "error");
  } finally {
    loading.value = false;
  }
}

onMounted(loadProfile);

async function handleRoleChange() {
  if (!profile.value || selectedRole.value === profile.value.role) {
    return;
  }

  changingRole.value = true;
  roleErrorMessage.value = "";
  try {
    profile.value = await authStore.changeRole(selectedRole.value);
    selectedRole.value = normalizeRoleSelection(profile.value.role);
    notifier.pushNotification(tx("Роль оновлено.", "Role updated."), "success");
  } catch (error) {
    roleErrorMessage.value = getErrorMessage(error);
    notifier.pushNotification(roleErrorMessage.value, "error");
  } finally {
    changingRole.value = false;
  }
}

function normalizeRoleSelection(role) {
  return roleOptions.value.some((option) => option.value === role) ? role : "USER";
}

async function handleLogout() {
  await authStore.logout();
  notifier.pushNotification(t("notifications.logout"), "success");
  router.push("/");
}
</script>
