import React from 'react'
import { WebSocketContext, PiFarmSocket } from '../client'
import * as styles from './index.scss'
import { WaitLoading } from '../utils/wait-loading'
import { getIsLoading } from '../inventory/controller/selectors'
import { InitController } from '../inventory/controller'
import { setValue } from './store'
import { onReceiveData } from '../client/receive'
import { Controllers } from './controllers'

const sensorWebSocket = new PiFarmSocket('/sensorWs')
const SensorContext = WebSocketContext(sensorWebSocket)

onReceiveData('measurement', (data) => setValue(data))

export const Dashboard: React.FC = () => (
  <SensorContext>
    <InitController />
    <WaitLoading isLoadingSelector={getIsLoading}>
      <div className={styles.container}>
        <h1>Dashboard</h1>
        <Controllers />
      </div>
    </WaitLoading>
  </SensorContext>
)
