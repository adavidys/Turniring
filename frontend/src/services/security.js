const SAFE_EXTERNAL_PROTOCOLS = new Set(["http:", "https:"]);
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function toSafeExternalUrl(value) {
  const input = String(value || "").trim();
  if (!input) {
    return "";
  }

  try {
    const base = typeof window !== "undefined" ? window.location.origin : "http://localhost";
    const url = new URL(input, base);
    return SAFE_EXTERNAL_PROTOCOLS.has(url.protocol) ? url.toString() : "";
  } catch {
    return "";
  }
}

export function toSafeMailto(email) {
  const normalizedEmail = String(email || "").trim();
  if (!EMAIL_PATTERN.test(normalizedEmail)) {
    return "";
  }
  return `mailto:${encodeURIComponent(normalizedEmail)}`;
}
