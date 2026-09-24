import { createSelector } from '@reduxjs/toolkit'
import { getKnownEntities as getControllerTypes } from '../../controller-types/selectors'
import { getKnownEntities as getPeripheryTypes } from '../../periphery-types/selectors'
import type { CtlEndpoint, RootState } from '../types'
import type { Controller, ControllerType, ControllerTypeId, PeripheryType, PeripheryTypeId, ProcessingUnit, Selector } from '../../../types'
import { puConnectionToEndpoint } from '../transformations'

type ControllerTypeMap = Record<ControllerTypeId, ControllerType>
const controllerTypeMap = createSelector(
  getControllerTypes,
  (controllerTypes): ControllerTypeMap =>
    controllerTypes.reduce((acc, type) => ({
      ...acc,
      [type.id]: type
    }), {} as ControllerTypeMap)
)

type PeripheryTypeMap = Record<PeripheryTypeId, PeripheryType>
const peripheryTypeMap = createSelector(
  getPeripheryTypes,
  (peripheryTypes): PeripheryTypeMap =>
    peripheryTypes.reduce((acc, type) => ({
      ...acc,
      [type.id]: type
    }), {} as PeripheryTypeMap)
)

export const controllersEndpointsSelector = (
  controllerSelector: (state: RootState) => Controller | undefined
) =>
  createSelector(
    controllerSelector,
    controllerTypeMap,
    peripheryTypeMap,
    (controller, controllerTypes, peripheryTypes): { endpoints: CtlEndpoint[] } => ({
      endpoints: Object.entries(
        controllerTypes[controller?.typeId ?? -1]?.peripheries || {}
      )
        .flatMap(([name, id]) => {
          const type: PeripheryType | undefined = peripheryTypes[id]
          if (type === undefined) return []
          return [{ name, type }]
        })
        .flatMap(({ name, type }) =>
          type.connections.map(connection => ({
            name: `${name} (${connection.name})`,
            units: connection.units,
            type: connection.type,
            direction: connection.direction,
            controller: {
              peripheryTypeName: type.name,
              peripheryName: name,
              controllerId: controller?.id || 0,
              peripheryChannel: connection.name
            }
          }))
        )
        .sort((a, b) => a.name.localeCompare(b.name))
    })
  )

export const processorsEndpointsSelector = (unitSelector: Selector<ProcessingUnit, RootState>) =>
  createSelector(unitSelector, processingUnit => ({
    endpoints: [
      ...(processingUnit?.inbound || []).map(
        puConnectionToEndpoint(processingUnit?.name || '', 'in')
      ),
      ...(processingUnit?.outbound || []).map(
        puConnectionToEndpoint(processingUnit?.name || '', 'out')
      )
    ]
  }))
