import { Navigate, Route, Routes } from 'react-router-dom'
import { AppLayout } from './components/AppLayout'
import type { NavItem } from './components/AppLayout'
import { ProtectedRoute } from './components/ProtectedRoute'
import AdminHome from './pages/AdminHome'
import Checkout from './pages/Checkout'
import DashboardHome from './pages/DashboardHome'
import FraudAlerts from './pages/FraudAlerts'
import Login from './pages/Login'
import OrderDetail from './pages/OrderDetail'
import Orders from './pages/Orders'
import Refunds from './pages/Refunds'
import Signup from './pages/Signup'
import TransactionDetail from './pages/TransactionDetail'
import Transactions from './pages/Transactions'

const merchantNav: NavItem[] = [
  { to: '/dashboard', label: 'Overview' },
  { to: '/dashboard/orders', label: 'Orders' },
  { to: '/dashboard/transactions', label: 'Transactions' },
  { to: '/dashboard/fraud-alerts', label: 'Fraud alerts' },
  { to: '/dashboard/refunds', label: 'Refunds' },
]

const adminNav: NavItem[] = [{ to: '/admin', label: 'Overview' }]

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="/login" element={<Login />} />
      <Route path="/signup" element={<Signup />} />
      <Route path="/pay/:orderId" element={<Checkout />} />

      <Route
        path="/dashboard"
        element={
          <ProtectedRoute>
            <AppLayout items={merchantNav} subtitle="Merchant" />
          </ProtectedRoute>
        }
      >
        <Route index element={<DashboardHome />} />
        <Route path="orders" element={<Orders />} />
        <Route path="orders/:orderId" element={<OrderDetail />} />
        <Route path="transactions" element={<Transactions />} />
        <Route path="transactions/:paymentId" element={<TransactionDetail />} />
        <Route path="fraud-alerts" element={<FraudAlerts />} />
        <Route path="refunds" element={<Refunds />} />
      </Route>

      <Route
        path="/admin"
        element={
          <ProtectedRoute role="ADMIN">
            <AppLayout items={adminNav} subtitle="Admin" />
          </ProtectedRoute>
        }
      >
        <Route index element={<AdminHome />} />
      </Route>

      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  )
}
