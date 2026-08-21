import { Card } from '../components/ui/Card'
import { EmptyState } from '../components/ui/Feedback'

/** Stub surface: full cross-merchant admin tooling is a later phase. */
export default function AdminHome() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-semibold text-slate-800">Admin console</h1>
        <p className="text-sm text-slate-500">Platform-wide operations across all merchants.</p>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card title="Merchant onboarding">
          <EmptyState
            title="KYC review queue is not wired up yet"
            description="Merchant approval, KYC document review and account suspension land in a later phase."
          />
        </Card>
        <Card title="Platform risk">
          <EmptyState
            title="Cross-merchant risk monitoring coming soon"
            description="Aggregated fraud alerts, model performance and rule tuning will appear here."
          />
        </Card>
      </div>
    </div>
  )
}
