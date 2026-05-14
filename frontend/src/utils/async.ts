export function settled<T>(
  result: PromiseSettledResult<T>,
  fallback?: T
): T | undefined {
  return result.status === 'fulfilled'
    ? result.value
    : fallback
}