let config: ClientConfig

export function setConfig(c: ClientConfig) {
  config = c
}

export function getConfig(): ClientConfig {
  return config
}

export interface ClientConfig {
  keycloackUrl: string
  keycloackRealm: string
  keycloackClientId: string
}
