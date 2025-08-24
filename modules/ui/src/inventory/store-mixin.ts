import type { IdType, InventoryState, NewEntity, WithId } from '../types'
import { PayloadAction } from '@reduxjs/toolkit'

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
  const entity = state.knownEntities[id]
  if (entity === undefined) {
    if (Object.keys(state.knownEntities).length > 0) return false
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
) => InventoryState<T>

type DefaultInventoryActions<T extends WithId, S extends InventoryState<T>> = {
  setLoading: InventoryReducer<T, S, boolean>
  editEntity: InventoryReducer<T, S, number>
  setEntities: InventoryReducer<T, S, T[]>
  setNewEntityCanBeSaved: InventoryReducer<T, S, boolean>
  startNewEntity: InventoryReducer<T, S, void>
  saveNewEntity: InventoryReducer<T, S, void>
  cancelNewEntity: InventoryReducer<T, S, void>
  addNewEntity: InventoryReducer<T, S, T>
}

export const defaultInventoryActions = <T extends WithId, S extends InventoryState<T>>(
  emptyNewEntity: NewEntity<T>
): DefaultInventoryActions<T, S> => ({
  setLoading: (state: S, action: PayloadAction<boolean>) => ({
    ...state,
    isLoading: action.payload
  }),
  editEntity: (state: S, action: PayloadAction<number>) => ({
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
    ...sate
  }),
  addNewEntity: (state: S, action: PayloadAction<T>) => {
    const entity = state.knownEntities[action.payload.id]
    if (entity === undefined) {
      return {
        knownEntities: { [action.payload.id]: action.payload, ...state.knownEntities },
        isLoading: false
      }
    }

    return {
      knownEntities: { ...state.knownEntities, [action.payload.id]: action.payload },
      isLoading: false
    }
  }
})

type DefaultInventorySelectors<T extends WithId, S extends InventoryState<T>> = {
  getKnownEntities: (state: S) => { [id: number]: T }
  getNewEntity: (state: S) => NewEntity<T> | undefined
  getIsLoading: (state: S) => boolean
}

export const defaultInventorySelectors = <
  T extends WithId,
  S extends InventoryState<T>
>(): DefaultInventorySelectors<T, S> => ({
  getKnownEntities: ({ knownEntities }) => knownEntities,
  getNewEntity: ({ newEntity }) => newEntity,
  getIsLoading: ({ isLoading }) => isLoading
})
