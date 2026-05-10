import { PayloadAction } from '@reduxjs/toolkit'

export type Data<T> = T extends void ? Record<string, never> : { data: T }
export type TransportObj<K extends string, D> = {
  [k in K]: Data<D>
}

export type Creator<D, P = D> = (arg: D) => PayloadAction<P>

export type PartialMessage = {
  data: string
  totalCount: number
  id: string
  index: number
}
