"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";

interface Props {
  initial: { action?: string; entityType?: string };
}

export function AuditSearch({ initial }: Props) {
  const router = useRouter();
  const params = useSearchParams();
  const [action, setAction] = useState(initial.action ?? "");
  const [entityType, setEntityType] = useState(initial.entityType ?? "");

  function apply(e: React.FormEvent) {
    e.preventDefault();
    const next = new URLSearchParams(params.toString());
    if (action) next.set("action", action);
    else next.delete("action");
    if (entityType) next.set("entityType", entityType);
    else next.delete("entityType");
    router.push(`/audit?${next.toString()}`);
  }

  return (
    <form onSubmit={apply} className="flex flex-wrap items-end gap-3 rounded-lg border bg-white p-4 shadow-sm">
      <label className="block">
        <span className="mb-1 block text-sm font-medium">Ação</span>
        <select value={action} onChange={(e) => setAction(e.target.value)}
                className="rounded border px-3 py-2">
          <option value="">Todas</option>
          <option value="IN">IN · Inclusão</option>
          <option value="AL">AL · Alteração</option>
          <option value="EX">EX · Exclusão</option>
        </select>
      </label>
      <label className="block">
        <span className="mb-1 block text-sm font-medium">Tipo de entidade</span>
        <input value={entityType} onChange={(e) => setEntityType(e.target.value)}
               placeholder="Beneficiary, Payment, SocialProgram..."
               className="rounded border px-3 py-2" />
      </label>
      <button type="submit" className="rounded bg-blue-600 px-4 py-2 font-medium text-white hover:bg-blue-700">
        Filtrar
      </button>
    </form>
  );
}
