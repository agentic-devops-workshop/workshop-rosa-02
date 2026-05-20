import { api } from "@/lib/api";
import type { AuditEntry } from "@/lib/types";
import { AuditSearch } from "@/components/audit/AuditSearch";
import { AuditLog } from "@/components/audit/AuditLog";

export const dynamic = "force-dynamic";

interface PageProps {
  searchParams: { action?: string; entityType?: string };
}

async function fetchAudit(action?: string, entityType?: string): Promise<AuditEntry[]> {
  const params = new URLSearchParams();
  if (action) params.set("action", action);
  if (entityType) params.set("entityType", entityType);
  const qs = params.toString();
  try {
    return await api.get<AuditEntry[]>(`/api/v1/audit${qs ? `?${qs}` : ""}`);
  } catch {
    return [];
  }
}

export default async function AuditPage({ searchParams }: PageProps) {
  const entries = await fetchAudit(searchParams.action, searchParams.entityType);
  return (
    <div className="space-y-6">
      <header>
        <h2 className="text-xl font-semibold">Auditoria · Append-Only</h2>
        <p className="text-sm text-slate-500">
          REQ-AUD-001: registros são somente leitura. REQ-AUD-002: eventos de exclusão (EX) permanecem visíveis.
        </p>
      </header>
      <AuditSearch initial={searchParams} />
      <AuditLog entries={entries} />
    </div>
  );
}
