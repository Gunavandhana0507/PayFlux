export function LoadingSpinner({ fullPage = false }: { fullPage?: boolean }) {
  return (
    <div
      className={
        fullPage ? 'flex min-h-screen items-center justify-center' : 'flex justify-center py-12'
      }
    >
      <div
        className="h-8 w-8 animate-spin rounded-full border-4 border-primary-light border-t-primary"
        aria-label="Loading"
      />
    </div>
  )
}
