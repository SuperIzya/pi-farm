import React from 'react'
import { is, PresentationNames, type GetProps, type PData, type Presentation, type PresentationName } from '../types/presentation'
import { ValuePropsForm } from './value'
import type { FormArgs } from '../utils/form-mixin'
import { useState, useEffect } from 'react'
import * as styles from './form.scss'
import { DefaultGaugeProps, GaugePropsForm } from './gauge'
import Select from '@mui/material/Select'
import Fade from '@mui/material/Fade'
import MenuItem from '@mui/material/MenuItem'
import Modal from '@mui/material/Modal'


const formFor = (p: Presentation | PresentationName): React.FC<{onSave: (data: Presentation) => void}> => {
  if(p === 'Gauge' || typeof p !== 'string' && is('Gauge')(p)) {
    const props = typeof p === 'string' ? DefaultGaugeProps : p.Gauge.props
    return ({onSave}) => <GaugePropsForm onSave={(data) => onSave({ Gauge: { props: data } })} original={props} />
  }
  if(p === 'Value' || typeof p !== 'string' && is('Value')(p)) {
    const props = typeof p === 'string' ? { unit: ''} : p.Value.props
    return ({onSave}) => <ValuePropsForm onSave={(data) => onSave({ Value: { props: data } })} original={props} />
  }
  return () => null
}

export const PresentationForm = ({ original, save }: FormArgs<Presentation | undefined>) => {
  const presentation: Presentation = original ?? { Noop: { creator: () => () => null, props: {} } }
  const keys = Object.keys(presentation) as PresentationName[]
  const [selected, setSelected] = useState(keys[0] as PresentationName)
  const [showForm, setShowForm] = useState(false)
  useEffect(() => setShowForm(selected !== 'Noop'), [selected])
  const saveAndClose = (data: Presentation) => {
    save(data);
    setShowForm(false);
  }

  const PropForm = ({ name }: { name: PresentationName }) => {
    
    const C = (original !== undefined && name in original) ? formFor(original) : formFor(name)
    return <C onSave={saveAndClose} />
  }

  return (
    <div className={styles.presentation}>
      <Select
        label={'Presentation'}
        variant={'standard'}
        size={'small'}
        value={selected}
        onChange={e => setSelected(e.target.value as PresentationName)}
      >
        {PresentationNames.map((key, idx) => (
          <MenuItem key={idx} value={key}>
            {key}
          </MenuItem>
        ))}
      </Select>
      <Modal
        open={showForm}
        onClose={() => setShowForm(false)}
      >
        <Fade in={showForm}>
          <div className={styles.modal}>
            <PropForm name={selected} />
          </div>
        </Fade>
      </Modal>
    </div>
  )
}