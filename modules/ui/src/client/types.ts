export type Data<T> = T extends void ? Record<string, never> : { data: T }
export type TransportObj<K extends string, D> = {
  [k in K]: Data<D>
}
