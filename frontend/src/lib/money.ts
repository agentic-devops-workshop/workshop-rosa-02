/** Formata BigDecimal com 2 casas, sem arredondar — REQ-PAY-004. */
export function formatMoney(value: number | string | null | undefined): string {
  if (value === null || value === undefined || value === "") return "—";
  const num = typeof value === "string" ? parseFloat(value) : value;
  // Trunca, não arredonda (consistente com backend).
  const truncated = Math.floor(num * 100) / 100;
  return truncated.toLocaleString("pt-BR", {
    style: "currency",
    currency: "BRL",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}
