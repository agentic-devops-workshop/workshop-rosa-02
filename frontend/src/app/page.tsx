export default function HomePage() {
  return (
    <div className="space-y-4">
      <h2 className="text-2xl font-semibold">Bem-vindo ao SIFAP 2.0</h2>
      <p className="text-slate-600">
        Sistema de Fiscalização e Administração de Pagamentos — modernização.
      </p>
      <ul className="list-inside list-disc text-slate-700">
        <li>Beneficiários · REQ-BEN-001 a 006</li>
        <li>Pagamentos · REQ-PAY-001 a 005</li>
        <li>Programas · REQ-ADM-001 a 004</li>
        <li>Auditoria · REQ-AUD-001 e 002</li>
      </ul>
    </div>
  );
}
