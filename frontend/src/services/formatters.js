import { useLang } from "./i18n";

const lang = useLang();

function getLocale() {
  return lang.value === "en" ? "en-US" : "uk-UA";
}

export function formatDateTime(value) {
  if (!value) {
    return lang.value === "en" ? "Not set" : "Не вказано";
  }
  return new Intl.DateTimeFormat(getLocale(), {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(new Date(value));
}

export function formatDate(value) {
  if (!value) {
    return lang.value === "en" ? "Not set" : "Не вказано";
  }
  return new Intl.DateTimeFormat(getLocale(), {
    dateStyle: "medium"
  }).format(new Date(value));
}

export function formatStatus(value) {
  const status = value || "";
  const translations = lang.value === "en"
    ? {
        DRAFT: "Draft",
        REGISTRATION: "Registration",
        RUNNING: "Running",
        FINISHED: "Finished",
        ACTIVE: "Active",
        SUBMISSION_CLOSED: "Submission closed",
        EVALUATED: "Evaluated",
        SUBMITTED: "Submitted",
        ASSIGNED: "Assigned",
        COMPLETED: "Completed"
      }
    : {
        DRAFT: "Чернетка",
        REGISTRATION: "Реєстрація",
        RUNNING: "У процесі",
        FINISHED: "Завершено",
        ACTIVE: "Активний",
        SUBMISSION_CLOSED: "Прийом закрито",
        EVALUATED: "Оцінено",
        SUBMITTED: "Подано",
        ASSIGNED: "Призначено",
        COMPLETED: "Завершено"
      };

  if (translations[status]) {
    return translations[status];
  }

  if (lang.value !== "en") {
    return status
      .toLowerCase()
      .split("_")
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(" ");
  }

  return status
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

export function getErrorMessage(error) {
  if (!error) {
    return lang.value === "en" ? "Unexpected error" : "Неочікувана помилка";
  }
  if (error.details?.length) {
    return [error.message, ...error.details].join(" ");
  }
  return error.message || (lang.value === "en" ? "Unexpected error" : "Неочікувана помилка");
}
