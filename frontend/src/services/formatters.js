const dateTimeFormatter = new Intl.DateTimeFormat("uk-UA", {
  dateStyle: "medium",
  timeStyle: "short"
});

const shortDateFormatter = new Intl.DateTimeFormat("uk-UA", {
  dateStyle: "medium"
});

export function formatDateTime(value) {
  return value ? dateTimeFormatter.format(new Date(value)) : "Not set";
}

export function formatDate(value) {
  return value ? shortDateFormatter.format(new Date(value)) : "Not set";
}

export function formatStatus(value) {
  return (value || "")
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

export function getErrorMessage(error) {
  if (!error) {
    return "Unexpected error";
  }
  if (error.details?.length) {
    return [error.message, ...error.details].join(" ");
  }
  return error.message || "Unexpected error";
}
