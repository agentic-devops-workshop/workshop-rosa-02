"use client";

import { useMemo, useState } from "react";
import { api } from "@/lib/api";
import { BRAZILIAN_STATES, isValidUf } from "@/lib/ufs";
import { isSpecialCpf, isValidCpf } from "@/lib/cpf";

interface DependentDraft {
  name: string;
  birthDate: string;
}

export function BeneficiaryForm() {
  const [cpf, setCpf] = useState("");
  const [name, setName] = useState("");
  const [birthDate, setBirthDate] = useState("");
  const [uf, setUf] = useState("");
  const [regionCode, setRegionCode] = useState("");
  const [dependents, setDependents] = useState<DependentDraft[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [ok, setOk] = useState<string | null>(null);

  const age = useMemo(() => {
    if (!birthDate) return null;
    const d = new Date(birthDate);
    const diff = Date.now() - d.getTime();
    return Math.floor(diff / (1000 * 60 * 60 * 24 * 365.25));
  }, [birthDate]);

  const willAutoSuspend = age !== null && age > 75; // REQ-BEN-002
  const dependentsExceeded = dependents.length > 5;  // REQ-BEN-003

  function addDependent() {
    if (dependents.length >= 5) {
      setError("Limite de 5 dependentes atingido (REQ-BEN-003)");
      return;
    }
    setDependents([...dependents, { name: "", birthDate: "" }]);
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setOk(null);

    if (uf && !isValidUf(uf)) {
      setError("UF inválida (REQ-BEN-005)");
      return;
    }
    if (dependentsExceeded) {
      setError("Mais de 5 dependentes (REQ-BEN-003)");
      return;
    }

    if (!isValidCpf(cpf)) {
      setError("CPF inválido (regra de formação mod-11). Verifique os dígitos verificadores.");
      return;
    }

    try {
      const cleaned = cpf.replace(/\D/g, "");
      const created = await api.post("/api/v1/beneficiaries", {
        cpf: cleaned,
        name,
        birthDate,
        uf: uf || null,
        regionCode: regionCode || null,
        status: "A",
      });
      for (const d of dependents) {
        if (!d.name || !d.birthDate) continue;
        await api.post(
          `/api/v1/beneficiaries/${(created as { id: number }).id}/dependents`,
          d,
        );
      }
      setOk("Beneficiário cadastrado com sucesso.");
      setCpf(""); setName(""); setBirthDate(""); setUf(""); setRegionCode(""); setDependents([]);
    } catch (err) {
      setError((err as Error).message);
    }
  }

  return (
    <form onSubmit={submit} className="space-y-4 rounded-lg border bg-white p-6 shadow-sm">
      <Field label="CPF" hint={
        isSpecialCpf(cpf)
          ? "Prefixo especial — bypass documental (REQ-BEN-006)"
          : cpf && !isValidCpf(cpf)
            ? "⚠ CPF inválido pela regra mod-11"
            : "11 dígitos · validação mod-11"
      }>
        <input value={cpf} onChange={(e) => setCpf(e.target.value)} required
               className={`w-full rounded border px-3 py-2 ${cpf && !isValidCpf(cpf) && !isSpecialCpf(cpf) ? "border-red-400" : ""}`}
               placeholder="00000000000" />
      </Field>
      <Field label="Nome">
        <input value={name} onChange={(e) => setName(e.target.value)} required
               className="w-full rounded border px-3 py-2" />
      </Field>
      <Field label="Data de Nascimento"
             hint={willAutoSuspend ? `Idade ${age} > 75 → status SUSPENDED automático (REQ-BEN-002)` : age !== null ? `Idade: ${age}` : undefined}>
        <input type="date" value={birthDate} onChange={(e) => setBirthDate(e.target.value)} required
               className="w-full rounded border px-3 py-2" />
      </Field>
      <div className="grid grid-cols-2 gap-3">
        <Field label="UF">
          <select value={uf} onChange={(e) => setUf(e.target.value)} className="w-full rounded border px-3 py-2">
            <option value="">—</option>
            {BRAZILIAN_STATES.map((u) => <option key={u} value={u}>{u}</option>)}
          </select>
        </Field>
        <Field label="Código Região" hint="99 = elegibilidade automática (REQ-ADM-003)">
          <input value={regionCode} onChange={(e) => setRegionCode(e.target.value)} maxLength={2}
                 className="w-full rounded border px-3 py-2" />
        </Field>
      </div>

      <fieldset className="rounded border p-3">
        <legend className="px-1 text-sm font-medium">Dependentes ({dependents.length}/5) · REQ-BEN-003</legend>
        {dependents.map((d, i) => (
          <div key={i} className="my-2 grid grid-cols-2 gap-2">
            <input placeholder="Nome" value={d.name}
                   onChange={(e) => setDependents(dependents.map((x, j) => j === i ? { ...x, name: e.target.value } : x))}
                   className="rounded border px-2 py-1" />
            <input type="date" value={d.birthDate}
                   onChange={(e) => setDependents(dependents.map((x, j) => j === i ? { ...x, birthDate: e.target.value } : x))}
                   className="rounded border px-2 py-1" />
          </div>
        ))}
        <button type="button" onClick={addDependent} disabled={dependents.length >= 5}
                className="rounded bg-slate-200 px-3 py-1 text-sm disabled:opacity-50">
          + Dependente
        </button>
      </fieldset>

      {error && <p className="rounded bg-red-50 p-2 text-sm text-red-700">{error}</p>}
      {ok && <p className="rounded bg-green-50 p-2 text-sm text-green-700">{ok}</p>}

      <button type="submit" className="rounded bg-blue-600 px-4 py-2 font-medium text-white hover:bg-blue-700">
        Cadastrar
      </button>
    </form>
  );
}

function Field({ label, hint, children }: { label: string; hint?: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="mb-1 block text-sm font-medium text-slate-700">{label}</span>
      {children}
      {hint && <span className="mt-1 block text-xs text-slate-500">{hint}</span>}
    </label>
  );
}
