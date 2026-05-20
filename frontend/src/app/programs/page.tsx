import { api } from "@/lib/api";
import type { SocialProgram } from "@/lib/types";
import { ProgramForm } from "@/components/admin/ProgramForm";
import { ProgramList } from "@/components/admin/ProgramList";
import { EligibilityRuleDisplay } from "@/components/admin/EligibilityRuleDisplay";

export const dynamic = "force-dynamic";

async function fetchPrograms(): Promise<SocialProgram[]> {
  try {
    return await api.get<SocialProgram[]>("/api/v1/admin/programs");
  } catch {
    return [];
  }
}

export default async function ProgramsPage() {
  const list = await fetchPrograms();
  return (
    <div className="grid grid-cols-1 gap-8 lg:grid-cols-2">
      <section>
        <h2 className="mb-4 text-xl font-semibold">Novo Programa Social</h2>
        <ProgramForm />
        <div className="mt-6">
          <EligibilityRuleDisplay />
        </div>
      </section>
      <section>
        <h2 className="mb-4 text-xl font-semibold">Programas cadastrados</h2>
        <ProgramList items={list} />
      </section>
    </div>
  );
}
