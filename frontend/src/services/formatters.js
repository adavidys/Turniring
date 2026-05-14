import { tx, useLang } from "./i18n";

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
  const status = Number(error?.status) || 0;
  const message = translateErrorText(error?.message, status);
  const details = Array.isArray(error?.details)
    ? error.details.map((detail) => translateValidationDetail(detail, status)).filter(Boolean)
    : [];

  if (!details.length) {
    return message;
  }

  return [message, ...details].join(" ");
}

const exactErrorTranslations = new Map([
  ["Server is unavailable. Please try again.", () => tx("Сервер недоступний. Спробуйте ще раз.", "Server is unavailable. Please try again.")],
  ["Failed to load CSRF token", () => tx("Не вдалося завантажити CSRF токен.", "Failed to load CSRF token.")],
  ["Request failed", () => tx("Запит не виконано.", "Request failed.")],
  ["Download failed", () => tx("Не вдалося завантажити файл.", "Download failed.")],
  ["Validation failed", () => tx("Помилка валідації даних.", "Validation failed.")],
  ["Data constraint violation", () => tx("Порушено обмеження даних.", "Data constraint violation.")],
  ["Database operation failed", () => tx("Помилка операції з базою даних.", "Database operation failed.")],
  ["Unexpected server error", () => tx("Неочікувана помилка сервера.", "Unexpected server error.")],
  ["Authentication is required", () => tx("Потрібна авторизація.", "Authentication is required.")],
  ["Insufficient permissions", () => tx("Недостатньо прав доступу.", "Insufficient permissions.")],
  ["Invalid email or password", () => tx("Невірний email або пароль.", "Invalid email or password.")],
  ["User not found", () => tx("Користувача не знайдено.", "User not found.")],
  ["JURY role can be granted only through invite links", () => tx("Роль JURY надається лише через запрошення.", "JURY role can be granted only through invite links.")],
  ["This link is not active", () => tx("Це посилання неактивне.", "This link is not active.")],
  ["User in team cannot accept jury invite", () => tx("Користувач у команді не може прийняти запрошення журі.", "User in team cannot accept jury invite.")],
  ["Team invite has no team", () => tx("Запрошення до команди не містить команди.", "Team invite has no team.")],
  ["User is already in this team", () => tx("Користувач уже в цій команді.", "User is already in this team.")],
  ["Team is full", () => tx("Команда вже заповнена.", "Team is full.")],
  ["Team name already exists", () => tx("Назва команди вже існує.", "Team name already exists.")],
  ["Team is already joined to an olympiad", () => tx("Команда вже приєднана до олімпіади.", "Team is already joined to an olympiad.")],
  ["Tournament registration is closed", () => tx("Реєстрацію на турнір завершено.", "Tournament registration is closed.")],
  ["Captain has already registered a team", () => tx("Капітан уже зареєстрував команду.", "Captain has already registered a team.")],
  ["Team name already registered in this tournament", () => tx("Назву команди вже зареєстровано в цьому турнірі.", "Team name already registered in this tournament.")],
  ["Tournament team limit has been reached", () => tx("Досягнуто ліміт команд у турнірі.", "Tournament team limit has been reached.")],
  ["Team is not joined to any olympiad", () => tx("Команда не приєднана до жодної олімпіади.", "Team is not joined to any olympiad.")],
  ["Team with submissions cannot leave olympiad", () => tx("Команда з поданнями не може вийти з олімпіади.", "Team with submissions cannot leave olympiad.")],
  ["Team with submissions cannot be deleted", () => tx("Команду з поданнями не можна видалити.", "Team with submissions cannot be deleted.")],
  ["Only the captain can edit this team", () => tx("Редагувати команду може лише капітан.", "Only the captain can edit this team.")],
  ["Only the captain can manage this team", () => tx("Керувати командою може лише капітан.", "Only the captain can manage this team.")],
  ["Team editing is closed after registration", () => tx("Редагування команди закрито після завершення реєстрації.", "Team editing is closed after registration.")],
  ["Team member emails must be unique", () => tx("Email учасників команди мають бути унікальними.", "Team member emails must be unique.")],
  ["Team name is required", () => tx("Назва команди обов'язкова.", "Team name is required.")],
  ["Task submission window is closed", () => tx("Вікно подання завдання закрито.", "Task submission window is closed.")],
  ["Submission not found", () => tx("Подання не знайдено.", "Submission not found.")],
  ["No submissions available for assignment", () => tx("Немає подань для призначення.", "No submissions available for assignment.")],
  ["Not enough jury users to assign evaluations", () => tx("Недостатньо користувачів журі для призначення оцінювання.", "Not enough jury users to assign evaluations.")],
  ["Assignment not found", () => tx("Призначення не знайдено.", "Assignment not found.")],
  ["Assignment belongs to another jury member", () => tx("Це призначення належить іншому члену журі.", "Assignment belongs to another jury member.")],
  ["Tournament not found", () => tx("Турнір не знайдено.", "Tournament not found.")],
  ["Task not found", () => tx("Завдання не знайдено.", "Task not found.")],
  ["Task deadline must be after start", () => tx("Дедлайн завдання має бути після старту.", "Task deadline must be after start.")],
  ["Schedule event end must be after start", () => tx("Завершення події розкладу має бути після початку.", "Schedule event end must be after start.")],
  ["Registration end must be after registration start", () => tx("Кінець реєстрації має бути після початку.", "Registration end must be after registration start.")],
  ["Tournament start must be after registration ends", () => tx("Початок турніру має бути після завершення реєстрації.", "Tournament start must be after registration ends.")],
  ["Team max members must be greater than or equal to team min members", () => tx("Максимум учасників має бути не меншим за мінімум.", "Team max members must be greater than or equal to team min members.")],
  ["Confirmation text must exactly match tournament title", () => tx("Текст підтвердження має точно збігатися з назвою турніру.", "Confirmation text must exactly match tournament title.")],
  ["Admin can create only one olympiad", () => tx("Адміністратор може створити лише одну олімпіаду.", "Admin can create only one olympiad.")],
  ["TEAM role is managed via team pages. Choose USER or ADMIN.", () =>
    tx("Роль TEAM керується через сторінки команди. Оберіть USER або ADMIN.", "TEAM role is managed via team pages. Choose USER or ADMIN.")
  ]
]);

const validationMessageTranslations = new Map([
  ["Title is required", () => tx("Назва обов'язкова.", "Title is required.")],
  ["Description is required", () => tx("Опис обов'язковий.", "Description is required.")],
  ["Content is required", () => tx("Вміст обов'язковий.", "Content is required.")],
  ["Email is required", () => tx("Email обов'язковий.", "Email is required.")],
  ["Password is required", () => tx("Пароль обов'язковий.", "Password is required.")],
  ["Role is required", () => tx("Роль обов'язкова.", "Role is required.")],
  ["Name is required", () => tx("Ім'я обов'язкове.", "Name is required.")],
  ["Last name is required", () => tx("Прізвище обов'язкове.", "Last name is required.")],
  ["Full name is required", () => tx("Повне ім'я обов'язкове.", "Full name is required.")],
  ["Username is required", () => tx("Ім'я користувача обов'язкове.", "Username is required.")],
  ["Members list is required", () => tx("Список учасників обов'язковий.", "Members list is required.")],
  ["GitHub URL is required", () => tx("GitHub URL обов'язковий.", "GitHub URL is required.")],
  ["Demo video URL is required", () => tx("URL демо-відео обов'язковий.", "Demo video URL is required.")],
  ["Start time is required", () => tx("Час початку обов'язковий.", "Start time is required.")],
  ["End time is required", () => tx("Час завершення обов'язковий.", "End time is required.")],
  ["Deadline is required", () => tx("Дедлайн обов'язковий.", "Deadline is required.")],
  ["Registration start time is required", () => tx("Час початку реєстрації обов'язковий.", "Registration start time is required.")],
  ["Registration end time is required", () => tx("Час завершення реєстрації обов'язковий.", "Registration end time is required.")],
  ["Minimum rounds is required", () => tx("Мінімум раундів обов'язковий.", "Minimum rounds is required.")],
  ["Minimum team members is required", () => tx("Мінімум учасників команди обов'язковий.", "Minimum team members is required.")],
  ["Maximum team members is required", () => tx("Максимум учасників команди обов'язковий.", "Maximum team members is required.")],
  ["Confirmation text is required", () => tx("Текст підтвердження обов'язковий.", "Confirmation text is required.")],
  ["Evaluators per submission is required", () => tx("Кількість оцінювачів на подання обов'язкова.", "Evaluators per submission is required.")],
  ["Invalid email format", () => tx("Невірний формат email.", "Invalid email format.")],
  ["Password must be at least 8 characters", () => tx("Пароль має містити щонайменше 8 символів.", "Password must be at least 8 characters.")],
  ["Password must be at least 8 characters long", () => tx("Пароль має містити щонайменше 8 символів.", "Password must be at least 8 characters long.")],
  ["Name must be between 2 and 255 characters", () => tx("Ім'я має містити від 2 до 255 символів.", "Name must be between 2 and 255 characters.")],
  ["Last name must be between 2 and 255 characters", () => tx("Прізвище має містити від 2 до 255 символів.", "Last name must be between 2 and 255 characters.")],
  ["The name must be between 2 and 255 characters.", () => tx("Ім'я має містити від 2 до 255 символів.", "Name must be between 2 and 255 characters.")],
  ["Team name must be between 2 and 255 characters", () => tx("Назва команди має містити від 2 до 255 символів.", "Team name must be between 2 and 255 characters.")],
  ["Title must be at most 255 characters", () => tx("Назва має містити не більше 255 символів.", "Title must be at most 255 characters.")],
  ["Max teams must be at least 1", () => tx("Максимум команд має бути не менше 1.", "Max teams must be at least 1.")],
  ["Minimum rounds must be at least 1", () => tx("Мінімум раундів має бути не менше 1.", "Minimum rounds must be at least 1.")],
  ["Minimum team members must be at least 1", () => tx("Мінімум учасників команди має бути не менше 1.", "Minimum team members must be at least 1.")],
  ["Maximum team members must be at least 1", () => tx("Максимум учасників команди має бути не менше 1.", "Maximum team members must be at least 1.")],
  ["Evaluators per submission must be at least 1", () =>
    tx("Кількість оцінювачів на подання має бути не менше 1.", "Evaluators per submission must be at least 1.")
  ],
  ["Max assignments per jury must be at least 1", () => tx("Максимум призначень на члена журі має бути не менше 1.", "Max assignments per jury must be at least 1.")],
  ["Registration start time must be in the present or future", () =>
    tx("Час початку реєстрації має бути в теперішньому або майбутньому.", "Registration start time must be in the present or future.")
  ],
  ["Registration end time must be in the future", () => tx("Час завершення реєстрації має бути в майбутньому.", "Registration end time must be in the future.")],
  ["End time must be in the future", () => tx("Час завершення має бути в майбутньому.", "End time must be in the future.")],
  ["Deadline must be in the future", () => tx("Дедлайн має бути в майбутньому.", "Deadline must be in the future.")]
]);

const statusFallbackTranslations = {
  0: () => tx("Сервер недоступний. Спробуйте ще раз.", "Server is unavailable. Please try again."),
  400: () => tx("Помилка в запиті.", "Invalid request."),
  401: () => tx("Потрібна авторизація.", "Authentication is required."),
  403: () => tx("Недостатньо прав доступу.", "Insufficient permissions."),
  404: () => tx("Ресурс не знайдено.", "Resource not found."),
  409: () => tx("Конфлікт даних.", "Data conflict."),
  500: () => tx("Внутрішня помилка сервера.", "Internal server error.")
};

const fieldNameTranslations = new Map([
  ["title", () => tx("Назва", "Title")],
  ["description", () => tx("Опис", "Description")],
  ["content", () => tx("Вміст", "Content")],
  ["email", () => tx("Email", "Email")],
  ["password", () => tx("Пароль", "Password")],
  ["name", () => tx("Ім'я", "Name")],
  ["lastName", () => tx("Прізвище", "Last name")],
  ["fullName", () => tx("Повне ім'я", "Full name")],
  ["role", () => tx("Роль", "Role")],
  ["members", () => tx("Учасники", "Members")],
  ["githubUrl", () => tx("GitHub URL", "GitHub URL")],
  ["demoVideoUrl", () => tx("Demo URL", "Demo URL")],
  ["startAt", () => tx("Початок", "Start")],
  ["endAt", () => tx("Кінець", "End")],
  ["deadlineAt", () => tx("Дедлайн", "Deadline")],
  ["registrationStartAt", () => tx("Початок реєстрації", "Registration start")],
  ["registrationEndAt", () => tx("Кінець реєстрації", "Registration end")],
  ["teamMinMembers", () => tx("Мінімум учасників", "Minimum team members")],
  ["teamMaxMembers", () => tx("Максимум учасників", "Maximum team members")],
  ["minimumRounds", () => tx("Мінімум раундів", "Minimum rounds")],
  ["maxTeams", () => tx("Максимум команд", "Max teams")],
  ["confirmationText", () => tx("Текст підтвердження", "Confirmation text")],
  ["evaluatorsPerSubmission", () => tx("Оцінювачів на подання", "Evaluators per submission")],
  ["maxAssignmentsPerJury", () => tx("Макс. призначень на журі", "Max assignments per jury")]
]);

function translateErrorText(input, status = 0) {
  const message = normalizeErrorText(input);
  if (!message) {
    return fallbackByStatus(status);
  }

  const exactTranslator = exactErrorTranslations.get(message);
  if (exactTranslator) {
    return exactTranslator();
  }

  const dynamicTranslation = translateDynamicMessage(message);
  if (dynamicTranslation) {
    return dynamicTranslation;
  }

  const validationTranslator = validationMessageTranslations.get(message);
  if (validationTranslator) {
    return validationTranslator();
  }

  if (containsCyrillic(message)) {
    return message;
  }

  if (lang.value === "en") {
    return message;
  }

  return fallbackByStatus(status);
}

function translateDynamicMessage(message) {
  let match = message.match(/^User with email\s+(.+)\s+already exists$/);
  if (match) {
    const email = match[1];
    return tx(`Користувач з email ${email} уже існує.`, `User with email ${email} already exists.`);
  }

  match = message.match(/^Team must have at least\s+(\d+)\s+members$/);
  if (match) {
    return tx(`Команда має містити щонайменше ${match[1]} учасників.`, `Team must have at least ${match[1]} members.`);
  }

  match = message.match(/^Team must have no more than\s+(\d+)\s+members$/);
  if (match) {
    return tx(`Команда має містити не більше ${match[1]} учасників.`, `Team must have no more than ${match[1]} members.`);
  }

  match = message.match(/^size must be between\s+(\d+)\s+and\s+(\d+)$/);
  if (match) {
    return tx(`Довжина має бути від ${match[1]} до ${match[2]} символів.`, `Length must be between ${match[1]} and ${match[2]} characters.`);
  }

  match = message.match(/^must be greater than or equal to\s+(-?\d+)$/);
  if (match) {
    return tx(`Значення має бути не менше ${match[1]}.`, `Value must be greater than or equal to ${match[1]}.`);
  }

  match = message.match(/^must be less than or equal to\s+(-?\d+)$/);
  if (match) {
    return tx(`Значення має бути не більше ${match[1]}.`, `Value must be less than or equal to ${match[1]}.`);
  }

  if (message === "must not be null") {
    return tx("Поле обов'язкове.", "Field is required.");
  }
  if (message === "must not be blank" || message === "must not be empty") {
    return tx("Поле не може бути порожнім.", "Field must not be blank.");
  }
  if (message === "must be a well-formed email address") {
    return tx("Невірний формат email.", "Invalid email format.");
  }
  if (message === "must be a future date") {
    return tx("Дата має бути в майбутньому.", "Date must be in the future.");
  }
  if (message === "must be a date in the present or in the future") {
    return tx("Дата має бути в теперішньому або майбутньому.", "Date must be in the present or in the future.");
  }

  return "";
}

function translateValidationDetail(detail, status = 0) {
  const text = normalizeErrorText(detail);
  if (!text) {
    return "";
  }

  const fieldMatch = text.match(/^([^:]+):\s*(.+)$/);
  if (!fieldMatch) {
    return translateErrorText(text, status);
  }

  const [, fieldName, rawMessage] = fieldMatch;
  const translatedField = translateFieldName(fieldName);
  const translatedMessage = translateErrorText(rawMessage, status);
  return `${translatedField}: ${translatedMessage}`;
}

function translateFieldName(fieldName) {
  const normalized = String(fieldName || "").trim();
  const translator = fieldNameTranslations.get(normalized);
  if (translator) {
    return translator();
  }
  return normalized || tx("Поле", "Field");
}

function fallbackByStatus(status) {
  const translator = statusFallbackTranslations[status] || statusFallbackTranslations[500];
  return translator();
}

function normalizeErrorText(text) {
  return String(text || "").trim();
}

function containsCyrillic(text) {
  return /[А-Яа-яІіЇїЄєҐґ]/.test(text);
}
