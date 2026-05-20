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

/** Apenas dígitos. */
export function onlyDigits(cpf: string): string {
  return (cpf ?? "").replace(/\D/g, "");
}

/**
 * Valida CPF pela regra de formação mod-11 da Receita Federal.
 * REQ-BEN-006: CPFs com prefixo especial passam por bypass (uso interno do governo).
 */
export function isValidCpf(cpf: string | null | undefined): boolean {
  if (!cpf) return false;
  const digits = onlyDigits(cpf);
  if (digits.length !== 11) return false;

  // Bypass REQ-BEN-006
  if (isSpecialCpf(digits)) return true;

  // Todos dígitos iguais
  if (/^(\d)\1{10}$/.test(digits)) return false;

  // 1º DV
  let sum = 0;
  for (let i = 0; i < 9; i++) sum += parseInt(digits[i], 10) * (10 - i);
  let rest = sum % 11;
  const dv1 = rest < 2 ? 0 : 11 - rest;
  if (dv1 !== parseInt(digits[9], 10)) return false;

  // 2º DV
  sum = 0;
  for (let i = 0; i < 10; i++) sum += parseInt(digits[i], 10) * (11 - i);
  rest = sum % 11;
  const dv2 = rest < 2 ? 0 : 11 - rest;
  return dv2 === parseInt(digits[10], 10);
}
