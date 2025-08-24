import { InventoryState, NewEntity, WithId } from '../types'
import { PayloadAction } from '@reduxjs/toolkit'

type NewEntityPart<T> =
  | {
      newEntity: NewEntity<T>
      editingIndex: number
    }
  | { editingIndex: number }
  | { newEntity: NewEntity<T> }
  | false

const buildNewEntity = <T extends WithId, S extends InventoryState<T>>(
  state: S,
  id?: number
): NewEntityPart<T> => {
  if (state.newEntity !== undefined && !('id' in state.newEntity))
    return { newEntity: state.newEntity }
  if (id == undefined) return false
  const index = state.knownEntities.findIndex((t) => t.id === id)
  if (index < 0) {
    if (state.knownEntities.length > 0) return false
    return { editingIndex: id }
  }
  return {
    newEntity: {
      ...state.knownEntities[index],
      canBeSaved: false
    },
    editingIndex: index
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
    editingIndex: undefined
  }),
  saveNewEntity: (state: S) => state,
  cancelNewEntity: (sate: S) => ({
    ...sate
  }),
  addNewEntity: (state: S, action: PayloadAction<T>) => {
    const index = state.knownEntities.findIndex((t) => t.id === action.payload.id)
    if (index < 0) {
      return {
        knownEntities: [action.payload, ...state.knownEntities],
        isLoading: false
      }
    }

    const before = state.knownEntities.slice(0, index)
    const after = state.knownEntities.slice(index + 1)
    return {
      knownEntities: [...before, action.payload, ...after],
      isLoading: false
    }
  }
})

type DefaultInventorySelectors<T extends WithId, S extends InventoryState<T>> = {
  getKnownEntities: (state: S) => T[]
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
