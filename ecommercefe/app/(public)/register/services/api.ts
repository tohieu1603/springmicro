import axios from "axios";

export class RegisterApi {
  static async register(payload: {
    username: string;
    email: string;
    password: string;
    firstName: string;
    lastName: string;
  }) {
    await axios.post("/api/auth/register", payload);
  }
}

/**
 * Split a free-text "Họ và tên" on the LAST space so multi-word given names
 * collapse into firstName: "Nguyễn Văn A" → first="A", last="Nguyễn Văn".
 */
export function splitFullName(fullName: string, fallback: string): { firstName: string; lastName: string } {
  const trimmed = fullName.trim();
  const lastSpace = trimmed.lastIndexOf(" ");
  const firstName = lastSpace === -1 ? trimmed : trimmed.slice(lastSpace + 1);
  const lastName = lastSpace === -1 ? trimmed : trimmed.slice(0, lastSpace);
  return {
    firstName: firstName || fallback,
    lastName: lastName || fallback,
  };
}
