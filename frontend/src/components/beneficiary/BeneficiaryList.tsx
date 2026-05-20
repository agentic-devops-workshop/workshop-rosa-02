import { maskCpf } from "@/lib/cpf";
import type { Beneficiary } from "@/lib/types";

const STATUS_LABEL: Record<string, string> = {
  A: "Ativo", S: "Suspenso", C: "Cancelado", I: "Inativo", D: "Desligado",
};
const STATUS_COLOR: Record<string, string> = {
  A: "bg-green-100 text-green-800",
  S: "bg-yellow-100 text-yellow-800",
  C: "bg-red-100 text-red-800",
  I: "bg-slate-100 text-slate-700",
  D: "bg-red-100 text-red-800",
};

export function BeneficiaryList({ items }: { items: Beneficiary[] }) {
  if (items.length === 0) {
    return <p className="text-sm text-slate-500">Nenhum beneficiário cadastrado.</p>;
  }
  return (
    <ul className="space-y-2">
      {items.map((b) => (
        <li key={b.id} className="rounded-lg border bg-white p-4 shadow-sm">
          <div className="flex items-start justify-between">
            <div>
              <p className="font-medium">{b.name}</p>
              <p className="text-sm text-slate-500">CPF: {maskCpf(b.cpf)}</p>
              <p className="text-xs text-slate-400">
                UF: {b.uf ?? "—"} · Região: {b.regionCode ?? "—"} · Dependentes: {b.dependents?.length ?? 0}/5
              </p>
            </div>
            <span className={`rounded-full px-2 py-1 text-xs font-medium ${STATUS_COLOR[b.status]}`}>
              {STATUS_LABEL[b.status]}
            </span>
          </div>
          {(b.status === "C" || b.status === "D") && (
            <p className="mt-2 text-xs text-red-600">
              REQ-BEN-004: dependentes bloqueados para status {b.status}
            </p>
          )}
        </li>
      ))}
    </ul>
  );
}
