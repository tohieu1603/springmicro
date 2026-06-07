import axios from "axios";

export class ForgotApi {
  static async sendResetLink(email: string): Promise<void> {
    await axios.post("/api/proxy/api/auth/forgot-password", { email });
  }
}
