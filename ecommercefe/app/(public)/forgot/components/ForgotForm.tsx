"use client";

import { Button } from "@/components/ui/button";
import { Input, Field } from "@/components/ui/input";

interface ForgotFormProps {
  email: string;
  setEmail: (v: string) => void;
  loading: boolean;
  onSubmit: () => void;
}

export function ForgotForm({ email, setEmail, loading, onSubmit }: ForgotFormProps) {
  return (
    <form onSubmit={(e) => { e.preventDefault(); onSubmit(); }} className="mt-6 space-y-4">
      <Field label="Email" required>
        <Input type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
      </Field>
      <Button variant="cta" size="lg" className="w-full" loading={loading} type="submit">
        Gửi liên kết
      </Button>
    </form>
  );
}
