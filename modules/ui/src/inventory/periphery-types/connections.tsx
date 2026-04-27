import React from 'react'
import {FormArgs, formMapField, formTextField, mapSave} from '../form-mixin'
import {getConnection, getNewEntity} from './selectors'
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
import {createSelector, Dispatch, PayloadAction} from '@reduxjs/toolkit'
import {PeripheryConnection, PeripheryDirection} from '../../types'
import {GenericList, ListItem, WithItemKey} from '../../utils/list-mixin'
import * as styles from './connections.scss'
import Select from '@mui/material/Select'
import MenuItem from '@mui/material/MenuItem'
import {connect, MapStateToPropsFactory} from 'react-redux'
import {Text} from '../../utils/text'
import classNames from 'classnames'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import InputIcon from '@mui/icons-material/Input'
import OutputIcon from '@mui/icons-material/Output'
import SyncAltIcon from '@mui/icons-material/SyncAlt'
import SaveIcon from '@mui/icons-material/Save'
import CancelIcon from '@mui/icons-material/CancelOutlined'
import {RootState} from './types'


const textField = formTextField(getConnection)
const mapField = formMapField(getConnection)

const directionStyles: { [key in PeripheryDirection]: string } = {
    in: styles.in,
    out: styles.out,
    both: styles.both
}
const directionIcons: { [key in PeripheryDirection]: React.ReactElement } = {
    in: <InputIcon sx={{fontSize: '18px'}}/>,
    out: <OutputIcon sx={{fontSize: '18px'}}/>,
    both: <SyncAltIcon sx={{fontSize: '18px'}}/>
}

const DirectionIcon = ({direction}: { direction: PeripheryDirection }) => (
    <div className={classNames(styles.direction, directionStyles[direction])}>
        {directionIcons[direction] || null}
    </div>
)
const Name = textField(setConnectionName, ({name}) => name || '', 'Name')

const Types = textField(setConnectionType, ({type}) => type || '', 'Type')

const Units = textField(setConnectionUnits, ({units}) => units || '', 'Units')

const isPeripheryDirection = (value: string): value is PeripheryDirection =>
    value === 'in' || value === 'out' || value === 'both'

const directionForm = ({original, save}: FormArgs<PeripheryDirection | undefined>) => (
    <div className={styles.direction}>
        <Select
            labelId={'direction-label'}
            id={'direction'}
            label={'Direction'}
            value={original || ''}
            variant={'standard'}
            size={'small'}
            onChange={(e) => isPeripheryDirection(e.target.value) && save(e.target.value)}
        >
            {!original && <MenuItem value={''}></MenuItem>}
            <MenuItem value={'in'}><DirectionIcon direction={'in'}/></MenuItem>
            <MenuItem value={'out'}><DirectionIcon direction={'out'}/></MenuItem>
            <MenuItem value={'both'}><DirectionIcon direction={'both'}/></MenuItem>
        </Select>
    </div>
)

const Direction = connect(
    mapField(({direction}) => direction),
    mapSave(setConnectionDirection)
)(directionForm)
Direction.displayName = 'Direction'

type FormProps = {
    save: () => void
    cancel: () => void
    show: boolean
}

export const ConnectionForm = connect(
    createSelector([getConnection, getNewEntity], (conn, newEntity) => ({show: !!conn || !!newEntity?.connections?.length})),
    (dispatch: Dispatch<PayloadAction<void>>) => ({
        save: () => dispatch(saveConnection()),
        cancel: () => dispatch(cancelConnection())
    })
)(({show, save, cancel}: FormProps) => !show ? null : (
    <div className={styles.form}>
        <Direction/>
        <Name size='small' variant='standard'/>
        <Units size='small' variant='standard'/>
        <Types size='small' variant='standard'/>
        <div className={styles.formButtons}>
            <div className={styles.saveButton} onClick={save}>
                <SaveIcon/>
            </div>
            <div className={styles.cancelButton} onClick={cancel}>
                <CancelIcon/>
            </div>
        </div>
    </div>
))


type ConnectionKey = { connectionKey: number; itemKey?: number }

type SelP<R, S, P> = (state: S, args: P & ConnectionKey) => R | undefined

const getConnectionKey = <S, >(_: S, {connectionKey}: ConnectionKey) => connectionKey


const itemSelector = <S, P>(getConnections: SelP<PeripheryConnection[], S, P>) =>
    createSelector(getConnections, getConnectionKey, (connections, itemKey) => itemKey === undefined ? itemKey : connections?.[itemKey])

const connector = <A extends object>(f: (c: PeripheryConnection | undefined) => A) => <S, P>(getConnections: SelP<PeripheryConnection[], S, P>) =>
    connect(() => createSelector(itemSelector(getConnections), f))

export const connectionListFactory = <S, P>(getConnections: SelP<PeripheryConnection[], S, P>, isEditable: boolean = false) => {

    const DirectionText =
        connector(p => ({direction: (p?.direction || '') as PeripheryDirection}))(getConnections)(DirectionIcon)

    const NameText =
        connector(p => ({text: p?.name || '', className: styles.name}))(getConnections)(Text)

    const TypesText =
        connector(p => ({text: p?.type || '', className: styles.type}))(getConnections)(Text)

    const UnitsText =
        connector(p => ({text: p?.units || '', className: styles.units}))(getConnections)(Text)

    const mapActions = connect(() => ({}), (dispatch: Dispatch<PayloadAction<number>>, {itemKey}: WithItemKey) => ({
        tryDelete: () => dispatch(deleteConnection(itemKey)),
        tryEdit: () => dispatch(editConnection(itemKey))
    }))

    type ButtonsProps = {
        tryDelete: () => void
        tryEdit: () => void
    }

    const ButtonComponent = ({tryDelete, tryEdit}: ButtonsProps) => (
        <div className={styles.buttons}>
            <div className={styles.editButton} onClick={tryEdit}>
                <EditIcon sx={{fontSize: '18px'}}/>
            </div>
            <div className={styles.deleteButton} onClick={tryDelete}>
                <DeleteIcon sx={{fontSize: '18px'}}/>
            </div>
        </div>
    )

    const Buttons = isEditable ? mapActions(ButtonComponent) : () => (<div/>)
    const ConnectionItem: ListItem<ConnectionKey> = ({itemKey, connectionKey}) => (
        <>
            <DirectionText itemKey={connectionKey} connectionKey={itemKey}/>
            <NameText itemKey={connectionKey} connectionKey={itemKey}/>
            <UnitsText itemKey={connectionKey} connectionKey={itemKey}/>
            <TypesText itemKey={connectionKey} connectionKey={itemKey}/>
            <Buttons itemKey={itemKey}/>
        </>
    )


    const mapCount: MapStateToPropsFactory<{
        count: number
    }, P & ConnectionKey, RootState> = () => createSelector(getConnections, connections => ({count: connections?.length || 0}))


    const UnMappedList = <T, >({count, itemKey, ...rest}: { count: number, itemKey?: number } & T) => (
        <GenericList
            {...rest}
            Item={ConnectionItem}
            count={count}
            connectionKey={itemKey || 0}
            listConfigCss={{columns: isEditable ? 6 : 5}}
            containerClassName={classNames(styles.listContainer, isEditable && styles.editable)}/>
    )

    return connect(mapCount)(UnMappedList)
}
const gne: (state: RootState) => PeripheryConnection[] | undefined = createSelector(getNewEntity, (entity) => entity?.connections)

const InnerNewEntityList = connectionListFactory(gne, true)

export const NewEntityConnectionsList = () => (
    <div className={styles.newEntityConnections}>
        <ConnectionForm/>
        <InnerNewEntityList/>
    </div>
);
