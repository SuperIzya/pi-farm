import { useSelector } from 'react-redux'
import { peripheryTypesSlice } from './store'
import { RootState } from './types'

export const { getKnownEntities, getNewEntity, getIsLoading, getIsInitialized, getConnection } =
  peripheryTypesSlice.selectors

export const getCurrentConnection = (state: RootState) => state.periphery?.newConnection
export const getImage = (obj: { image?: string } | undefined) =>
  obj?.image?.startsWith('data:image/') ? obj.image : (obj?.image && `/${obj.image}`) || ''

export const usePTSelector = useSelector.withTypes<RootState>()
