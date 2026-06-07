import axios from "axios";

interface LoginResp {
  data?: { user?: { roles?: string[] } };
  user?: { roles?: string[] };
}

export class LoginApi {
  /**
   * Calls the Next.js proxy `/api/auth/login` which forwards to auth-service
   * and re-emits the HttpOnly cookies. Returns the roles needed for the
   * post-login redirect (admin → /admin, customer → home/next).
   */
  static async login(usernameOrEmail: string, password: string): Promise<string[]> {
    const res = await axios.post<LoginResp>(
      "/api/auth/login",
      { usernameOrEmail, password },
      { withCredentials: true },
    );
    const data = res.data?.data ?? res.data;
    return data?.user?.roles ?? [];
  }
}
