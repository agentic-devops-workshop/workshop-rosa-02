/** 27 unidades federativas — REQ-BEN-005. */
export const BRAZILIAN_STATES = [
  "AC","AL","AP","AM","BA","CE","DF","ES","GO","MA","MT","MS","MG",
  "PA","PB","PR","PE","PI","RJ","RN","RS","RO","RR","SC","SP","SE","TO",
] as const;

export type Uf = (typeof BRAZILIAN_STATES)[number];

export function isValidUf(uf: string): boolean {
  return (BRAZILIAN_STATES as readonly string[]).includes(uf.toUpperCase());
}
