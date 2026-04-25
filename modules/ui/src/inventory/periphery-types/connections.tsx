import React from 'react'
import { FormArgs, formMapField, formSaveButton, formTextField, mapSave } from '../form-mixin'
import { getConnection, getNewEntity } from './selectors'
import { saveConnection, setConnectionDirection, setConnectionName, setConnectionType, setConnectionUnits, setLoading } from './actions'
import { NewConnection, RootState } from './types'
import { createSelector, Selector } from '@reduxjs/toolkit'
import { PeripheryConnection, PeripheryDirection } from '../../types'
import { GenericList, GenericListProps, getListKey, ListItem } from '../../utils/list-mixin'
import InputLabel from '@mui/material/InputLabel'
import * as styles from './connections.scss'
import Select from '@mui/material/Select'
import MenuItem from '@mui/material/MenuItem'
import { connect } from 'react-redux'
import { Text } from '../../utils/text'


const  textField = formTextField(getConnection)
const   mapField = formMapField(getConnection)
const   SaveButton = formSaveButton(getConnection, saveConnection, setLoading)
const   listSelectorFactory = <T,U>(getConnections: (state: U) => PeripheryConnection[] | undefined, f: (c: PeripheryConnection) => T) =>
   () => createSelector([getConnections, getListKey], (connections, itemKey) => {
      const connection = connections?.[itemKey]
      return connection ? f(connection) : undefined
    })

const Name = textField(setConnectionName, ({ name }) => name || '', 'Name')

const Types = textField(setConnectionType, ({ type }) => type || '', 'Type')

const Units = textField(setConnectionUnits, ({ units }) => units || '', 'Units')

const isPeripheryDirection = (value: string): value is PeripheryDirection =>
  value === 'in' || value === 'out' || value === 'both'

const directionForm = ({ original, save }: FormArgs<PeripheryDirection | undefined>) => (
  <div className={styles.direction}>
    <InputLabel id="direction-label">Direction</InputLabel>
    <Select
      labelId={'direction-label'}
      id={'direction'}
      label={'Direction'}
      value={original || ''}
      onChange={(e) => isPeripheryDirection(e.target.value) && save(e.target.value)}
    >
      <MenuItem value={''}>Select direction</MenuItem>
      <MenuItem value={'in'}>Into controller</MenuItem>
      <MenuItem value={'out'}>Out of controller</MenuItem>
      <MenuItem value={'both'}>Both</MenuItem>
    </Select>
  </div>
)

const Direction = connect(
  mapField(({ direction }) => direction),
  mapSave(setConnectionDirection)
)(directionForm)
Direction.displayName = 'Direction'

export const ConnectionForm = connect(
  getConnection,
  (conn) => ({ show: !!conn })
)(({ show }: {show: boolean}) => !show ? null : (
    <div className={styles.form}>
      <Name/>
      <Direction />
      <Units />
      <Types />
      <SaveButton className={styles.save} />
    </div>  
))

export const connectionListFactory = <T,>(getConnections: Selector<T, PeripheryConnection[] | undefined>) => {

  const DirectionText = connect(listSelectorFactory(getConnections, ({ direction }) => ({ direction: direction || '' })))(({ direction }: { direction: string }) => (
    <Text className={styles.direction} text={direction} />
  ))

  const NameText = connect(listSelectorFactory(getConnections, ({ name }) => ({ name: name || '' })))(({ name }: { name: string }) => (
      <Text className={styles.connectionName} text={name} />
  ))

  const TypesText = connect(listSelectorFactory(getConnections, ({ type }) => ({ type: type || '' })))(({ type }: { type: string }) => (
    <Text className={styles.connectionType} text={type} />
  ))

  const UnitsText = connect(listSelectorFactory(getConnections, ({ units }) => ({ units: units || '' })))(({ units }: { units: string }) => (
    <Text className={styles.units} text={units} />
  ))

  const ConnectionItem: ListItem = ({ itemKey }) => (
    <div className={styles.listContainer}>
      <DirectionText itemKey={itemKey} />
      <NameText itemKey={itemKey} />
      <TypesText itemKey={itemKey} />
      <UnitsText itemKey={itemKey} />
    </div>
  )

  const List = connect(createSelector(getConnections, (connections) => ({ count: (connections || []).length })))((props: GenericListProps) => (
    <GenericList {...props} />
  ))

  return <G extends Record<string, unknown>>(props: G) => <List {...props} Item={ConnectionItem} listConfigCss={{ columns: 1 }} />
}

export const NewEntityConnectionsList = connectionListFactory(createSelector(getNewEntity, (entity) => entity?.connections))
