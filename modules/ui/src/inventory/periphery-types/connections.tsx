import React from 'react'
import { FormArgs, formMapField, formTextInput, mapSave } from '../form-mixin'
import { getConnection, getKnownEntities, getNewEntity } from './selectors'
import {
  cancelConnection,
  deleteConnection,
  editConnection,
  saveConnection,
  setConnectionDirection,
  setConnectionName,
  setConnectionType,
  setConnectionUnits
} from './actions'
import { createSelector, Dispatch, PayloadAction } from '@reduxjs/toolkit'
import { PeripheryConnection, PeripheryDirection } from '../../types'
import { GenericList, getListKey, ListItem, WithItemKey } from '../../utils/list-mixin'
import * as styles from './connections.scss'
import Select from '@mui/material/Select'
import MenuItem from '@mui/material/MenuItem'
import { connect, InferableComponentEnhancerWithProps } from 'react-redux'
import { Text } from '../../utils/text'
import classNames from 'classnames'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import InputIcon from '@mui/icons-material/Input'
import OutputIcon from '@mui/icons-material/Output'
import SyncAltIcon from '@mui/icons-material/SyncAlt'
import SaveIcon from '@mui/icons-material/Save'
import CancelIcon from '@mui/icons-material/CancelOutlined'
import { RootState } from './types'

const textField = formTextInput(getConnection)
const mapField = formMapField(getConnection)

const directionStyles: { [key in PeripheryDirection]: string } = {
  in: styles.in,
  out: styles.out,
  both: styles.both
}
const directionIcons: { [key in PeripheryDirection]: React.ReactElement } = {
  in: <InputIcon sx={{ fontSize: '18px' }} />,
  out: <OutputIcon sx={{ fontSize: '18px' }} />,
  both: <SyncAltIcon sx={{ fontSize: '18px' }} />
}

const DirectionIcon = ({ direction }: { direction: PeripheryDirection }) => (
  <div className={classNames(styles.direction, directionStyles[direction])}>
    {directionIcons[direction] || null}
  </div>
)
const Name = textField(setConnectionName, ({ name }) => name || '', 'Name')

const Types = textField(setConnectionType, ({ type }) => type || '', 'Type')

const Units = textField(setConnectionUnits, ({ units }) => units || '', 'Units')

const isPeripheryDirection = (value: string): value is PeripheryDirection =>
  value === 'in' || value === 'out' || value === 'both'

const directionForm = ({ original, save }: FormArgs<PeripheryDirection | undefined>) => (
  <div className={styles.direction}>
    <Select
      labelId={'direction-label'}
      id={'direction'}
      label={'Direction'}
      value={original || ''}
      variant={'standard'}
      size={'small'}
      onChange={e => isPeripheryDirection(e.target.value) && save(e.target.value)}
    >
      {!original && <MenuItem value={''}></MenuItem>}
      <MenuItem value={'in'}>
        <DirectionIcon direction={'in'} />
      </MenuItem>
      <MenuItem value={'out'}>
        <DirectionIcon direction={'out'} />
      </MenuItem>
      <MenuItem value={'both'}>
        <DirectionIcon direction={'both'} />
      </MenuItem>
    </Select>
  </div>
)

const Direction = connect(
  mapField(({ direction }) => direction),
  mapSave(setConnectionDirection)
)(directionForm)
Direction.displayName = 'Direction'

type FormProps = {
  save: () => void
  cancel: () => void
}

export const ConnectionForm = connect(
  () => ({}),
  (dispatch: Dispatch<PayloadAction<void>>) => ({
    save: () => dispatch(saveConnection()),
    cancel: () => dispatch(cancelConnection())
  })
)(({ save, cancel }: FormProps) => (
    <div className={styles.form}>
      <Direction />
      <Name className={styles.name} />
      <Units className={styles.units} />
      <Types className={styles.type} />
      <div className={styles.formButtons}>
        <div className={styles.saveButton} onClick={save}>
          <SaveIcon />
        </div>
        <div className={styles.cancelButton} onClick={cancel}>
          <CancelIcon />
        </div>
      </div>
    </div>
  )
)

type SelP<R, P> = (state: RootState, args: P) => R | undefined

export const connectionListFactory = <P extends object = {}>(
  getConnections: SelP<PeripheryConnection[], P>,
  isEditable: boolean = false
): ((props: P) => React.JSX.Element) => {

  type OnlyConnectionKey = { connectionKey: number }
  type ConnectionKey =  P & OnlyConnectionKey
  type ListItemProps = { original: P }
  type ListProps = ListItemProps & {
    count: number
  }

  const getConnectionKey = <S,>(_: S, { connectionKey }: ConnectionKey) => connectionKey  

  const selector =
    <R extends object>(
      f: (c: PeripheryConnection | undefined) => R
    ): (() => SelP<R, ConnectionKey>) =>
    () =>
      createSelector(getConnections, getConnectionKey, (connections, itemKey) =>
        f(itemKey === undefined ? undefined : connections?.[itemKey])
      )

  const connector = <A extends object>(f: (c: PeripheryConnection | undefined) => A): InferableComponentEnhancerWithProps<A, ConnectionKey> =>
    connect(selector(f))  

  const DirectionText = connector(p => ({ direction: (p?.direction || '') as PeripheryDirection }))(
    DirectionIcon
  )

  const NameText = connector(p => ({ text: p?.name || '', className: styles.name }))(Text)

  const TypesText = connector(p => ({ text: p?.type || '', className: styles.type }))(Text)
 
  const UnitsText = connector(p => ({ text: p?.units || '', className: styles.units }))(Text)

  const mapActions = connect(
    () => ({}),
    (dispatch: Dispatch<PayloadAction<number>>, { connectionKey }: OnlyConnectionKey) => ({
      tryDelete: () => dispatch(deleteConnection(connectionKey)),
      tryEdit: () => dispatch(editConnection(connectionKey))
    })
  )

  type ButtonsProps = {
    tryDelete: () => void
    tryEdit: () => void
  }

  const ButtonComponent = ({ tryDelete, tryEdit }: ButtonsProps) => (
    <div className={styles.buttons}>
      <div className={styles.editButton} onClick={tryEdit}>
        <EditIcon sx={{ fontSize: '18px' }} />
      </div>
      <div className={styles.deleteButton} onClick={tryDelete}>
        <DeleteIcon sx={{ fontSize: '18px' }} />
      </div>
    </div>
  )

  const Buttons = isEditable ? mapActions(ButtonComponent) : () => <div />
  const ConnectionItem: ListItem<ListItemProps> = ({ itemKey, original }) => (
    <>
      <DirectionText {...original} connectionKey={itemKey} />
      <NameText {...original} connectionKey={itemKey} />
      <UnitsText {...original} connectionKey={itemKey} />
      <TypesText {...original} connectionKey={itemKey} />
      <Buttons connectionKey={itemKey} />
    </>
  )

  const mapCount = connect(() => createSelector(getConnections, connections => ({ count: connections?.length || 0 })))
  

  const List: (props: ListItemProps & P) => (React.ReactNode | Promise<React.ReactNode>) = mapCount(({ count, original }: ListProps) => (
    <GenericList
      original={original}
      Item={ConnectionItem}
      count={count}
      listConfigCss={{ columns: isEditable ? 6 : 5 }}
      containerClassName={classNames(styles.listContainer, isEditable && styles.editable)}
    />
  ))  

  return (props: P) => <List {...props} original={props} />
}

const fromListSelector: SelP<PeripheryConnection[], WithItemKey> = createSelector(
  getKnownEntities,
  getListKey,
  (entities, key) => key === undefined ? [] : entities?.[key].connections || []
)

export const ConnectionsList = connectionListFactory(fromListSelector)

const fromNewEntityListSelector: SelP<PeripheryConnection[], {}> = createSelector(getNewEntity, entity => entity?.connections)

const InnerNewEntityList = connectionListFactory(fromNewEntityListSelector, true)

export const NewEntityConnectionsList = () => (
  <div className={styles.newEntityConnections}>
    <ConnectionForm />
    <InnerNewEntityList />
  </div>
)
