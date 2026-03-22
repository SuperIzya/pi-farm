import { createSelector } from 'reselect'
import type {
  ControllerType,
  PeripheryType,
  Controller,
  IdType,
  PeripheryTypeId,
  ControllerId
} from '../../types'
import { RootState } from './types'

export type SlotSpec = {
  direction: 'in' | 'out'
  expectedType: string
  expectedUnits: string
}

export type PeripheryShort = {
  id: string
  name: string
  description: string
}

export type ControllerShort = {
  id: ControllerId
  name: string
  description: string
}

/**
 * Filter periphery types that are compatible with a slot.
 */
const filterCompatiblePeripheryTypes = (
  allPeripheryTypes: PeripheryType[],
  slot: SlotSpec
): PeripheryType[] => {
  return allPeripheryTypes.filter((pt) => {
    if (pt.type !== slot.expectedType || pt.units !== slot.expectedUnits) {
      return false
    }
    if (slot.direction === 'in') {
      return pt.direction === 'out' || pt.direction === 'both'
    } else {
      return pt.direction === 'in' || pt.direction === 'both'
    }
  })
}

/**
 * Get periphery options for a controller, filtered by slot compatibility.
 */
const getPeripheriesForControllerType = (
  controllerType: ControllerType,
  compatibleTypeIds: Set<PeripheryTypeId>,
  allPeripheryTypes: PeripheryType[]
): PeripheryShort[] =>
  Object.entries(controllerType.peripheries)
    .filter(([_, typeId]) => compatibleTypeIds.has(typeId))
    .map(([peripheryId, typeId]) => {
      const peripheryType = allPeripheryTypes.find((pt) => pt.id === typeId)
      return {
        id: peripheryId,
        name: peripheryType?.name || '?',
        description: peripheryType?.description || ''
      }
    })

/**
 * Selector: controller options (id, name, description only)
 */
export const makeControllerOptionsSelector = (
  getControllers: (state: RootState) => Controller[]
) =>
  createSelector([getControllers], (controllers) =>
    controllers.map((c) => ({
      id: c.id,
      name: c.name,
      description: c.description
    }))
  )

/**
 * Selector: compatible peripheries for a selected controller and slot spec.
 */
export const makeCompatiblePeripheriesSelector = (
  getControllers: (state: RootState) => Controller[],
  getControllerTypes: (state: RootState) => ControllerType[],
  getPeripheryTypes: (state: RootState) => PeripheryType[]
) =>
  createSelector(
    [getControllers, getControllerTypes, getPeripheryTypes],
    (controllers, controllerTypes, peripheryTypes) =>
      // Return a function that takes controllerId and slotSpec
      (controllerId: IdType | null, slotSpec: SlotSpec | null) => {
        if (!controllerId || !slotSpec) return []

        const controller = controllers.find((c) => c.id === controllerId)
        const controllerType = controllerTypes.find((ct) => ct.id === controller?.typeId)
        const compatibleTypes = filterCompatiblePeripheryTypes(peripheryTypes, slotSpec)
        const compatibleTypeIds = new Set(compatibleTypes.map((pt) => pt.id))

        return !controllerType
          ? []
          : getPeripheriesForControllerType(
              controllerType,
              compatibleTypeIds,
              peripheryTypes
            )
      }
  )
