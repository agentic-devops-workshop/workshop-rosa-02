import { maskCpf } from "@/lib/cpf";
import { formatMoney } from "@/lib/money";
import type { Payment } from "@/lib/types";

export function PaymentList({ items }: { items: Payment[] }) {
  if (items.length === 0) {
    return <p className="text-sm text-slate-500">Nenhum pagamento neste ciclo.</p>;
  }
  return (
    <div className="overflow-x-auto rounded-lg border bg-white shadow-sm">
      <table className="w-full text-sm">
        <thead className="bg-slate-100 text-left">
          <tr>
            <th className="px-3 py-2">CPF</th>
            <th className="px-3 py-2">Bruto</th>
            <th className="px-3 py-2" title="REQ-PAY-003">13º</th>
            <th className="px-3 py-2" title="REQ-PAY-003">Abono</th>
            <th className="px-3 py-2" title="REQ-PAY-001/002">Descontos</th>
            <th className="px-3 py-2">Líquido</th>
          </tr>
        </thead>
        <tbody>
          {items.map((p) => (
            <tr key={p.id} className="border-t">
              <td className="px-3 py-2 font-mono">{maskCpf(p.beneficiaryCpf)}</td>
              <td className="px-3 py-2">{formatMoney(p.grossAmount)}</td>
              <td className="px-3 py-2">{formatMoney(p.thirteenth)}</td>
              <td className="px-3 py-2">{formatMoney(p.christmasBonus)}</td>
              <td className="px-3 py-2 text-red-600">- {formatMoney(p.totalDiscount)}</td>
              <td className="px-3 py-2 font-medium">{formatMoney(p.netAmount)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
