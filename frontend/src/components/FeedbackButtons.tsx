import { useState } from 'react'
import { Button } from './ui/Button'
import { ConfirmDialog } from './ui/ConfirmDialog'
import type { MerchantFeedback } from '../types/api'

export function FeedbackButtons({ onFeedback }: { onFeedback: (feedback: MerchantFeedback, note?: string) => Promise<void> }) {
  const [selected, setSelected] = useState<MerchantFeedback | null>(null)
  return <><div className="flex flex-wrap gap-2"><Button size="sm" variant="danger" onClick={() => setSelected('CONFIRMED_FRAUD')}>Confirm fraud</Button><Button size="sm" variant="secondary" onClick={() => setSelected('FALSE_POSITIVE')}>False positive</Button></div><ConfirmDialog open={Boolean(selected)} title={selected === 'CONFIRMED_FRAUD' ? 'Confirm this payment was fraudulent?' : 'Mark this as a false positive?'} description="Your feedback helps improve future fraud detection." note onClose={() => setSelected(null)} onConfirm={(note) => onFeedback(selected!, note)} /></>
}
