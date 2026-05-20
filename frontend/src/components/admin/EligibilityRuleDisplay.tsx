export function EligibilityRuleDisplay() {
  return (
    <div className="rounded-lg border bg-slate-50 p-4 text-sm">
      <h3 className="mb-2 font-semibold">Regras de Elegibilidade</h3>
      <ul className="space-y-1 text-slate-700">
        <li>
          <strong>P · Previdenciário:</strong> idade mínima de 60 anos (REQ-ADM-002)
        </li>
        <li>
          <strong>T · Trabalho:</strong> idade entre 16 e 65 anos (REQ-ADM-002)
        </li>
        <li>
          <strong>A · Assistencial:</strong> sem restrição etária por este requisito
        </li>
        <li className="mt-2 rounded bg-amber-50 p-2 text-amber-900">
          <strong>Região 99:</strong> aprovação automática, sem validações adicionais (REQ-ADM-003)
        </li>
      </ul>
    </div>
  );
}
