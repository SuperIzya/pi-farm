import React from 'react'
import { WebSocketContext, PiFarmSocket } from '../client'
import * as styles from './index.scss'
import { WaitLoading } from '../utils/wait-loading'
import { getIsLoading } from '../inventory/controller/selectors'
import { InitController } from '../inventory/controller'

const sensorWebSocket = new PiFarmSocket('/sensorWs')
const SensorContext = WebSocketContext(sensorWebSocket)

export const Dashboard: React.FC = () => (
  <SensorContext>
    <InitController />
    <WaitLoading isLoadingSelector={getIsLoading}>
      <div className={styles.container}>Dashboard</div>
    </WaitLoading>
  </SensorContext>
)
