export type BeneficiaryStatus = "A" | "S" | "C" | "I" | "D";

export interface Beneficiary {
  id: number;
  cpf: string;
  name: string;
  birthDate: string;
  uf: string | null;
  regionCode: string | null;
  status: BeneficiaryStatus;
  dependents: Dependent[];
}

export interface Dependent {
  id: number;
  name: string;
  birthDate: string;
}

export type ProgramType = "P" | "A" | "T";

export interface SocialProgram {
  id: number;
  code: string;
  name: string;
  type: ProgramType;
  baseValue: string;
  active: boolean;
}

export interface Payment {
  id: number;
  beneficiaryId: number;
  beneficiaryCpf: string;
  programId: number;
  cycle: string;
  grossAmount: string;
  totalDiscount: string;
  thirteenth: string;
  christmasBonus: string;
  netAmount: string;
}

export type AuditAction = "IN" | "AL" | "EX";

export interface AuditEntry {
  id: number;
  entityType: string;
  entityId: string;
  action: AuditAction;
  stateBefore: string | null;
  stateAfter: string | null;
  reason: string | null;
  cpfMasked: string | null;
  createdAt: string;
}
