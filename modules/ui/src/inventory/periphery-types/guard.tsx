import React, { useEffect } from 'react'
import { useNavigate } from 'react-router'
import { useSelector } from 'react-redux'
import { getIsLoading, getKnownEntities } from './selectors'
import { RootState } from './types'
import { composeRoutes, RouteNames } from '../../utils/routes'
import { IsLoadingOr } from '../../utils/wait-loading'

const ToNewPeripheryType = () => {
  const needToNavigate = useSelector((s: RootState) => getKnownEntities(s).length === 0)
  const navigate = useNavigate()
  useEffect(() => {
    if (needToNavigate) {
      navigate(
        composeRoutes(
          RouteNames.base,
          RouteNames.inventory,
          RouteNames.periphery,
          RouteNames.new
        )
      )
    }
  })
  return null
}

export const Guard = () => (
  <IsLoadingOr isLoadingSelector={getIsLoading} whileLoading={null}>
    <ToNewPeripheryType />
  </IsLoadingOr>
)
