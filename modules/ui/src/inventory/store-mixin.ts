import type { IdType, InventoryState, NewEntity, WithId } from '../types'
import { PayloadAction } from '@reduxjs/toolkit'

export type NewEntityPayload<T> = PayloadAction<T | undefined>

type NewEntityPart<T> =
  | {
      newEntity: NewEntity<T>
      editingId: IdType
    }
  | { editingId: IdType }
  | { newEntity: NewEntity<T> }
  | false

const buildNewEntity = <T extends WithId, S extends InventoryState<T>>(
  state: S,
  id?: IdType
): NewEntityPart<T> => {
  if (state.newEntity !== undefined && !('id' in state.newEntity))
    return { newEntity: state.newEntity }
  if (id == undefined) return false
  const entity = state.knownEntities.find((e) => e.id === id)
  if (entity === undefined) {
    if (state.knownEntities.length > 0) return false
    return { editingId: id }
  }
  return {
    newEntity: {
      ...entity,
      canBeSaved: false
    },
    editingId: entity.id
  }
}

type InventoryReducer<T extends WithId, S extends InventoryState<T>, P> = (
  state: S,
  action: PayloadAction<P>
) => S

type DefaultInventoryActions<T extends WithId, S extends InventoryState<T>> = {
  setLoading: InventoryReducer<T, S, boolean>
  editEntity: InventoryReducer<T, S, IdType>
  setEntities: InventoryReducer<T, S, T[]>
  setNewEntityCanBeSaved: InventoryReducer<T, S, boolean>
  startNewEntity: InventoryReducer<T, S, void>
  saveNewEntity: InventoryReducer<T, S, void>
  cancelNewEntity: InventoryReducer<T, S, void>
  addNewEntity: InventoryReducer<T, S, T>
  setInitialized: InventoryReducer<T, S, void>
}

export const defaultInventoryActions = <
  T extends WithId,
  S extends InventoryState<T> = InventoryState<T>
>(
  emptyNewEntity: NewEntity<T>
): DefaultInventoryActions<T, S> => ({
  setLoading: (state: S, action: PayloadAction<boolean>) => ({
    ...state,
    isLoading: action.payload
  }),
  setInitialized: (state: S) => ({
    ...state,
    isInitialized: true
  }),
  editEntity: (state: S, action: PayloadAction<IdType>) => ({
    ...state,
    ...(buildNewEntity<T, S>(state, action.payload) || {})
  }),
  setEntities: (state: S, action: PayloadAction<T[]>) => {
    const knownEntities = action.payload
    const newState = { ...state, knownEntities, isLoading: false }
    const newEntity = buildNewEntity<T, S>(newState, newState.editingIndex)
    return {
      ...newState,
      ...newEntity
    }
  },
  setNewEntityCanBeSaved: (state: S, action: PayloadAction<boolean>) => ({
    ...state,
    newEntity: {
      ...(state.newEntity || {}),
      canBeSaved: action.payload
    }
  }),
  startNewEntity: (state: S) => ({
    ...state,
    newEntity: emptyNewEntity,
    editingId: undefined
  }),
  saveNewEntity: (state: S) => state,
  cancelNewEntity: (sate: S) => ({
    ...sate,
    newEntity: undefined,
    editingId: undefined
  }),
  addNewEntity: (state: S, action: PayloadAction<T>) => {
    const index = state.knownEntities.findIndex((e) => e.id === action.payload.id)
    if (index === -1) {
      return {
        ...state,
        knownEntities: [action.payload, ...state.knownEntities],
        isLoading: false
      }
    }

    const before = state.knownEntities.slice(0, index)
    const after = state.knownEntities.slice(index + 1)
    return {
      ...state,
      knownEntities: [...before, action.payload, ...after],
      isLoading: false
    }
  }
})

type DefaultInventorySelectors<T extends WithId, S extends InventoryState<T>> = {
  getKnownEntities: (state: S) => T[]
  getNewEntity: (state: S) => NewEntity<T> | undefined
  getIsLoading: (state: S) => boolean
  getIsInitialized: (state: S) => boolean
}

export const defaultInventorySelectors = <
  T extends WithId,
  S extends InventoryState<T> = InventoryState<T>
>(
  _: NewEntity<T>
): DefaultInventorySelectors<T, S> => ({
  getKnownEntities: ({ knownEntities }) => knownEntities,
  getNewEntity: ({ newEntity }) => newEntity,
  getIsLoading: ({ isLoading }) => isLoading,
  getIsInitialized: ({ isInitialized }) => isInitialized
})
