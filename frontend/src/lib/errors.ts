import axios from 'axios'
import type { ApiError } from '../types/api'

export function getFriendlyError(error: unknown): {
  message: string
  fieldErrors: Record<string, string>
} {
  if (axios.isAxiosError(error)) {
    const payload = error.response?.data as ApiError | undefined
    const status = error.response?.status
    if (payload && status && status >= 400 && status < 500) {
      const messages: Record<string, string> = {
        UNAUTHORIZED: 'Please sign in to continue.',
        FORBIDDEN: "You don't have access to this.",
        VALIDATION_ERROR: 'Check the highlighted fields and try again.',
        INVALID_CREDENTIALS: 'The email or password is incorrect.',
        EMAIL_ALREADY_REGISTERED: 'An account with this email already exists.',
        ORDER_NOT_FOUND: "We couldn't find this payment link.",
        PAYMENT_NOT_FOUND: "We couldn't find this payment.",
        ORDER_EXPIRED: 'This payment link has expired.',
        REFUND_NOT_ALLOWED: 'This payment cannot be refunded right now.',
      }
      return {
        message:
          (payload.code && messages[payload.code]) ||
          payload.message ||
          'Something went wrong. Please try again.',
        fieldErrors: payload.fieldErrors ?? {},
      }
    }
    if (!error.response) {
      return {
        message: "Can't reach PayFlux right now. Check your connection and try again.",
        fieldErrors: {},
      }
    }
  }
  return { message: 'Something went wrong. Please try again.', fieldErrors: {} }
}
