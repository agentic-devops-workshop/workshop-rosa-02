"use client";

import { useState } from "react";
import type { AuditEntry } from "@/lib/types";

const ACTION_STYLE: Record<string, string> = {
  IN: "bg-green-100 text-green-800",
  AL: "bg-blue-100 text-blue-800",
  EX: "bg-red-100 text-red-800",
};

function formatDate(iso: string): string {
  try {
    return new Date(iso).toLocaleString("pt-BR");
  } catch {
    return iso;
  }
}

export function AuditLog({ entries }: { entries: AuditEntry[] }) {
  const [selected, setSelected] = useState<AuditEntry | null>(null);

  if (entries.length === 0) {
    return <p className="text-sm text-slate-500">Nenhum evento de auditoria encontrado.</p>;
  }

  return (
    <>
      <div className="overflow-x-auto rounded-lg border bg-white shadow-sm">
        <table className="min-w-full text-sm">
          <thead className="bg-slate-100 text-left text-xs uppercase text-slate-600">
            <tr>
              <th className="px-3 py-2">Quando</th>
              <th className="px-3 py-2">Ação</th>
              <th className="px-3 py-2">Entidade</th>
              <th className="px-3 py-2">ID</th>
              <th className="px-3 py-2">CPF</th>
              <th className="px-3 py-2">Motivo</th>
              <th className="px-3 py-2 text-right">Detalhes</th>
            </tr>
          </thead>
          <tbody>
            {entries.map((e) => (
              <tr key={e.id} className="border-t">
                <td className="px-3 py-2 whitespace-nowrap">{formatDate(e.createdAt)}</td>
                <td className="px-3 py-2">
                  <span className={`rounded px-2 py-0.5 text-xs font-medium ${ACTION_STYLE[e.action] ?? ""}`}>
                    {e.action}
                  </span>
                </td>
                <td className="px-3 py-2">{e.entityType}</td>
                <td className="px-3 py-2 font-mono text-xs">{e.entityId}</td>
                <td className="px-3 py-2 font-mono">{e.cpfMasked ?? "—"}</td>
                <td className="px-3 py-2">{e.reason ?? "—"}</td>
                <td className="px-3 py-2 text-right">
                  <button onClick={() => setSelected(e)}
                          className="text-blue-600 hover:underline">
                    ver
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <p className="border-t bg-slate-50 px-3 py-2 text-xs text-slate-500">
          Registros são imutáveis (REQ-AUD-001) · sem botões de editar/excluir.
        </p>
      </div>

      {selected && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
             onClick={() => setSelected(null)}>
          <div className="max-h-[80vh] w-full max-w-3xl overflow-auto rounded-lg bg-white p-6 shadow-xl"
               onClick={(e) => e.stopPropagation()}>
            <div className="mb-4 flex items-center justify-between">
              <h3 className="text-lg font-semibold">
                {selected.action} · {selected.entityType} · {selected.entityId}
              </h3>
              <button onClick={() => setSelected(null)} className="text-slate-500 hover:text-slate-800">
                fechar
              </button>
            </div>
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              <section>
                <h4 className="mb-1 text-sm font-medium text-slate-600">Estado anterior</h4>
                <pre className="overflow-auto rounded bg-slate-50 p-3 text-xs">
                  {selected.stateBefore ?? "(nenhum)"}
                </pre>
              </section>
              <section>
                <h4 className="mb-1 text-sm font-medium text-slate-600">Estado posterior</h4>
                <pre className="overflow-auto rounded bg-slate-50 p-3 text-xs">
                  {selected.stateAfter ?? "(nenhum)"}
                </pre>
              </section>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
