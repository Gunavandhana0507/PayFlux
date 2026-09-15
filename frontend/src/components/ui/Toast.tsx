import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'
import { X } from 'lucide-react'

type Toast = { id: number; message: string; type: 'success' | 'error' | 'info' }
const ToastContext = createContext<{ show: (message: string, type?: Toast['type']) => void } | null>(null)

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])
  const value = useMemo(() => ({
    show: (message: string, type: Toast['type'] = 'info') => {
      const id = Date.now()
      setToasts((current) => [...current, { id, message, type }])
      window.setTimeout(() => setToasts((current) => current.filter((toast) => toast.id !== id)), 4000)
    },
  }), [])
  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="fixed bottom-4 right-4 z-[60] flex w-80 max-w-[calc(100vw-2rem)] flex-col gap-2">
        {toasts.map((toast) => <div key={toast.id} className={`flex items-center justify-between gap-3 rounded-md px-4 py-3 text-sm text-white shadow-lg ${toast.type === 'success' ? 'bg-emerald-600' : toast.type === 'error' ? 'bg-rose-600' : 'bg-slate-700'}`}><span>{toast.message}</span><button onClick={() => setToasts((current) => current.filter((item) => item.id !== toast.id))}><X className="h-4 w-4" /></button></div>)}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast() {
  const value = useContext(ToastContext)
  if (!value) throw new Error('ToastProvider is missing')
  return value
}
