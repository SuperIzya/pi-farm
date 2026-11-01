import React, { useId, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import Alert from '@mui/material/Alert'
import Dialog from '@mui/material/Dialog'
import DialogTitle from '@mui/material/DialogTitle'
import DialogContent from '@mui/material/DialogContent'
import DialogActions from '@mui/material/DialogActions'
import Button from '@mui/material/Button'
import TextField from '@mui/material/TextField'
import Switch from '@mui/material/Switch'
import FormControlLabel from '@mui/material/FormControlLabel'
import type { FieldType, ProcessingUnit, WithSelector } from '../../../types'
import { setProcessorParams } from '../actions'
import type { RootState } from '../types'
import { parameterDraft, parseParameters, validateParams } from './parameters'
import * as styles from './params-dialog.scss'

type ParamsDialogProps = WithSelector<ProcessingUnit, RootState> & {
  onClose: () => void
  processorId: string
  currentParams: Record<string, unknown>
}

type ParameterFieldProps = {
  name: string
  type: FieldType
  value: unknown
  onChange: (name: string, value: string | boolean) => void
}

const ParameterField = ({ name, type, value, onChange }: ParameterFieldProps) => {
  const id = useId()
  return type === 'Boolean' ? (
    <FormControlLabel
      control={
        <Switch checked={value === true} onChange={(_, checked) => onChange(name, checked)} />
      }
      label={name}
    />
  ) : (
    <TextField
      id={id}
      fullWidth
      required
      margin='dense'
      label={name}
      type={type === 'String' ? 'text' : 'number'}
      value={value ?? ''}
      onChange={event => onChange(name, event.target.value)}
      slotProps={{
        htmlInput: type === 'Int' ? { step: 1, min: -2147483648, max: 2147483647 } : { step: 'any' }
      }}
    />
  )
}

const useCSelector = useSelector.withTypes<RootState>()

export const ParamsDialog = ({
  selector,
  onClose,
  processorId,
  currentParams
}: ParamsDialogProps) => {
  const unit = useCSelector(selector)
  const schema = unit?.paramsSchema
  const dispatch = useDispatch()
  const titleId = useId()
  const [values, setValues] = useState(() => parameterDraft(schema ?? {}, currentParams))
  const parameters = parseParameters(schema ?? {}, values)
  const isValid = schema !== undefined && validateParams(schema, parameters)
  const handleChange = (name: string, value: string | boolean) =>
    setValues(previous => ({ ...previous, [name]: value }))
  const handleSave = () => {
    dispatch(setProcessorParams({ id: processorId, parameters }))
    onClose()
  }

  return (
    <Dialog
      open
      onClose={onClose}
      aria-labelledby={titleId}
      maxWidth='sm'
      fullWidth
      classes={{ paper: styles.container }}
    >
      <DialogTitle id={titleId}>Processor Parameters: {unit?.name}</DialogTitle>
      <DialogContent className={styles.content}>
        {schema === undefined ? (
          <Alert severity='error'>Processor parameter schema is unavailable.</Alert>
        ) : (
          <>
            {Object.keys(schema).length === 0 && <Alert severity='info'>No parameters.</Alert>}
            {Object.entries(schema).map(([name, type]) => (
              <ParameterField
                key={name}
                name={name}
                type={type}
                value={values[name]}
                onChange={handleChange}
              />
            ))}
            {!isValid && (
              <Alert severity='warning'>
                Enter a valid value for every parameter. Integers must be within the 32-bit range;
                numbers must be finite.
              </Alert>
            )}
          </>
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
