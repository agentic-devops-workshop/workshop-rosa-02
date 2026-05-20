"use client";

import { useMemo, useState } from "react";
import { api } from "@/lib/api";
import { formatMoney } from "@/lib/money";

const FATOR_K = 0.347215; // REQ-ADM-004 (MYS-003) — exibição apenas; backend é a fonte da verdade.

export function ProgramForm() {
  const [code, setCode] = useState("");
  const [name, setName] = useState("");
  const [type, setType] = useState<"P" | "A" | "T">("A");
  const [baseValueInput, setBaseValueInput] = useState("");
  const [adjustmentFactor, setAdjustmentFactor] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [ok, setOk] = useState<string | null>(null);

  const adjusted = useMemo(() => {
    const base = parseFloat(baseValueInput);
    const factor = parseFloat(adjustmentFactor);
    if (isNaN(base)) return null;
    const f = isNaN(factor) ? 0 : factor;
    const value = base * (1 + f * FATOR_K);
    return Math.floor(value * 100) / 100;
  }, [baseValueInput, adjustmentFactor]);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setOk(null);
    try {
      await api.post("/api/v1/admin/programs", {
        code,
        name,
        type,
        baseValueInput: parseFloat(baseValueInput),
        adjustmentFactor: parseFloat(adjustmentFactor || "0"),
      });
      setOk("Programa cadastrado com sucesso.");
      setCode(""); setName(""); setBaseValueInput(""); setAdjustmentFactor("");
    } catch (err) {
      setError((err as Error).message);
    }
  }

  return (
    <form onSubmit={submit} className="space-y-4 rounded-lg border bg-white p-6 shadow-sm">
      <div className="grid grid-cols-2 gap-3">
        <label className="block">
          <span className="mb-1 block text-sm font-medium">Código</span>
          <input value={code} onChange={(e) => setCode(e.target.value)} required maxLength={8}
                 className="w-full rounded border px-3 py-2" placeholder="BPC1" />
          <span className="mt-1 block text-xs text-slate-500">REQ-ADM-001: deve ser único</span>
        </label>
        <label className="block">
          <span className="mb-1 block text-sm font-medium">Tipo</span>
          <select value={type} onChange={(e) => setType(e.target.value as "P" | "A" | "T")}
                  className="w-full rounded border px-3 py-2">
            <option value="P">P · Previdenciário (≥ 60 anos)</option>
            <option value="A">A · Assistencial (sem restrição etária)</option>
            <option value="T">T · Trabalho (16-65 anos)</option>
          </select>
        </label>
      </div>

      <label className="block">
        <span className="mb-1 block text-sm font-medium">Nome</span>
        <input value={name} onChange={(e) => setName(e.target.value)} required
               className="w-full rounded border px-3 py-2" />
      </label>

      <div className="grid grid-cols-2 gap-3">
        <label className="block">
          <span className="mb-1 block text-sm font-medium">Valor Base (entrada)</span>
          <input type="number" step="0.01" value={baseValueInput}
                 onChange={(e) => setBaseValueInput(e.target.value)} required
                 className="w-full rounded border px-3 py-2" placeholder="500.00" />
        </label>
        <label className="block">
          <span className="mb-1 block text-sm font-medium">FATOR-REAJ</span>
          <input type="number" step="0.0001" value={adjustmentFactor}
                 onChange={(e) => setAdjustmentFactor(e.target.value)}
                 className="w-full rounded border px-3 py-2" placeholder="0.10" />
        </label>
      </div>

      {adjusted !== null && (
        <div className="rounded bg-blue-50 p-3 text-sm text-blue-900">
          <strong>REQ-ADM-004 · FATOR-K = {FATOR_K}</strong>
          <br />
          VLR-BASE-AJUSTADO = {baseValueInput || 0} × (1 + {adjustmentFactor || 0} × {FATOR_K}) ={" "}
          <strong>{formatMoney(adjusted)}</strong>
        </div>
      )}

      {error && <p className="rounded bg-red-50 p-2 text-sm text-red-700">{error}</p>}
      {ok && <p className="rounded bg-green-50 p-2 text-sm text-green-700">{ok}</p>}

      <button type="submit" className="rounded bg-blue-600 px-4 py-2 font-medium text-white hover:bg-blue-700">
        Cadastrar Programa
      </button>
    </form>
  );
}
