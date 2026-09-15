import { Link, useParams } from 'react-router-dom'
import { fraudApi } from '../../api/fraud'
import { paymentsApi } from '../../api/payments'
import { Breadcrumbs } from '../../components/ui/Breadcrumbs'
import { Button } from '../../components/ui/Button'
import { LoadingSpinner } from '../../components/ui/LoadingSpinner'
import { PageHeader } from '../../components/ui/PageHeader'
import { useToast } from '../../components/ui/Toast'
import { useAsync } from '../../hooks/useAsync'
import {
  RefundsCard,
  RiskAssessmentCard,
  SummaryCard,
  TimelineCard,
} from './TransactionDetailCards'

export function TransactionDetailPage() {
  const { id = '' } = useParams()
  const toast = useToast()
  const result = useAsync(() => paymentsApi.detail(id).then((response) => response.data), [id])
  const reload = result.reload

  if (result.loading) return <LoadingSpinner />
  if (result.error || !result.data) {
    return (
      <div className="card text-center">
        <p className="text-sm text-rose-600">{result.error || 'Transaction unavailable'}</p>
        <Button className="mt-4" onClick={result.reload}>
          Try again
        </Button>
      </div>
    )
  }

  const detail = result.data
  const submitFeedback = async (feedback: 'CONFIRMED_FRAUD' | 'FALSE_POSITIVE', note?: string) => {
    if (!detail.fraudAnalysis) return
    await fraudApi.feedback(detail.fraudAnalysis.id, { feedback, note })
    toast.show('Thanks — this helps improve future fraud detection', 'success')
    await result.reload()
  }

  return (
    <>
      <Breadcrumbs
        items={[
          {
            label: 'Transactions',
            element: (
              <Link
                className="hover:text-primary-dark hover:underline"
                to="/dashboard/transactions"
              >
                Transactions
              </Link>
            ),
          },
          { label: detail.payment.id },
        ]}
      />
      <PageHeader title={detail.payment.id} subtitle={`Order ${detail.order.id}`} />
      <div className="grid gap-6 lg:grid-cols-2">
        <SummaryCard detail={detail} />
        <RiskAssessmentCard detail={detail} onFeedback={submitFeedback} />
        <RefundsCard detail={detail} onReload={reload} />
        <TimelineCard detail={detail} />
      </div>
    </>
  )
}
