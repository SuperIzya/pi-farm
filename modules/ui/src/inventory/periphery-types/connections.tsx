import React from 'react'
import { FormArgs, formMapField, formSaveButton, formTextField, mapSave } from '../form-mixin'
import { getConnection, getNewEntity } from './selectors'
import { deleteConnection, editConnection, saveConnection, setConnectionDirection, setConnectionName, setConnectionType, setConnectionUnits, setLoading } from './actions'
import { createSelector, Dispatch, PayloadAction, Selector } from '@reduxjs/toolkit'
import { PeripheryConnection, PeripheryDirection } from '../../types'
import { GenericList, GenericListProps, getListKey, ListItem } from '../../utils/list-mixin'
import InputLabel from '@mui/material/InputLabel'
import * as styles from './connections.scss'
import Select from '@mui/material/Select'
import MenuItem from '@mui/material/MenuItem'
import { connect } from 'react-redux'
import { Text } from '../../utils/text'
import classNames from 'classnames'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'


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
      <Name size='small'/>
      <Direction />
      <Units size='small' />
      <Types size='small' />
      <SaveButton className={styles.save} />
    </div>  
))

export const connectionListFactory = <T,>(getConnections: Selector<T, PeripheryConnection[] | undefined>, isEditable: boolean = false) => {

  const connector = <T,>(f: (c: PeripheryConnection) => T) => connect(listSelectorFactory(getConnections, f))

  const DirectionText = connector(({ direction }) => ({ direction: direction || '' }))(({ direction }: { direction: string }) => (
    <Text className={classNames(styles.direction, styles[direction])} text={direction} />
  ))

  const NameText = connector(({ name }) => ({ name: name || '' }))(({ name }: { name: string }) => (
      <Text className={styles.connectionName} text={name} />
  ))

  const TypesText = connector(({ type }) => ({ type: type || '' }))(({ type }: { type: string }) => (
    <Text className={styles.connectionType} text={type} />
  ))

  const UnitsText = connector(({ units }) => ({ units: units || '' }))(({ units }: { units: string }) => (
    <Text className={styles.units} text={units} />
  ))

  const mapActions = connect(() => ({}), (dispatch: Dispatch<PayloadAction<number>>, {itemKey}: {itemKey: number}) => ({
    tryDelete: () => dispatch(deleteConnection(itemKey)),
    tryEdit: () => dispatch(editConnection(itemKey))
  }))

  type ButtonsProps = {
    tryDelete: () => void
    tryEdit: () => void
  }

  const ButtonComponent = ({ tryDelete, tryEdit }: ButtonsProps) => (
     <div className={styles.buttons}>
      <div className={styles.editButton} onClick={tryEdit}>
        <EditIcon sx={{fontSize: '18px'}} />
      </div>
      <div className={styles.deleteButton} onClick={tryDelete}>
        <DeleteIcon sx={{fontSize: '18px'}} />
      </div>
    </div>
  )
  
  const Buttons = isEditable ? mapActions(ButtonComponent) : () => (<div/>)
  const ConnectionItem: ListItem = ({ itemKey }) => (
    <>
      <DirectionText itemKey={itemKey} />
      <NameText itemKey={itemKey} />
      <UnitsText itemKey={itemKey} />
      <TypesText itemKey={itemKey} />
      <Buttons itemKey={itemKey} />
    </>
  )

  const List = connect(createSelector(getConnections, (connections) => ({ count: (connections || []).length })))((props: GenericListProps) => (
    <GenericList {...props} />
  ))

  return <G extends Record<string, unknown>>(props: G) => <List 
    {...props}
    Item={ConnectionItem}
    listConfigCss={{ columns: 6 }} />
}

const InnerNewEntityList = connectionListFactory(createSelector(getNewEntity, (entity) => entity?.connections), true)

export const NewEntityConnectionsList = <G extends Record<string, unknown>>(props: G) => (
  <div className={styles.newEntityConnections}>
  <ConnectionForm />
  <InnerNewEntityList containerClassName={classNames(styles.listContainer, styles.editable)} {...props} />
  </div>
);
