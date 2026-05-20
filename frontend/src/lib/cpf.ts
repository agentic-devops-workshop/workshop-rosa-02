/** Máscara de CPF: XXX.XXX.NNN-NN. REQ-AUD-001. */
export function maskCpf(cpf: string | null | undefined): string {
  if (!cpf) return "";
  const digits = cpf.replace(/\D/g, "");
  if (digits.length !== 11) return cpf;
  return `XXX.XXX.${digits.slice(6, 9)}-${digits.slice(9, 11)}`;
}

/** REQ-BEN-006: prefixos especiais governamentais. */
export const CPF_SPECIAL_PREFIXES = ["000", "001", "002", "010", "011", "099", "100", "999"];

export function isSpecialCpf(cpf: string): boolean {
  const digits = cpf.replace(/\D/g, "");
  return digits.length >= 3 && CPF_SPECIAL_PREFIXES.includes(digits.slice(0, 3));
}
