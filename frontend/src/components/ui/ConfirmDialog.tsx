import { useState } from 'react'
import { Button } from './Button'
import { Modal } from './Modal'
import { Textarea } from './Input'

export function ConfirmDialog({ open, title, description, onClose, onConfirm, note }: { open: boolean; title: string; description: string; onClose: () => void; onConfirm: (note?: string) => Promise<void>; note?: boolean }) {
  const [value, setValue] = useState('')
  const [loading, setLoading] = useState(false)
  const confirm = async () => { setLoading(true); try { await onConfirm(value || undefined); onClose(); } finally { setLoading(false) } }
  return <Modal open={open} title={title} onClose={onClose}><p className="text-sm text-slate-600">{description}</p>{note && <div className="mt-4"><Textarea label="Note (optional)" value={value} onChange={(event) => setValue(event.target.value)} maxLength={500} /></div>}<div className="mt-6 flex justify-end gap-2"><Button variant="secondary" onClick={onClose}>Cancel</Button><Button loading={loading} onClick={confirm}>Confirm</Button></div></Modal>
}
