import { createSelector } from '@reduxjs/toolkit'
import { getAllProcessingUnits } from '../selectors'
import { getKnownEntities as getControllers } from '../../controller/selectors'
import { getKnownEntities as getControllerTypes } from '../../controller-types/selectors'
import { getKnownEntities as getPeripheryTypes } from '../../periphery-types/selectors'
import { RootState } from '../types'
import { ControllerId, PeripheryDirection, PeripheryType } from '../../../types'
import { connect } from 'react-redux'

const getControllerId = (_: RootState, { id }: { id: ControllerId }) => id
const getProcessingUnitId = (_: RootState, { id }: { id: string }) => id
const getEndpointDirection = (_: RootState, { direction }: { direction: PeripheryDirection }) => direction

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

export const getControllersEndpoints = connect(() =>
  createSelector(
    getControllerById(),
    getControllerTypes,
    getPeripheryTypes,
    (controller, controllerTypes, peripheryTypes) => ({
      endpoints: Object.entries(controllerTypes.find(type => type.id === controller?.typeId)?.peripheries || {})
        .flatMap(([name, id]) => {
          const type: PeripheryType | undefined = peripheryTypes.find(type => type.id === id)
          if (type === undefined) return []
          return [{name, type}]
        })
        .flatMap(({name, type}) => type.connections
          .map(connection => ({
            name: `${name} (${connection.name})`,
            units: connection.units,
            type: connection.type,
            direction: connection.direction
          }))
        )
    })    
  ))

export const getProcessorName = () =>
  createSelector(getProcessingUnitById(), processingUnit => ({ name: processingUnit?.name || '' }))

export const getProcessorDescription = () =>
  createSelector(getProcessingUnitById(), processingUnit => ({
    description: processingUnit?.description || ''
  }))

export const getProcessorsEndpoints = connect(() =>
  createSelector(getProcessingUnitById(), processingUnit => ({
    endpoints: [
      ...(processingUnit?.inbound || []).map(({ name, units, type }) => ({ name, units, type, direction: 'in' } as Endpoint)),
      ...(processingUnit?.outbound || []).map(({ name, units, type }) => ({ name, units, type, direction: 'out' } as Endpoint))
    ]      
  })))

export type Endpoint = {
  name: string
  units: string
  type: string
  direction: PeripheryDirection
}
