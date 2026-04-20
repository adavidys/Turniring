import { createRouter, createWebHistory } from "vue-router";
import HomePage from "../pages/HomePage.vue";
import TournamentPage from "../pages/TournamentPage.vue";
import LoginPage from "../pages/LoginPage.vue";
import RegisterPage from "../pages/RegisterPage.vue";
import ProfilePage from "../pages/ProfilePage.vue";
import TeamWorkspacePage from "../pages/TeamWorkspacePage.vue";
import JuryWorkspacePage from "../pages/JuryWorkspacePage.vue";
import AdminDashboardPage from "../pages/AdminDashboardPage.vue";
import OlympiadJoinPage from "../pages/OlympiadJoinPage.vue";
import TeamsJoinPage from "../pages/TeamsJoinPage.vue";
import OlympiadCreatePage from "../pages/OlympiadCreatePage.vue";
import TeamCreatePage from "../pages/TeamCreatePage.vue";
import JuryAddPage from "../pages/JuryAddPage.vue";
import AdminUserCreatePage from "../pages/AdminUserCreatePage.vue";
import UserDataEditPage from "../pages/UserDataEditPage.vue";
import InviteAcceptPage from "../pages/InviteAcceptPage.vue";
import NotFoundPage from "../pages/NotFoundPage.vue";
import { authStore } from "../services/auth";

const routes = [
  {
    path: "/",
    name: "home",
    component: HomePage
  },
  {
    path: "/tournaments/:id",
    name: "tournament",
    component: TournamentPage,
    props: true
  },
  {
    path: "/auth/login",
    name: "login",
    component: LoginPage,
    meta: { guestOnly: true }
  },
  {
    path: "/auth/register",
    name: "register",
    component: RegisterPage,
    meta: { guestOnly: true }
  },
  {
    path: "/profile",
    name: "profile",
    component: ProfilePage,
    meta: { requiresAuth: true }
  },
  {
    path: "/profile/data",
    name: "profile-data",
    component: UserDataEditPage,
    meta: { requiresAuth: true }
  },
  {
    path: "/team",
    name: "team",
    component: TeamWorkspacePage,
    meta: { requiresAuth: true }
  },
  {
    path: "/teams/join",
    name: "teams-join",
    component: TeamsJoinPage
  },
  {
    path: "/teams/create",
    name: "teams-create",
    component: TeamCreatePage,
    meta: { requiresAuth: true }
  },
  {
    path: "/jury",
    name: "jury",
    component: JuryWorkspacePage,
    meta: { requiresAuth: true, roles: ["JURY"] }
  },
  {
    path: "/jury/add",
    name: "jury-add",
    component: JuryAddPage,
    meta: { requiresAuth: true, roles: ["ADMIN", "ORGANIZER"] }
  },
  {
    path: "/invite/:token",
    name: "invite-accept",
    component: InviteAcceptPage
  },
  {
    path: "/admin",
    name: "admin",
    component: AdminDashboardPage,
    meta: { requiresAuth: true, roles: ["ADMIN", "ORGANIZER"] }
  },
  {
    path: "/admin/users/create",
    name: "admin-users-create",
    component: AdminUserCreatePage,
    meta: { requiresAuth: true, roles: ["ADMIN", "ORGANIZER"] }
  },
  {
    path: "/olympiads/join",
    name: "olympiads-join",
    component: OlympiadJoinPage
  },
  {
    path: "/olympiads/create",
    name: "olympiads-create",
    component: OlympiadCreatePage,
    meta: { requiresAuth: true, roles: ["ADMIN", "ORGANIZER"] }
  },
  {
    path: "/:pathMatch(.*)*",
    name: "not-found",
    component: NotFoundPage
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0, behavior: "smooth" };
  }
});

router.beforeEach(async (to) => {
  await authStore.restoreSession();

  if (to.meta.requiresAuth && !authStore.isLoggedIn.value) {
    return {
      name: "login",
      query: { redirect: to.fullPath }
    };
  }

  if (to.meta.guestOnly && authStore.isLoggedIn.value) {
    return { name: "home" };
  }

  if (to.meta.roles && !authStore.hasRole(...to.meta.roles)) {
    return authStore.isLoggedIn.value ? { name: "profile" } : { name: "login" };
  }

  return true;
});

export default router;
