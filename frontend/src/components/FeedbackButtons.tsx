import { useState } from 'react'
import { Button } from './ui/Button'
import { ConfirmDialog } from './ui/ConfirmDialog'
import type { MerchantFeedback } from '../types/api'

export function FeedbackButtons({
  onFeedback,
  feedback,
  feedbackAt,
  feedbackNote,
}: {
  onFeedback: (feedback: MerchantFeedback, note?: string) => Promise<void>
  feedback?: MerchantFeedback | null
  feedbackAt?: string | null
  feedbackNote?: string | null
}) {
  const [selected, setSelected] = useState<MerchantFeedback | null>(null)
  const [changing, setChanging] = useState(false)
  if (feedback && !changing) {
    const confirmed = feedback === 'CONFIRMED_FRAUD'
    return (
      <div className="rounded-md bg-primary-light/20 p-4 text-sm text-primary-dark">
        <p className="font-medium">
          You marked this as {confirmed ? 'Confirmed fraud' : 'Not fraud (false positive)'} on{' '}
          {feedbackAt ? new Date(feedbackAt).toLocaleDateString('en-IN') : 'a previous date'}
        </p>
        {feedbackNote && <p className="mt-2 text-slate-700">Note: {feedbackNote}</p>}
        <p className="mt-2">Thanks — this helps improve future fraud detection</p>
        <button
          type="button"
          className="mt-3 font-medium underline"
          onClick={() => setChanging(true)}
        >
          Change answer
        </button>
      </div>
    )
  }
  return (
    <>
      <div className="flex flex-wrap gap-2">
        <Button size="sm" variant="danger" onClick={() => setSelected('CONFIRMED_FRAUD')}>
          Confirm fraud
        </Button>
        <Button size="sm" variant="secondary" onClick={() => setSelected('FALSE_POSITIVE')}>
          Mark as false positive
        </Button>
      </div>
      <ConfirmDialog
        open={Boolean(selected)}
        title={
          selected === 'CONFIRMED_FRAUD'
            ? 'Confirm this payment was fraudulent?'
            : 'Mark this as a false positive?'
        }
        description="Your feedback helps improve future fraud detection."
        note
        onClose={() => setSelected(null)}
        onConfirm={(note) => onFeedback(selected!, note)}
      />
    </>
  )
}
