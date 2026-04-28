import React, { Dispatch } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  List,
  ListItemButton,
  ListItemText,
  Box,
  Typography
} from '@mui/material'
import { connect } from 'react-redux'
import { createSelector } from 'reselect'
import type { ControllerId, PeripheryTypeId } from '../../types'
import { getKnownEntities as getControllers } from '../controller/selectors'
import { getKnownEntities as getControllerTypes } from '../controller-types/selectors'
import { getKnownEntities as getPeripheryTypes } from '../periphery-types/selectors'
import { getNewEntity, getSelectedControllerId, getSelectedPeripheryId } from './selectors'
import { Node } from '@xyflow/react'
import { ControllerShort, PeripheryShort } from './slot-modal-selectors'
import { resetControllerId, setSelectedControllerId, setSelectedPeripheryId } from './actions'
import { PayloadAction } from '@reduxjs/toolkit'

export type SlotNodeData = {
  slotIndex: number
  direction: 'in' | 'out'
  expectedType: string
  expectedUnits: string
  controllerId?: ControllerId
  peripheryId?: string
  controllerName?: string
  peripheryTypeName?: string
}

type ControllerListInnerProps = {
  controllers: ControllerShort[]
  selectedId: ControllerId | null
  onSelect: (id: ControllerId) => void
}

const ControllerListInner = ({ controllers, selectedId, onSelect }: ControllerListInnerProps) => (
  <>
    <Typography variant='subtitle2'>Step 1: Select Controller</Typography>
    <List>
      {controllers.map(c => (
        <ListItemButton key={c.id} selected={selectedId === c.id} onClick={() => onSelect(c.id)}>
          <ListItemText primary={c.name} secondary={c.description} />
        </ListItemButton>
      ))}
    </List>
  </>
)

const mapControllerListProps = createSelector(
  [getControllers, getSelectedControllerId],
  (controllers, selectedControllerId) => ({
    controllers,
    selectedId: selectedControllerId || null
  })
)

const mapControllerListDispatch = (dispatch: Dispatch<PayloadAction<ControllerId>>) => ({
  onSelect: (id: ControllerId) => dispatch(setSelectedControllerId(id))
})

const ControllerList = connect(
  mapControllerListProps,
  mapControllerListDispatch
)(ControllerListInner)

const NoPeripheries = () => (
  <Typography variant='body2' color='error'>
    No compatible peripheries.
  </Typography>
)

const PeripheriesListInner = ({
  peripheries,
  onSelect,
  selectedId
}: {
  peripheries: PeripheryShort[]
  selectedId: string
  onSelect: (id: string) => void
}) =>
  peripheries.length === 0 ? (
    <NoPeripheries />
  ) : (
    <List>
      {peripheries.map(p => (
        <ListItemButton key={p.id} selected={selectedId === p.id} onClick={() => onSelect(p.id)}>
          <ListItemText primary={p.name} secondary={p.description} />
        </ListItemButton>
      ))}
    </List>
  )

const mapPeripheriesListProps = createSelector(
  [
    getControllers,
    getControllerTypes,
    getPeripheryTypes,
    getSelectedControllerId,
    getSelectedPeripheryId
  ],
  (controllers, controllerTypes, peripheryTypes, selectedControllerId, selectedPeripheryId) => {
    const typeId = controllers.find(c => c.id === selectedControllerId)?.typeId
    const peripheryIds = Object.entries(
      controllerTypes.find(ct => ct.id === typeId)?.peripheries || {}
    ).reduce<Record<PeripheryTypeId, string>>((acc, [id, ptId]) => ({ ...acc, [ptId]: id }), {})
    const peripheries: PeripheryShort[] = peripheryTypes
      .map(
        pt =>
          pt.id in peripheryIds && {
            id: peripheryIds[pt.id],
            name: pt.name,
            description: pt.description
          }
      )
      .filter(p => p !== false)
    return {
      selectedId: selectedPeripheryId || '',
      peripheries
    }
  }
)

const mapPeripheriesListDispatch = (dispatch: Dispatch<PayloadAction<string>>) => ({
  onSelect: (id: string) => dispatch(setSelectedPeripheryId(id))
})

const PeripheriesList = connect(
  mapPeripheriesListProps,
  mapPeripheriesListDispatch
)(PeripheriesListInner)

type InnerPeripheryListProps = {
  controllerName: string
  onBack: () => void
}

const InnerPeripheryList = ({ controllerName, onBack }: InnerPeripheryListProps) => (
  <>
    <Typography variant='subtitle2' sx={{ mb: 1 }}>
      Controller: <strong>{controllerName}</strong>
    </Typography>
    <Button size='small' onClick={onBack} sx={{ mb: 2 }}>
      ← Change
    </Button>

    <Typography variant='subtitle2' sx={{ mb: 1 }}>
      Step 2: Select Periphery
    </Typography>
    <PeripheriesList />
  </>
)

const mapPeripheryListProps = createSelector(
  [getControllers, getSelectedControllerId],
  (controllers, selectedControllerId) => ({
    controllerName: controllers.find(({ id }) => id === selectedControllerId)?.name || ''
  })
)

const PeripheryList = connect(mapPeripheryListProps)(InnerPeripheryList)

type InnerSlotModalProps = {
  open: boolean
  slotNode?: Node<SlotNodeData>
  selectedControllerId?: ControllerId
  canConfirm: boolean
  onClose: () => void
  onBackToControllers: () => void
  onConfirm: () => void
}

const InnerSlotModal = ({
  open,
  slotNode,
  selectedControllerId,
  canConfirm,
  onClose,
  onBackToControllers,
  onConfirm
}: InnerSlotModalProps) =>
  open
  && slotNode && (
    <Dialog open={open} onClose={onClose} maxWidth='sm' fullWidth>
      <DialogTitle>Select Controller & Periphery</DialogTitle>
      <DialogContent>
        <Box>
          <Typography variant='body2' color='textSecondary' sx={{ mb: 2 }}>
            Expected: <strong>{slotNode.data.expectedType}</strong> in{' '}
            <strong>{slotNode.data.expectedUnits}</strong>
          </Typography>

          {!selectedControllerId ? (
            <ControllerList />
          ) : (
            <PeripheryList onBack={onBackToControllers} />
          )}
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button onClick={onConfirm} variant='contained' disabled={!canConfirm}>
          Confirm
        </Button>
      </DialogActions>
    </Dialog>
  )

const mapStateToProps = createSelector(
  [getControllers, getNewEntity, getSelectedControllerId, getSelectedPeripheryId],
  (controllers, newEntity, selectedControllerId, selectedPeripheryId) => ({
    controllers: controllers.map(c => ({
      id: c.id,
      name: c.name,
      description: c.description
    })),
    nodes: newEntity?.nodes || [],
    edges: newEntity?.edges || [],
    selectedControllerId,
    canConfirm: !!selectedControllerId && !!selectedPeripheryId
  })
)

// TODO: Add these to store.ts later
const mapDispatchToProps = (dispatch: Dispatch<PayloadAction<unknown>>) => ({
  onSelect: (id: ControllerId) => dispatch(setSelectedControllerId(id)),
  onBackToControllers: () => dispatch(resetControllerId()),
  onConfirm: () => {},
  resetSlotSelection: () => {},
  confirmSlotSelection: () => {}
})

export const SlotModal = connect(mapStateToProps, mapDispatchToProps)(InnerSlotModal)
