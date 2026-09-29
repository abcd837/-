export function getRoleCodes(): string[] {
  const raw = localStorage.getItem('roleCodes') || '[]'
  try {
    return JSON.parse(raw) as string[]
  } catch {
    return []
  }
}

export function hasAnyRole(roles: string[]): boolean {
  const userRoles = getRoleCodes()
  return roles.some((role) => userRoles.includes(role))
}
