"use client";

import { useRouter } from "next/navigation";
import axios from "axios";
import { LogOut } from "lucide-react";
import { Button } from "@/components/ui/button";

export function LogoutButton() {
  const router = useRouter();
  async function logout() {
    try {
      await axios.post("/api/auth/logout", null, { withCredentials: true });
    } finally {
      router.replace("/");
      router.refresh();
    }
  }
  return (
    <Button variant="ghost" onClick={logout}>
      <LogOut className="h-4 w-4" /> Đăng xuất
    </Button>
  );
}
