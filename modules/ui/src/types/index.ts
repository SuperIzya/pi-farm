export type PeripheryDirection = 'in' | 'out' | 'both'

export type PeripheryType = {
  id: number
  name: string
  description: string
  image: string
  direction: PeripheryDirection
  units: string
}

export type Peripheries = Record<string, number>

export type ControllerType = {
  id: number
  name: string
  description: string
  schema?: string
  code: string
  peripheries: Peripheries
}
