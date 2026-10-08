import { createSelector } from '@reduxjs/toolkit'
import type { ControllerId } from '../types'
import store from './store'

const { getValues } = store.selectors

export const valueSelector = (
  controllerId: ControllerId,
  peripheryName: string,
  channelName: string
) =>
  createSelector(getValues, values => values?.[controllerId]?.[peripheryName]?.[channelName]?.value)
