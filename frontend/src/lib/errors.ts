import axios from 'axios'
import type { ApiError } from '../types/api'

export function getFriendlyError(error: unknown): {
  message: string
  fieldErrors: Record<string, string>
} {
  if (axios.isAxiosError(error)) {
    const payload = error.response?.data as ApiError | undefined
    if (payload?.message) {
      return { message: payload.message, fieldErrors: payload.fieldErrors ?? {} }
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
