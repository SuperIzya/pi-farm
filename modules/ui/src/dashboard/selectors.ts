import { createSelector } from '@reduxjs/toolkit'
import type { ControllerId } from '../types'
import store, { deepMerge, normalizeChannelKeys } from './store'
import { typedKeys } from '../types'

import { getKnownEntities as getControllers } from '../inventory/controller/selectors'
import { getKnownEntities as getControllerTypes } from '../inventory/controller-types/selectors'
import { getKnownEntities as getPeripheries } from '../inventory/periphery-types/selectors'
import type { ControllerPresentation } from './types'
import type { Presentation } from '../types/presentation'
export const { getValues, getIsInitialized } = store.selectors

export const valuesSelector = (controllerId: ControllerId) => {
  const controllerValues = createSelector(getValues, values => values?.[controllerId])
  return (peripheryName: string, channelName: string) => {
    const pn = peripheryName.toLowerCase()
    const pc = channelName.toLowerCase()
    return createSelector(controllerValues, values => values?.[pn]?.[pc])
  }
}

export const controllerIdsSelector = createSelector(getValues, values =>
  typedKeys(values)
    .filter(key => key !== 'isInitialized')
    .map(key => Number(key))
)

export const getAllControllers = getControllers

export const getControllerPresentation = (id: ControllerId) =>
  createSelector(
    getControllers,
    getControllerTypes,
    getPeripheries,
    (controllers, controllerTypes, peripheries): ControllerPresentation => {
      const controller = controllers.find(controller => controller.id === id)
      if (!controller) return {}

      const controllerType = controllerTypes.find(type => type.id === controller.typeId)
      if (!controllerType) return {}

      const reversePeripheries = Object.entries(controllerType.peripheries).reduce(
        (acc, [key, value]) => ({
          ...acc,
          [value]: [key, ...(acc[value] ?? [])]
        }),
        {} as Record<number, string[]>
      )

      const periphery = peripheries.filter(periphery =>
        reversePeripheries.hasOwnProperty(periphery.id)
      )
      if (periphery.length === 0) return {}

      const presentationMap = periphery.reduce<ControllerPresentation>((acc, p) => {
        const channels = p.connections.reduce<Record<string, Presentation>>(
          (channels, { name, presentation }) =>
            presentation === null
              ? channels
              : {
                  ...channels,
                  [name]: deepMerge(channels[name], presentation)
                },
          {}
        )

        return (reversePeripheries[p.id] ?? []).reduce<ControllerPresentation>(
          (presentation, name) => ({ ...presentation, [name]: channels }),
          acc
        )
      }, {})

      return deepMerge(
        deepMerge(
          normalizeChannelKeys(presentationMap),
          normalizeChannelKeys(controllerType.presentation)
        ),
        normalizeChannelKeys(controller.presentation)
      )
    }
  )
