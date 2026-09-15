import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { RequireAuth } from './components/RequireAuth'
import { DashboardLayout } from './components/layout/DashboardLayout'
import { PublicLayout } from './components/layout/PublicLayout'
import { AdminLayout } from './components/layout/AdminLayout'
import { LoginPage } from './pages/auth/LoginPage'
import { RegisterPage } from './pages/auth/RegisterPage'
import { PayPage } from './pages/pay/PayPage'
import { OverviewPage } from './pages/dashboard/OverviewPage'
import { OrdersPage } from './pages/dashboard/OrdersPage'
import { OrderDetailPage } from './pages/dashboard/OrderDetailPage'
import { TransactionsPage } from './pages/dashboard/TransactionsPage'
import { TransactionDetailPage } from './pages/dashboard/TransactionDetailPage'
import { FraudAlertsPage } from './pages/dashboard/FraudAlertsPage'
import { RefundsPage } from './pages/dashboard/RefundsPage'
import { AdminHomePage } from './pages/admin/AdminHomePage'
import { NotFoundPage } from './pages/NotFoundPage'
import { ToastProvider } from './components/ui/Toast'

export default function App() {
  return (
    <ToastProvider>
      <AuthProvider>
        <Routes>
          <Route element={<PublicLayout />}>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/pay/:orderId" element={<PayPage />} />
          </Route>
          <Route element={<RequireAuth />}>
            <Route element={<DashboardLayout />}>
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
              <Route path="/dashboard" element={<OverviewPage />} />
              <Route path="/dashboard/orders" element={<OrdersPage />} />
              <Route path="/dashboard/orders/:id" element={<OrderDetailPage />} />
              <Route path="/dashboard/transactions" element={<TransactionsPage />} />
              <Route path="/dashboard/transactions/:id" element={<TransactionDetailPage />} />
              <Route path="/dashboard/fraud-alerts" element={<FraudAlertsPage />} />
              <Route path="/dashboard/refunds" element={<RefundsPage />} />
            </Route>
            <Route element={<AdminLayout />}>
              <Route path="/admin" element={<AdminHomePage title="Admin overview" />} />
              <Route path="/admin/merchants" element={<AdminHomePage title="Merchants" />} />
              <Route path="/admin/transactions" element={<AdminHomePage title="Transactions" />} />
              <Route path="/admin/fraud-alerts" element={<AdminHomePage title="Fraud alerts" />} />
            </Route>
          </Route>
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </AuthProvider>
    </ToastProvider>
  )
}
