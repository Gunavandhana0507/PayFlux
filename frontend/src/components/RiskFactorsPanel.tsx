import type { FraudAnalysisDto } from '../types/api'
import { formatMoney } from '../lib/format'

export function RiskFactorsPanel({ analysis }: { analysis: FraudAnalysisDto }) {
  const features = analysis.features
  const score = Math.min(1, Math.max(0, Number(analysis.riskScore)))
  const meterColor =
    analysis.riskLevel === 'HIGH'
      ? 'bg-rose-500'
      : analysis.riskLevel === 'MEDIUM'
        ? 'bg-amber-500'
        : 'bg-emerald-500'

  return (
    <div className="space-y-4">
      <div>
        <div className="flex items-center justify-between text-sm">
          <span className="font-medium text-slate-900">
            Risk score {Number(analysis.riskScore).toFixed(2)}
          </span>
          <span className="text-slate-500">{analysis.riskLevel}</span>
        </div>
        <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-slate-200">
          <div className={`h-full ${meterColor}`} style={{ width: `${score * 100}%` }} />
        </div>
      </div>
      <div>
        <h3 className="font-medium text-slate-900">Flagged because:</h3>
        <ul className="mt-2 list-disc space-y-2 pl-5 text-sm text-slate-700">
          {analysis.factors.map((factor) => (
            <li key={factor.code}>
              <span>{factor.description}</span>{' '}
              <span className="rounded-full bg-primary-light/40 px-2 py-0.5 text-xs font-medium text-primary-dark">
                +{Number(factor.weight).toFixed(2)}
              </span>
            </li>
          ))}
        </ul>
      </div>
      <details className="rounded-md border border-slate-200 p-3 text-sm">
        <summary className="cursor-pointer font-medium">Details we looked at</summary>
        <dl className="mt-3 grid gap-3 text-slate-600 sm:grid-cols-2">
          <div>
            <dt>Amount</dt>
            <dd className="font-medium text-slate-900">
              {features ? formatMoney(features.amount) : '—'}
            </dd>
          </div>
          <div>
            <dt>Customer&apos;s usual amount</dt>
            <dd className="font-medium text-slate-900">
              {features?.customerAvgAmount == null
                ? 'No history yet'
                : formatMoney(features.customerAvgAmount)}
            </dd>
          </div>
          <div>
            <dt>Attempts in last 10 min</dt>
            <dd className="font-medium text-slate-900">{features?.attemptsLast10Min ?? '—'}</dd>
          </div>
          <div>
            <dt>Failed attempts in last 10 min</dt>
            <dd className="font-medium text-slate-900">
              {features?.failedAttemptsLast10Min ?? '—'}
            </dd>
          </div>
          <div>
            <dt>New device?</dt>
            <dd className="font-medium text-slate-900">
              {features ? (features.newDevice ? 'Yes' : 'No') : '—'}
            </dd>
          </div>
        </dl>
      </details>
    </div>
  )
}
