import React from 'react'
import { useSelector } from 'react-redux'
import { Loading } from './loading'

type Props<S> = {
  children: React.ReactNode
  isLoadingSelector: (state: S) => boolean
}

export const WaitLoading = <S,>({ children, isLoadingSelector }: Props<S>) => {
  const isLoading = useSelector(isLoadingSelector)
  return isLoading ? <Loading /> : <>{children}</>
}
