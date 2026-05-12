import { useEffect } from 'react'
import { connect, useDispatch } from 'react-redux'
import { useOnReceiveData, useSendCommand } from '../client'
import type { DataNames, ExtractData } from '../client/data'
import type { Creator } from '../client/types'
import type { PayloadAction } from '@reduxjs/toolkit'
import type { CommandName, ProperName } from '../client/commands'

export type RegisterCallbacks = <
  T extends DataNames,
  D extends ExtractData<T> = ExtractData<T>,
  P = D
>(
  dataType: T,
  callback: Creator<D, P>
) => void

type InitProps = {
  isInitialized: boolean
}
export const initFor = <S, C extends CommandName>(
  initCommand: ProperName<C, void>,
  getInitialized: (s: S) => boolean,
  setInitialized: () => PayloadAction,
  setLoading: (loading: boolean) => PayloadAction<boolean>,
  register: (registerCallback: RegisterCallbacks) => void
) =>
  connect((state: S) => ({ isInitialized: getInitialized(state) }))(
    ({ isInitialized }: InitProps) => {
      const dispatch = useDispatch()
      const registerCallback = useOnReceiveData()
      const send = useSendCommand()

      useEffect(() => {
        if (!isInitialized) {
          dispatch(setInitialized())
          register(registerCallback)
          send(initCommand)

          dispatch(setLoading(true))
        }
      }, [])

      return null
    }
  )
