import { api } from "@/lib/api";
import type { Beneficiary } from "@/lib/types";
import { BeneficiaryForm } from "@/components/beneficiary/BeneficiaryForm";
import { BeneficiaryList } from "@/components/beneficiary/BeneficiaryList";

export const dynamic = "force-dynamic";

async function fetchBeneficiaries(): Promise<Beneficiary[]> {
  try {
    return await api.get<Beneficiary[]>("/api/v1/beneficiaries");
  } catch {
    return [];
  }
}

export default async function BeneficiariesPage() {
  const list = await fetchBeneficiaries();
  return (
    <div className="grid grid-cols-1 gap-8 md:grid-cols-2">
      <section>
        <h2 className="mb-4 text-xl font-semibold">Novo Beneficiário</h2>
        <BeneficiaryForm />
      </section>
      <section>
        <h2 className="mb-4 text-xl font-semibold">Beneficiários cadastrados</h2>
        <BeneficiaryList items={list} />
      </section>
    </div>
  );
}
