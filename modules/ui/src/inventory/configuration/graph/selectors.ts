import { createSelector } from '@reduxjs/toolkit'
import { getKnownEntities as getControllerTypes } from '../../controller-types/selectors'
import { getKnownEntities as getPeripheryTypes } from '../../periphery-types/selectors'
import type { CtlEndpoint, RootState } from '../types'
import type { Controller, PeripheryType, ProcessingUnit, Selector } from '../../../types'
import { puConnectionToEndpoint } from '../listener'

export const controllersEndpointsSelector = (
  controllerSelector: (state: RootState) => Controller
) =>
  createSelector(
    controllerSelector,
    getControllerTypes,
    getPeripheryTypes,
    (controller, controllerTypes, peripheryTypes): { endpoints: CtlEndpoint[] } => ({
      endpoints: Object.entries(
        controllerTypes.find(type => type.id === controller?.typeId)?.peripheries || {}
      )
        .flatMap(([name, id]) => {
          const type: PeripheryType | undefined = peripheryTypes.find(type => type.id === id)
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
              peripjeryChannel: connection.name
            }
          }))
        )
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
