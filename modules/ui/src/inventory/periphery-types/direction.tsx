import React, { JSX } from 'react'
import WestIcon from '@mui/icons-material/West'
import EastIcon from '@mui/icons-material/East'
import SyncAltIcon from '@mui/icons-material/SyncAlt'
import { PeripheryDirection } from '../../types'
import classNames from 'classnames'
import * as styles from './direction.scss'
import Tooltip from '@mui/material/Tooltip'

type GenericProps = {
  Icon: React.JSX.Element
  title: string
  parentClass?: string
  className?: string
}

const Generic = ({ Icon, title, parentClass, className }: GenericProps) => (
  <div className={classNames(parentClass, className, styles.direction)}>
    <Tooltip title={title} placement='bottom' arrow enterDelay={200} leaveDelay={300}>
      {Icon}
    </Tooltip>
  </div>
)

type Props = {
  direction: PeripheryDirection
  className?: string
}

type Directions = { [key in PeripheryDirection]: (p: Props) => JSX.Element }

const In = ({ className }: Props) => (
  <Generic
    Icon={<WestIcon />}
    title={'In'}
    parentClass={className}
    className={styles.directionIn}
  />
)

const Out = ({ className }: Props) => (
  <Generic Icon={<EastIcon />} title={'Out'} className={styles.out} parentClass={className} />
)

const Both = ({ className }: Props) => (
  <Generic Icon={<SyncAltIcon />} title={'Both'} parentClass={className} className={styles.both} />
)

const directions: Directions = {
  in: In,
  out: Out,
  both: Both
}

export const Direction = (p: Props) => directions[p.direction](p)
