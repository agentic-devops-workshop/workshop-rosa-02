import { describe, it, expect } from "vitest";
import { formatMoney } from "./money";

describe("formatMoney — REQ-PAY-004 (truncamento, não arredondamento)", () => {
  it("trunca 100.456 para R$ 100,45 (não arredonda para 100,46)", () => {
    expect(formatMoney(100.456)).toContain("100,45");
  });

  it("trunca 100.999 para R$ 100,99", () => {
    expect(formatMoney(100.999)).toContain("100,99");
  });

  it("formata valores inteiros com 2 casas", () => {
    expect(formatMoney(1000)).toContain("1.000,00");
  });

  it("aceita string numérica", () => {
    expect(formatMoney("250.50")).toContain("250,50");
  });

  it("retorna travessão para null/undefined/vazio", () => {
    expect(formatMoney(null)).toBe("—");
    expect(formatMoney(undefined)).toBe("—");
    expect(formatMoney("")).toBe("—");
  });
});
