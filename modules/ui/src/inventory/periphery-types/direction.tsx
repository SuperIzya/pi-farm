import React, { JSX } from 'react'
import ArrowRightAltIcon from '@mui/icons-material/ArrowRightAlt'
import SyncAltIcon from '@mui/icons-material/SyncAlt'
import TrendingFlatIcon from '@mui/icons-material/TrendingFlat'
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
    <Tooltip title={title} placement="bottom" arrow enterDelay={200} leaveDelay={300}>
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
    Icon={<ArrowRightAltIcon />}
    title={'In'}
    parentClass={className}
    className={styles.directionIn}
  />
)

const Out = ({ className }: Props) => (
  <Generic
    Icon={<TrendingFlatIcon />}
    title={'Out'}
    className={styles.out}
    parentClass={className}
  />
)

const Both = ({ className }: Props) => (
  <Generic
    Icon={<SyncAltIcon />}
    title={'Both'}
    parentClass={className}
    className={styles.both}
  />
)

const directions: Directions = {
  in: In,
  out: Out,
  both: Both
}

export const Direction = (p: Props) => directions[p.direction](p)
