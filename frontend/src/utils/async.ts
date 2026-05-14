/* eslint-disable @typescript-eslint/ban-ts-comment */
// @ts-nocheck
export function settled(result, fallback = null) {
  return result.status === 'fulfilled' ? result.value : fallback
}
