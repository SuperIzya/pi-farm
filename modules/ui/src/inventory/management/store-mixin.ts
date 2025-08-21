import { InventoryState, NewType, WithId } from './types'
import { PayloadAction } from '@reduxjs/toolkit'

type NewTypePart<T> =
  | {
      newType: NewType<T>
      editingIndex: number
    }
  | {
      editingIndex: number
    }
  | false

const buildNewType = <T extends WithId, S extends InventoryState<T>>(
  state: S,
  id?: number
): NewTypePart<T> => {
  if (id == undefined) return false
  const index = state.knownTypes.findIndex((t) => t.id === id)
  if (index < 0) {
    if (state.knownTypes.length > 0) return false
    return { editingIndex: id }
  }
  return {
    newType: {
      ...state.knownTypes[index],
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
  editType: InventoryReducer<T, S, number>
  setTypes: InventoryReducer<T, S, T[]>
  setNewTypeCanBeSaved: InventoryReducer<T, S, boolean>
  startNewType: InventoryReducer<T, S, void>
  saveNewType: InventoryReducer<T, S, void>
  cancelNewType: InventoryReducer<T, S, void>
  addNewType: InventoryReducer<T, S, T>
}

export const defaultInventoryActions = <T extends WithId, S extends InventoryState<T>>(
  emptyNewType: NewType<T>
): DefaultInventoryActions<T, S> => ({
  setLoading: (state: S, action: PayloadAction<boolean>) => ({
    ...state,
    isLoading: action.payload
  }),
  editType: (state: S, action: PayloadAction<number>) => ({
    ...state,
    ...(buildNewType<T, S>(state, action.payload) || {})
  }),
  setTypes: (state: S, action: PayloadAction<T[]>) => ({
    knownTypes: action.payload,
    isLoading: false,
    ...(buildNewType<T, S>(state, state.editingIndex) || {})
  }),
  setNewTypeCanBeSaved: (state: S, action: PayloadAction<boolean>) => ({
    ...state,
    newType: {
      ...(state.newType || {}),
      canBeSaved: action.payload
    }
  }),
  startNewType: (state: S) => ({
    ...state,
    newType: emptyNewType,
    editingIndex: undefined
  }),
  saveNewType: (state: S) => state,
  cancelNewType: (sate: S) => ({
    ...sate
  }),
  addNewType: (state: S, action: PayloadAction<T>) => {
    const index = state.knownTypes.findIndex((t) => t.id === action.payload.id)
    if (index < 0) {
      return {
        knownTypes: [action.payload, ...state.knownTypes],
        isLoading: false
      }
    }

    const before = state.knownTypes.slice(0, index)
    const after = state.knownTypes.slice(index + 1)
    return {
      knownTypes: [...before, action.payload, ...after],
      isLoading: false
    }
  }
})

type DefaultInventorySelectors<T extends WithId, S extends InventoryState<T>> = {
  getKnownTypes: (state: S) => T[]
  getNewType: (state: S) => NewType<T> | undefined
  getIsLoading: (state: S) => boolean
}

export const defaultInventorySelectors = <
  T extends WithId,
  S extends InventoryState<T>
>(): DefaultInventorySelectors<T, S> => ({
  getKnownTypes: ({ knownTypes }) => knownTypes,
  getNewType: ({ newType }) => newType,
  getIsLoading: ({ isLoading }) => isLoading
})
