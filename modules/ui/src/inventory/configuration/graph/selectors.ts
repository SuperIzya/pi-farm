import { createSelector } from '@reduxjs/toolkit'
import { getAllProcessingUnits } from '../selectors'
import { getKnownEntities as getControllers } from '../../controller/selectors'
import { getKnownEntities as getControllerTypes } from '../../controller-types/selectors'
import { getKnownEntities as getPeripheryTypes } from '../../periphery-types/selectors'
import { RootState } from '../types'
import { ControllerId, CtlAddress, PeripheryDirection, PeripheryType, ProcessorAddress } from '../../../types'
import { connect } from 'react-redux'

const getControllerId = (_: RootState, { id }: { id: ControllerId }) => id
const getProcessingUnitId = (_: RootState, { id }: { id: string }) => id

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
              name,
              peripheryId: connection.name,
              controllerId: controller?.id || 0
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
        ({ name, units, type }) => ({
          name,
          units, 
          type, 
          direction: 'in', 
          processor: { 
            name, 
            processingUnitId: processingUnit?.name || ''
          }
        }) as Endpoint
      ),
      ...(processingUnit?.outbound || []).map(
        ({ name, units, type }) => ({ 
          name, 
          units, 
          type, 
          direction: 'out',
          processor: { 
            name, 
            processingUnitId: processingUnit?.name || ''
          }
        }) as Endpoint
      )
    ]
  }))
)

export type Endpoint = {
  name: string
  units: string
  type: string
  direction: PeripheryDirection
} & ({ controller: CtlAddress } | { processor: ProcessorAddress })
