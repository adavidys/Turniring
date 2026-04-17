import { createRouter, createWebHistory } from "vue-router";
import HomePage from "../pages/HomePage.vue";
import TournamentPage from "../pages/TournamentPage.vue";
import LoginPage from "../pages/LoginPage.vue";
import RegisterPage from "../pages/RegisterPage.vue";
import ProfilePage from "../pages/ProfilePage.vue";
import TeamWorkspacePage from "../pages/TeamWorkspacePage.vue";
import JuryWorkspacePage from "../pages/JuryWorkspacePage.vue";
import AdminDashboardPage from "../pages/AdminDashboardPage.vue";
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
    path: "/team",
    name: "team",
    component: TeamWorkspacePage,
    meta: { requiresAuth: true, roles: ["TEAM", "USER", "ADMIN"] }
  },
  {
    path: "/jury",
    name: "jury",
    component: JuryWorkspacePage,
    meta: { requiresAuth: true, roles: ["JURY"] }
  },
  {
    path: "/admin",
    name: "admin",
    component: AdminDashboardPage,
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
