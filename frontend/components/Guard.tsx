'use client';
import { useEffect, useState } from 'react';
import { getToken } from '../lib/api';

export default function Guard({ children }: { children: React.ReactNode }) {
  const [ok, setOk] = useState(false);
  useEffect(() => {
    if (!getToken()) window.location.replace('/login');
    else setOk(true);
  }, []);
  if (!ok) return <div className="boot"><span className="spinner" /></div>;
  return <>{children}</>;
}
