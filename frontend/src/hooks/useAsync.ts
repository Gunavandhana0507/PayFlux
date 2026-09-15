import { useCallback, useEffect, useState } from 'react'
import { getFriendlyError } from '../lib/errors'

export function useAsync<T>(loader: () => Promise<T>, dependencies: unknown[] = []) {
  const [data, setData] = useState<T | undefined>()
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const reload = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setData(await loader())
    } catch (caught) {
      setError(getFriendlyError(caught).message)
    } finally {
      setLoading(false)
    }
  }, dependencies)

  useEffect(() => {
    void reload()
  }, [reload])

  return { data, loading, error, reload }
}
