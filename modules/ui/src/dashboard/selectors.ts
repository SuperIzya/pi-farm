import { createSelector } from '@reduxjs/toolkit'
import type { ControllerId } from '../types'
import store, { deepMerge } from './store'

import { getKnownEntities as getControllers } from '../inventory/controller/selectors'
import { getKnownEntities as getControllerTypes } from '../inventory/controller-types/selectors'
import { getKnownEntities as getPeripheries } from '../inventory/periphery-types/selectors'
import type { Presentation } from './types'
import type { Definition } from '../types/presentation'

export const { getValues, getIsInitialized } = store.selectors

export const valueSelector = (
  controllerId: ControllerId,
  peripheryName: string,
  channelName: string
) =>
  createSelector(getValues, values => values?.[controllerId]?.[peripheryName]?.[channelName])


export const controllerIdsSelector = createSelector(
  getValues,
  values => Object.keys(values)
)


export const getAllControllers = getControllers

export const getControllerPresentation = (id: ControllerId) =>
  createSelector(
    getControllers,
    getControllerTypes,
    getPeripheries,
    (controllers, controllerTypes, peripheries) => {
      const controller = controllers.find(controller => controller.id === id)
      if(!controller) return {}

      const controllerType = controllerTypes.find(type => type.id === controller.typeId)
      if(!controllerType) return {}

      const reversePeripheries = Object.entries(controllerType.peripheries).reduce((acc, [key, value]) => ({
        ...acc,
        [value]: [...key, ...(acc[value] ?? [])]
      }), {} as Record<number, string[]>)

      const periphery = peripheries.filter(periphery => reversePeripheries.hasOwnProperty(periphery.id))
      if(periphery.length === 0) return {}

      const presentationMap: Presentation = periphery.reduce((acc, p) => {
        const names = new Set(reversePeripheries[p.id] ?? [])

        const presentation = Array.from(names)
          .flatMap<Presentation, string>(name => ({
            [name]: p.connections
              .filter(({ presentation }) => presentation !== null)
              .reduce<Record<string, Definition>>((acc, c) => {
                const m = deepMerge(acc[c.name], c.presentation)
                return m === null ? acc : { 
                ...acc,
                [c.name]: m
              }
            }, {})              
          }))
          .reduce<Presentation>((acc, curr) => ({ ...acc, ...curr }), {})

        return {
          ...acc,
          ...presentation
        }
      }, {})

      return deepMerge(
        deepMerge(presentationMap, controllerType.presentation),
        controller.presentation
      )
    }
  )