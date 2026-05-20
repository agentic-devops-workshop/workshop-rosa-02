"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";

export function CycleGenerationForm({ defaultCycle }: { defaultCycle: string }) {
  const [cycle, setCycle] = useState(defaultCycle);
  const [programId, setProgramId] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const router = useRouter();

  async function generate(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await api.post(`/api/v1/payments/cycles?cycle=${cycle}&programId=${programId}`, {});
      router.replace(`/payments?cycle=${cycle}`);
      router.refresh();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setBusy(false);
    }
  }

  const isDecember = cycle.endsWith("-12");

  return (
    <form onSubmit={generate} className="grid grid-cols-1 gap-3 rounded-lg border bg-white p-4 shadow-sm md:grid-cols-3 md:items-end">
      <label className="block">
        <span className="mb-1 block text-sm font-medium">Ciclo (YYYY-MM)</span>
        <input type="month" value={cycle} onChange={(e) => setCycle(e.target.value)} required
               className="w-full rounded border px-3 py-2" />
        {isDecember && (
          <span className="mt-1 block text-xs text-amber-600">
            Dezembro: programas A geram 13º + abono 15% (REQ-PAY-003)
          </span>
        )}
      </label>
      <label className="block">
        <span className="mb-1 block text-sm font-medium">ID do Programa</span>
        <input value={programId} onChange={(e) => setProgramId(e.target.value)} required
               className="w-full rounded border px-3 py-2" placeholder="1" />
      </label>
      <button type="submit" disabled={busy}
              className="rounded bg-blue-600 px-4 py-2 font-medium text-white hover:bg-blue-700 disabled:opacity-50">
        {busy ? "Processando..." : "Gerar Ciclo"}
      </button>
      {error && <p className="md:col-span-3 rounded bg-red-50 p-2 text-sm text-red-700">{error}</p>}
    </form>
  );
}
