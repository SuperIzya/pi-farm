import { createSelector } from '@reduxjs/toolkit'
import { getAllProcessingUnits } from '../selectors'
import { getKnownEntities as getControllers } from '../../controller/selectors'
import { getKnownEntities as getControllerTypes } from '../../controller-types/selectors'
import { getKnownEntities as getPeripheryTypes } from '../../periphery-types/selectors'
import { RootState } from '../types'
import { ControllerId, PeripheryDirection } from '../../../types'

const getControllerId = (_: RootState, { controllerId }: { controllerId: ControllerId }) =>
  controllerId
const getProcessingUnitId = (_: RootState, { processingUnitId }: { processingUnitId: string }) =>
  processingUnitId
const getEndpointDirection = (_: RootState, { direction }: { direction: PeripheryDirection }) =>
  direction

const getProcessingUnitById = () =>
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

export const getControllersEndpoints = () =>
  createSelector(
    getControllerById(),
    getControllerTypes,
    getPeripheryTypes,
    getEndpointDirection,
    (controller, controllerTypes, peripheryTypes, direction) => {
      const controllerType = controllerTypes.find(type => type.id === controller?.typeId)
      const peripherySet = new Set(Object.values(controllerType?.peripheries || {}))
      const peripheryType = peripheryTypes.find(type => peripherySet.has(type.id))
      return {
        endpoints: (peripheryType?.connections || [])
          .filter(connection => connection.direction === direction)
          .map(({ name, units, type }) => ({ name, units, type }))
      }
    }
  )

export const getProcessorName = () =>
  createSelector(getProcessingUnitById(), processingUnit => ({ name: processingUnit?.name || '' }))

export const getProcessorDescription = () =>
  createSelector(getProcessingUnitById(), processingUnit => ({
    description: processingUnit?.description || ''
  }))

export const getProcessorsEndpoints = () =>
  createSelector(getProcessingUnitById(), getEndpointDirection, (processingUnit, direction) => ({
    endpoints:
      (direction === 'in' ? processingUnit?.inbound : processingUnit?.outbound)?.map(
        ({ name, units, type }) => ({ name, units, type })
      ) || []
  }))

export type Endpoint = {
  name: string
  units: string
  type: string
}
