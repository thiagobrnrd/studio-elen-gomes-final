import type { Metadata } from 'next';

const site = process.env.NEXT_PUBLIC_SITE_URL
  || (process.env.VERCEL_PROJECT_PRODUCTION_URL ? `https://${process.env.VERCEL_PROJECT_PRODUCTION_URL}` : 'http://localhost:3000');

export const metadata: Metadata = {
  metadataBase: new URL(site),
  title: 'Agende seu horário · Studio Elen Gomes',
  description: 'Escolha o serviço, o dia e o horário. Confirmação pelo WhatsApp.',
  openGraph: {
    title: 'Agende seu horário · Studio Elen Gomes',
    description: 'Escolha o serviço, o dia e o horário em poucos segundos.',
    images: ['/images/login-background.jpg'],
    locale: 'pt_BR',
    type: 'website',
  },
};

export default function AgendarLayout({ children }: { children: React.ReactNode }) {
  return children;
}
