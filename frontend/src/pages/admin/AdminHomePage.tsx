import { EmptyState } from '../../components/ui/EmptyState'
import { PageHeader } from '../../components/ui/PageHeader'
export function AdminHomePage({ title }: { title: string }) { return <><PageHeader title={title} /><EmptyState title="Admin tools are coming in a later phase" /></> }
