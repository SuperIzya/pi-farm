import React, { useEffect, useState } from 'react'
import * as styles from './params-dialog.scss'
import Dialog from '@mui/material/Dialog'
import DialogTitle from '@mui/material/DialogTitle'
import DialogContent from '@mui/material/DialogContent'
import DialogActions from '@mui/material/DialogActions'
import Button from '@mui/material/Button'
import TextField from '@mui/material/TextField'
import Switch from '@mui/material/Switch'
import FormControlLabel from '@mui/material/FormControlLabel'
import { setProcessorParams } from '../actions'
import type { FieldType, ProcessingUnit, WithSelector } from '../../../types'
import { useDispatch, useSelector } from 'react-redux'
import { RootState } from '../types'

type ParamsDialogOwnProps = {
  open: boolean
  onClose: () => void
  processorId: string
  unit: string
  currentParams: Record<string, unknown>
}

type StateProps = {
  schema: Record<string, FieldType>
}

type DispatchProps = {
  saveParams: (id: string, parameters: Record<string, unknown>) => void
}

type ParamsDialogProps = ParamsDialogOwnProps & StateProps & DispatchProps

const inputType: Record<FieldType, string> = {
  String: 'text',
  Int: 'number',
  Float: 'number',
  Double: 'number',
  Boolean: 'checkbox'
}

const parseValue = (type: FieldType, raw: string): unknown => {
  switch (type) {
    case 'Int':
      return parseInt(raw, 10) || 0
    case 'Float':
    case 'Double':
      return parseFloat(raw) || 0
    case 'Boolean':
      return raw === 'true'
    default:
      return raw
  }
}

export const validateParams = (
  schema: Record<string, FieldType>,
  params: Record<string, unknown>
): boolean =>
  Object.entries(schema).every(([name, type]) => {
    const value = params[name]
    if (value === undefined) return false
    switch (type) {
      case 'Int':
        return Number.isInteger(value)
      case 'Float':
      case 'Double':
        return typeof value === 'number'
      case 'Boolean':
        return typeof value === 'boolean'
      default:
        return typeof value === 'string'
    }
  }) && Object.keys(params).every(key => key in schema)

const ParamsDialogInner = ({
  open,
  onClose,
  processorId,
  schema,
  currentParams,
  saveParams
}: ParamsDialogProps) => {
  const [values, setValues] = useState<Record<string, unknown>>(currentParams)
  const [isValid, setIsValid] = useState(validateParams(schema, currentParams))

  useEffect(() => {
    setValues(currentParams)
  }, [open])

  useEffect(() => {
    setIsValid(validateParams(schema, values))
  }, [values, schema])

  const handleChange = (name: string, type: FieldType, raw: string) => {
    setValues(prev => ({ ...prev, [name]: parseValue(type, raw) }))
  }

  const handleBoolChange = (name: string, checked: boolean) => {
    setValues(prev => ({ ...prev, [name]: checked }))
  }

  const handleSave = () => {
    saveParams(processorId, values)
    onClose()
  }

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth='sm'
      fullWidth
      classes={{ paper: styles.container }}
    >
      <DialogTitle>Processor Parameters</DialogTitle>
      <DialogContent className={styles.content}>
        {Object.entries(schema).map(([name, type]) =>
          type === 'Boolean' ? (
            <FormControlLabel
              key={name}
              control={
                <Switch
                  checked={Boolean(values[name])}
                  onChange={(_, checked) => handleBoolChange(name, checked)}
                />
              }
              label={name}
            />
          ) : (
            <TextField
              key={name}
              fullWidth
              margin='dense'
              label={name}
              type={inputType[type]}
              value={values[name] ?? ''}
              onChange={e => handleChange(name, type, e.target.value)}
              slotProps={type === 'Int' ? { htmlInput: { step: 1 } } : undefined}
            />
          )
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button onClick={handleSave} variant='contained' disabled={!isValid}>
          Save
        </Button>
      </DialogActions>
    </Dialog>
  )
}

type ParamsDialog = Omit<ParamsDialogProps, 'schema' | 'saveParams'>
  & WithSelector<ProcessingUnit, RootState>

export const ParamsDialog = (params: ParamsDialog) => {
  const schema = useSelector.withTypes<RootState>()(
    state => params.selector(state)?.paramsSchema || {}
  )

  const dispatch = useDispatch()
  const saveParams = (id: string, parameters: Record<string, unknown>) =>
    dispatch(setProcessorParams({ id, parameters }))
  return <ParamsDialogInner {...params} schema={schema} saveParams={saveParams} />
}
