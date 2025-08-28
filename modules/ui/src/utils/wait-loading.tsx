import React from 'react'
import { useSelector } from 'react-redux'
import { Loading } from './loading'

type WaitLoadingProps<S> = {
  children: React.ReactNode
  isLoadingSelector: (state: S) => boolean
}

type IsLoadingOrProps<S> = WaitLoadingProps<S> & {
  whileLoading: React.ReactNode | null
}

export const IsLoadingOr = <S,>({
  children,
  isLoadingSelector,
  whileLoading
}: IsLoadingOrProps<S>) => {
  const isLoading = useSelector(isLoadingSelector)
  return isLoading ? whileLoading : children
}

export const WaitLoading = <S,>({ children, isLoadingSelector }: WaitLoadingProps<S>) => (
  <IsLoadingOr isLoadingSelector={isLoadingSelector} whileLoading={<Loading />}>
    {children}
  </IsLoadingOr>
)
