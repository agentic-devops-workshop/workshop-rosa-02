import { describe, it, expect } from "vitest";
import { isValidCpf, maskCpf, isSpecialCpf, onlyDigits } from "./cpf";

describe("cpf utils", () => {
  describe("maskCpf — REQ-AUD-001", () => {
    it("mascara CPF válido no formato XXX.XXX.NNN-NN", () => {
      expect(maskCpf("52998224725")).toBe("XXX.XXX.247-25");
    });

    it("retorna string vazia para null/undefined", () => {
      expect(maskCpf(null)).toBe("");
      expect(maskCpf(undefined)).toBe("");
    });

    it("retorna entrada original quando não tem 11 dígitos", () => {
      expect(maskCpf("123")).toBe("123");
    });
  });

  describe("isValidCpf — mod-11", () => {
    it("aceita CPF válido pela regra mod-11", () => {
      // covers REQ-BEN-001 (52998224725 é CPF válido pela mod-11)
      expect(isValidCpf("52998224725")).toBe(true);
      expect(isValidCpf("529.982.247-25")).toBe(true);
    });

    it("rejeita CPF com dígito verificador inválido", () => {
      expect(isValidCpf("12345678901")).toBe(false);
    });

    it("rejeita CPF com todos os dígitos iguais", () => {
      expect(isValidCpf("11111111111")).toBe(false);
      expect(isValidCpf("22222222222")).toBe(false);
    });

    it("rejeita entrada com menos de 11 dígitos", () => {
      expect(isValidCpf("123")).toBe(false);
      expect(isValidCpf("")).toBe(false);
      expect(isValidCpf(null)).toBe(false);
    });

    it("aceita CPF com prefixo especial — REQ-BEN-006 bypass", () => {
      // covers REQ-BEN-006: prefixos governamentais ignoram validação documental
      expect(isValidCpf("00012345678")).toBe(true);
      expect(isValidCpf("99912345678")).toBe(true);
      expect(isValidCpf("10012345678")).toBe(true);
    });
  });

  describe("isSpecialCpf — REQ-BEN-006", () => {
    it("identifica prefixos especiais governamentais", () => {
      expect(isSpecialCpf("00012345678")).toBe(true);
      expect(isSpecialCpf("999.123.456-78")).toBe(true);
    });

    it("rejeita prefixos comuns", () => {
      expect(isSpecialCpf("52998224725")).toBe(false);
      expect(isSpecialCpf("12345678901")).toBe(false);
    });
  });

  describe("onlyDigits", () => {
    it("remove caracteres não numéricos", () => {
      expect(onlyDigits("529.982.247-25")).toBe("52998224725");
    });

    it("retorna string vazia para entrada nula", () => {
      expect(onlyDigits(null as unknown as string)).toBe("");
    });
  });
});
