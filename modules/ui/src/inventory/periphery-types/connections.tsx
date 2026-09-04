import React from 'react'
import { FormArgs, formInput, formTextInput } from '../form-mixin'
import { getConnection, getKnownEntities, getNewEntity, usePTSelector } from './selectors'
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
import { bindActionCreators } from '@reduxjs/toolkit'
import {
  PeripheryConnection,
  FlowDirection,
  FieldType,
  fieldTypes,
  flowDirections
} from '../../types'
import { GenericList, ListItem, WithItemKey } from '../../utils/list-mixin'
import * as styles from './connections.scss'
import Select from '@mui/material/Select'
import MenuItem from '@mui/material/MenuItem'
import { useDispatch, useSelector } from 'react-redux'
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
import { InputProps } from '@mui/material/Input'
import { getAppConfiguration } from '../../store/root-store'

const textField = formTextInput(getConnection)

const directionStyles: Record<FlowDirection, string> = {
  in: styles.in,
  out: styles.out,
  both: styles.both
}

const directionIcons: Record<FlowDirection, React.ReactElement> = {
  in: <InputIcon sx={{ fontSize: '18px' }} />,
  out: <OutputIcon sx={{ fontSize: '18px' }} />,
  both: <SyncAltIcon sx={{ fontSize: '18px' }} />
}

const DirectionIcon = ({ direction }: { direction: FlowDirection }) => (
  <div className={classNames(styles.direction, directionStyles[direction])}>
    {directionIcons[direction] || null}
  </div>
)
const Name = textField('Name')(setConnectionName, ({ name }) => name || '')

const Types = formInput(
  getConnection,
  setConnectionType,
  ({ type }) => type,
  (args: FormArgs<FieldType | undefined, InputProps>) => (
    <Select
      label={'Type'}
      variant={'standard'}
      size={'small'}
      value={args.original || ''}
      onChange={e => args.save(e.target.value as FieldType)}
    >
      {!args.original && <MenuItem value={''}></MenuItem>}
      {fieldTypes.map((type, idx) => (
        <MenuItem key={idx} value={type}>
          {type}
        </MenuItem>
      ))}
    </Select>
  )
)

const Units = ({ className }: InputProps) => {
  const appConfig = useSelector(getAppConfiguration)
  const original = usePTSelector(getConnection)?.units
  const dispatch = useDispatch()
  const save = (value: string) => dispatch(setConnectionUnits(value))
  return (
    <div className={className}>
      <Select
        label={'Units'}
        variant={'standard'}
        size={'small'}
        value={original || ''}
        onChange={e => save(e.target.value)}
      >
        {!original && <MenuItem value={''}></MenuItem>}
        {appConfig?.units.map((unit, idx) => (
          <MenuItem key={idx} value={unit}>
            {unit}
          </MenuItem>
        ))}
      </Select>
    </div>
  )
}

const isPeripheryDirection = (value: string): value is FlowDirection =>
  value === 'in' || value === 'out' || value === 'both'

const DirectionForm = ({ original, save }: FormArgs<FlowDirection | undefined>) => (
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
      {flowDirections.map((direction, idx) => (
        <MenuItem key={idx} value={direction}>
          {directionIcons[direction]} {direction}
        </MenuItem>
      ))}
    </Select>
  </div>
)

const Direction = () => {
  const direction = usePTSelector(s => getConnection(s)?.direction)
  const save = bindActionCreators(setConnectionDirection, useDispatch())
  return <DirectionForm original={direction} save={save} />
}
Direction.displayName = 'Direction'

export const ConnectionForm = () => {
  const { save, cancel } = bindActionCreators(
    { save: saveConnection, cancel: cancelConnection },
    useDispatch()
  )
  return (
    <div className={styles.form}>
      <Direction />
      <Name className={styles.name} />
      <Units className={styles.units} />
      <Types className={styles.type} />
      <div className={styles.formButtons}>
        <div className={styles.saveButton} onClick={() => save()}>
          <SaveIcon />
        </div>
        <div className={styles.cancelButton} onClick={() => cancel()}>
          <CancelIcon />
        </div>
      </div>
    </div>
  )
}

// eslint-disable-next-line @typescript-eslint/no-empty-object-type
type SelP<R, P = {}> = (args: P) => (state: RootState) => R | undefined

// eslint-disable-next-line @typescript-eslint/no-empty-object-type
export const connectionListFactory = <P extends object = {}>(
  getConnections: SelP<PeripheryConnection[], P>,
  isEditable: boolean = false
): ((props: P) => React.JSX.Element) => {
  type OnlyConnectionKey = { connectionKey: number }
  type ConnectionKey = P & OnlyConnectionKey
  type ListItemProps = { original: P }

  const selector =
    <R extends object>(f: (c: PeripheryConnection | undefined) => R): SelP<R, ConnectionKey> =>
    (connectionKey: ConnectionKey) =>
    (state: RootState) =>
      f(getConnections(connectionKey)(state)?.[connectionKey.connectionKey])

  const connector =
    <A extends object>(f: (c: PeripheryConnection | undefined) => A) =>
    (cmp: (props: A) => React.JSX.Element) =>
    (args: ConnectionKey) => {
      const data = usePTSelector(selector(f)(args))
      return !!data ? cmp(data!) : null
    }

  const DirectionText = connector(p => ({ direction: (p?.direction || '') as FlowDirection }))(
    DirectionIcon
  )

  const NameText = connector(p => ({ text: p?.name || '', className: styles.name }))(Text)

  const TypesText = connector(p => ({ text: p?.type || '', className: styles.type }))(Text)

  const UnitsText = connector(p => ({ text: p?.units || '', className: styles.units }))(Text)

  const ButtonComponent = ({ connectionKey }: OnlyConnectionKey) => {
    const { tryDelete, tryEdit } = bindActionCreators(
      {
        tryDelete: deleteConnection,
        tryEdit: editConnection
      },
      useDispatch()
    )
    return (
      <div className={styles.buttons}>
        <div className={styles.editButton} onClick={() => tryEdit(connectionKey)}>
          <EditIcon sx={{ fontSize: '18px' }} />
        </div>
        <div className={styles.deleteButton} onClick={() => tryDelete(connectionKey)}>
          <DeleteIcon sx={{ fontSize: '18px' }} />
        </div>
      </div>
    )
  }

  const Buttons = isEditable ? ButtonComponent : () => <div />
  const ConnectionItem: ListItem<ListItemProps> = ({ itemKey, original }) => (
    <>
      <DirectionText {...original} connectionKey={itemKey} />
      <NameText {...original} connectionKey={itemKey} />
      <UnitsText {...original} connectionKey={itemKey} />
      <TypesText {...original} connectionKey={itemKey} />
      <Buttons connectionKey={itemKey} />
    </>
  )

  const List = (props: ListItemProps & P) => {
    const { original } = props
    const count = usePTSelector(s => getConnections(props)(s)?.length || 0)
    return (
      <GenericList
        original={original}
        Item={ConnectionItem}
        count={count}
        listConfigCss={{
          columns: isEditable ? 6 : 5,
          overflow: 'hidden',
          maxHeight: 'fit-content',
          columnMin: 'auto',
          columnMax: 'min-content'
        }}
        containerClassName={classNames(styles.listContainer, isEditable && styles.editable)}
      />
    )
  }

  return (props: P) => <List {...props} original={props} />
}

const fromListSelector: SelP<PeripheryConnection[], WithItemKey> =
  ({ itemKey }: WithItemKey) =>
  (state: RootState) =>
    getKnownEntities(state)[itemKey].connections || []

export const ConnectionsList = connectionListFactory(fromListSelector)

const fromNewEntityListSelector: SelP<PeripheryConnection[]> = () => (state: RootState) =>
  getNewEntity(state)?.connections || []

const InnerNewEntityList = connectionListFactory(fromNewEntityListSelector, true)

export const NewEntityConnectionsList = () => (
  <div className={styles.newEntityConnections}>
    <ConnectionForm />
    <InnerNewEntityList />
  </div>
)
