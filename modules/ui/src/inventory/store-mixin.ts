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

const buildNewEntity = <Id extends IdType, T extends WithId<Id>, S extends InventoryState<Id, T>>(
  state: S,
  id?: IdType
): NewEntityPart<T> => {
  if (state.newEntity !== undefined && !('id' in state.newEntity))
    return { newEntity: state.newEntity }
  if (id == undefined) return false
  const entity = state.knownEntities.find(e => e.id === id)
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

type InventoryReducer<
  Id extends IdType,
  T extends WithId<Id>,
  S extends InventoryState<Id, T>,
  P
> = (state: S, action: PayloadAction<P>) => S

type DefaultInventoryActions<
  Id extends IdType,
  T extends WithId<Id>,
  S extends InventoryState<Id, T>
> = {
  setLoading: InventoryReducer<Id, T, S, boolean>
  editEntity: InventoryReducer<Id, T, S, IdType>
  setEntities: InventoryReducer<Id, T, S, T[]>
  setNewEntityCanBeSaved: InventoryReducer<Id, T, S, boolean>
  startNewEntity: InventoryReducer<Id, T, S, void>
  saveNewEntity: InventoryReducer<Id, T, S, void>
  cancelNewEntity: InventoryReducer<Id, T, S, void>
  addNewEntity: InventoryReducer<Id, T, S, T>
  setInitialized: InventoryReducer<Id, T, S, void>
}

export const defaultInventoryActions = <
  Id extends IdType,
  T extends WithId<Id>,
  S extends InventoryState<Id, T> = InventoryState<Id, T>
>(
  emptyNewEntity: NewEntity<T>
): DefaultInventoryActions<Id, T, S> => ({
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
    ...(buildNewEntity<Id, T, S>(state, action.payload) || {})
  }),
  setEntities: (state: S, action: PayloadAction<T[]>) => {
    const knownEntities = action.payload
    const newState = { ...state, knownEntities, isLoading: false }
    const newEntity = buildNewEntity<Id, T, S>(newState, newState.editingIndex)
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
    const index = state.knownEntities.findIndex(e => e.id === action.payload.id)
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

type DefaultInventorySelectors<
  Id extends IdType,
  T extends WithId<Id>,
  S extends InventoryState<Id, T>
> = {
  getKnownEntities: (state: S) => T[]
  getNewEntity: (state: S) => NewEntity<T> | undefined
  getIsLoading: (state: S) => boolean
  getIsInitialized: (state: S) => boolean
}

export const defaultInventorySelectors = <
  Id extends IdType,
  T extends WithId<Id>,
  S extends InventoryState<Id, T>
>(
  _: NewEntity<T>
): DefaultInventorySelectors<Id, T, S> => ({
  getKnownEntities: ({ knownEntities }) => knownEntities,
  getNewEntity: ({ newEntity }) => newEntity,
  getIsLoading: ({ isLoading }) => isLoading,
  getIsInitialized: ({ isInitialized }) => isInitialized
})
