import { createContext, useContext, useMemo, useState, useCallback } from 'react';

const ToastContext = createContext(null);

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const addToast = useCallback((message, type = 'info', duration = 2500) => {
    const id = crypto.randomUUID();
    setToasts((prev) => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, duration);
  }, []);

  const value = useMemo(() => ({ addToast }), [addToast]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="fixed bottom-6 inset-x-0 z-50 flex justify-center">
        <div className="space-y-2">
          {toasts.map((toast) => (
            <div
              key={toast.id}
              className={[
                'rounded-xl px-4 py-3 text-sm font-semibold shadow-lg text-white',
                toast.type === 'success'
                  ? 'bg-mint-600'
                  : toast.type === 'error'
                  ? 'bg-red-600'
                  : 'bg-ink/90',
              ].join(' ')}
            >
              {toast.message}
            </div>
          ))}
        </div>
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within ToastProvider');
  return ctx;
}
