import { api } from "@/lib/api";
import type { Payment } from "@/lib/types";
import { CycleGenerationForm } from "@/components/payment/CycleGenerationForm";
import { PaymentList } from "@/components/payment/PaymentList";

export const dynamic = "force-dynamic";

async function fetchPayments(cycle: string): Promise<Payment[]> {
  try {
    return await api.get<Payment[]>(`/api/v1/payments?cycle=${encodeURIComponent(cycle)}`);
  } catch {
    return [];
  }
}

export default async function PaymentsPage({
  searchParams,
}: {
  searchParams: Promise<{ cycle?: string }>;
}) {
  const params = await searchParams;
  const cycle = params.cycle ?? new Date().toISOString().slice(0, 7);
  const items = await fetchPayments(cycle);
  return (
    <div className="space-y-8">
      <section>
        <h2 className="mb-4 text-xl font-semibold">Geração de Ciclo</h2>
        <CycleGenerationForm defaultCycle={cycle} />
      </section>
      <section>
        <h2 className="mb-4 text-xl font-semibold">Pagamentos do ciclo {cycle}</h2>
        <p className="mb-2 text-xs text-slate-500">
          REQ-PAY-005: apenas beneficiários ACTIVE · ordenado por CPF asc
        </p>
        <PaymentList items={items} />
      </section>
    </div>
  );
}
