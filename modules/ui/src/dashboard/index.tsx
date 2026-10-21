import React from 'react'
import { WebSocketContext, PiFarmSocket } from '../client'
import * as styles from './index.scss'
import { WaitLoading } from '../utils/wait-loading'
import { getIsLoading } from '../inventory/controller/selectors'
import { InitController } from '../inventory/controller'
import { setValue } from './store'
import { Controllers } from './controllers'
import type { Measurement, MeasurementData } from '../types/presentation'

const SensorSocket = new WebSocketContext(() => new PiFarmSocket('/sensorsWs'))

const SetValue = () => {
  const onReceiveData = SensorSocket.useOnReceiveData()
  const transformData = (data: MeasurementData): Measurement => ({
    [data['controller-id']]: data['data-points']
  })

  onReceiveData('measurements', data => setValue(transformData(data)))
  onReceiveData('command', data => setValue(transformData(data)))
  return null
}

export const Dashboard: React.FC = () => (
  <SensorSocket.Component>
    <SetValue />
    <InitController />
    <WaitLoading isLoadingSelector={getIsLoading}>
      <div className={styles.container}>
        <Controllers />
      </div>
    </WaitLoading>
  </SensorSocket.Component>
)
