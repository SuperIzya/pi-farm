import { createSelector } from '@reduxjs/toolkit'
import { getAllProcessingUnits } from '../selectors'
import { getKnownEntities as getControllers } from '../../controller/selectors'
import { getKnownEntities as getControllerTypes } from '../../controller-types/selectors'
import { getKnownEntities as getPeripheryTypes } from '../../periphery-types/selectors'
import type { CtlEndpoint, RootState } from '../types'
import type { ControllerId, PeripheryType } from '../../../types'
import { connect } from 'react-redux'
import { puConnectionToEndpoint } from '../listener'

const getControllerId = (_: RootState, { id }: { id: ControllerId }) => id
const getProcessingUnitId = (_: RootState, { unit }: { unit: string }) => unit

export const getProcessingUnitById = () =>
  createSelector(
    getAllProcessingUnits,
    getProcessingUnitId,
    (processingUnits, processingUnitId) => processingUnits[processingUnitId]
  )

const getControllerById = () =>
  createSelector(getControllers, getControllerId, (controllers, controllerId) =>
    controllers.find(controller => controller.id === controllerId)
  )

export const getControllerName = () =>
  createSelector(getControllerById(), controller => ({ name: controller?.name || '' }))

export const getControllerDescription = () =>
  createSelector(getControllerById(), controller => ({
    description: controller?.description || ''
  }))

export const getControllersEndpoints = connect(() =>
  createSelector(
    getControllerById(),
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
)

export const getProcessorName = () =>
  createSelector(getProcessingUnitById(), processingUnit => ({ name: processingUnit?.name || '' }))

export const getProcessorDescription = () =>
  createSelector(getProcessingUnitById(), processingUnit => ({
    description: processingUnit?.description || ''
  }))

export const getProcessorsEndpoints = connect(() =>
  createSelector(getProcessingUnitById(), processingUnit => ({
    endpoints: [
      ...(processingUnit?.inbound || []).map(
        puConnectionToEndpoint(processingUnit?.name || '', 'in')
      ),
      ...(processingUnit?.outbound || []).map(
        puConnectionToEndpoint(processingUnit?.name || '', 'out')
      )
    ]
  }))
)

export const getProcessorHasParams = () =>
  createSelector(getProcessingUnitById(), processingUnit => ({
    hasParams: Object.keys(processingUnit?.paramsSchema ?? {}).length > 0
  }))

export const getSchema = () =>
  createSelector(getProcessingUnitById(), processingUnit => ({
    schema: processingUnit?.paramsSchema ?? {}
  }))
