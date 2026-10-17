import { createSelector } from '@reduxjs/toolkit'
import { controllerIdsSelector, getControllerPresentation, valuesSelector } from './selectors'
import React from 'react'
import { GenericList, type GenericListProps, type ListItem, type WithKey } from '../utils/list-mixin'
import { useSelector } from 'react-redux'
import type { ControllerPresentation, RootState } from './types'
import { Show } from '../presentation'
import * as styles from './controllers.scss'


const countSelector = createSelector(
  controllerIdsSelector,
  ids => ids.length
)

const controllerIdSelector = (itemKey: number) => createSelector(
  controllerIdsSelector,
  ids => ids[itemKey]
)

const Item: ListItem<ControllerPresentation, RootState, WithKey> = ({itemKey}) => {
  const controllerId = useSelector(controllerIdSelector(itemKey))
  const presentation: ControllerPresentation | undefined = useSelector(getControllerPresentation(controllerId))
  const controllerValue = valuesSelector(controllerId)
  return presentation && (
    <div className={styles.item}>
      {Object.entries(presentation).sort(([a, ], [b, ]) => a.localeCompare(b)).map(([periphery, value]) => (
        <div key={periphery} className={styles.periphery}>
          <label>{periphery}</label>
          <div className={styles.channels}>
            {Object.entries(value).sort(([a, ], [b, ]) => a.localeCompare(b)).map(([channel, definition]) => (
              <div key={channel} className={styles.channel}>
                <label>{channel}</label>
                <Show
                definition={definition}
                valueSelector={controllerValue(periphery, channel)}
                />
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  )
}

const GL = GenericList<ControllerPresentation, RootState>
type GLP = GenericListProps<ControllerPresentation, RootState>

const List = (p: Omit<GLP, 'count' | 'selectorFactory'>) => {
  const count = useSelector(countSelector)
  return <GL {...p} count={count} selectorFactory={() => () => undefined}/>
}

export const Controllers = () => (
  <div className={styles.container}>
    <h1 className={styles.header}>Dashboard</h1>
    <List
      Item={Item}
      containerClassName={styles.list}
      listConfigCss={{
          itemMaxHeight: '400px',
          columnMin: 'min-content',
          columnMax: 'auto'
        }}
    />
  </div>
)
