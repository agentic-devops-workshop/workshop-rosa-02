import { formatMoney } from "@/lib/money";
import type { SocialProgram } from "@/lib/types";

const TYPE_LABEL: Record<string, string> = { P: "Previdenciário", A: "Assistencial", T: "Trabalho" };

export function ProgramList({ items }: { items: SocialProgram[] }) {
  if (items.length === 0) {
    return <p className="text-sm text-slate-500">Nenhum programa cadastrado.</p>;
  }
  return (
    <ul className="space-y-2">
      {items.map((p) => (
        <li key={p.id} className="rounded-lg border bg-white p-4 shadow-sm">
          <div className="flex items-center justify-between">
            <div>
              <p className="font-medium">
                <span className="font-mono">{p.code}</span> · {p.name}
              </p>
              <p className="text-sm text-slate-500">
                Tipo: {TYPE_LABEL[p.type]} · Valor Base Ajustado: {formatMoney(p.baseValue)}
              </p>
            </div>
            {p.active && (
              <span className="rounded-full bg-green-100 px-2 py-1 text-xs font-medium text-green-800">
                Ativo
              </span>
            )}
          </div>
        </li>
      ))}
    </ul>
  );
}
