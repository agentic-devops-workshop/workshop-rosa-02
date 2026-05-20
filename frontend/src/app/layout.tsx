import "./globals.css";
import Link from "next/link";
import type { ReactNode } from "react";

export const metadata = { title: "SIFAP 2.0", description: "Modernização SIFAP" };

const NAV = [
  { href: "/beneficiaries", label: "Beneficiários" },
  { href: "/payments", label: "Pagamentos" },
  { href: "/programs", label: "Programas" },
  { href: "/audit", label: "Auditoria" },
];

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="pt-BR">
      <body>
        <header className="border-b bg-white">
          <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
            <h1 className="text-xl font-bold text-blue-700">SIFAP 2.0</h1>
            <nav className="flex gap-4 text-sm">
              {NAV.map((n) => (
                <Link key={n.href} href={n.href} className="hover:text-blue-600">
                  {n.label}
                </Link>
              ))}
            </nav>
          </div>
        </header>
        <main className="mx-auto max-w-6xl px-6 py-8">{children}</main>
      </body>
    </html>
  );
}
