import React, { useEffect, useLayoutEffect, useRef } from 'react'
import classNames from 'classnames'
import * as styles from './text.scss'
import Tooltip, { tooltipClasses, TooltipProps } from '@mui/material/Tooltip'
import Zoom from '@mui/material/Zoom'
import { styled } from '@mui/material/styles'

function findOverflowPosition(div: HTMLElement, text: string) {
  const left = text.length * (div.clientWidth / (div.scrollWidth * 1.05)) * (div.clientHeight / (div.scrollHeight * 1.05))

  return left // Returns the last character index that fits
}

type Props = {
  text: string
  className?: string
  title?: string
  icon?: React.ReactNode
}

type TextState = 'unknown' | 'known'

const Wrapped = styled(({ className, ...props }: TooltipProps) => (
  <Tooltip {...props} classes={{ popper: className }} />
))(({ theme }) => ({
  [`& .${tooltipClasses.tooltip}`]: {
    backgroundColor: '#656569',
    color: '#fff',
    maxWidth: 220,
    fontSize: theme.typography.pxToRem(12),
    border: '1px solid #dadde9',
    padding: 0
  },
  [`& .${tooltipClasses.arrow}`]: {
    color: '#656569'
  }
}))

Wrapped.displayName = 'WrappedTooltip'

export const Text = ({ text, className, title, icon }: Props) => {
  const [state, setState] = React.useState<TextState>('unknown')
  const divRef = useRef<HTMLDivElement>(null)
  const [clippedText, setClippedText] = React.useState(text)
  useLayoutEffect(() => {
    if (divRef.current && state === 'unknown') {
      const div = divRef.current
      setState('known')
      if (div.scrollWidth > div.clientWidth || div.scrollHeight > div.clientHeight) {
        const overflowAt = findOverflowPosition(div, text)
        const clipped = text.slice(0, overflowAt - 3) + '...'
        setClippedText(clipped)
      }
    }
  }, [text, className])

  const Title = !!title ? () => <span>{title}:</span> : () => null

  if (state === 'unknown') {
    return (
      <div ref={divRef} className={classNames(className, styles.text)}>
        {icon}
        <Title />
        {text}
      </div>
    )
  }
  if (text === clippedText) {
    return (
      <div className={classNames(className, styles.text)}>
        {icon}
        <Title />
        {text}
      </div>
    )
  }
  return (
    <Wrapped
      title={
        <span className={styles.textContainer}>
          <span className={styles.fullText}>{text}</span>
        </span>
      }
      placement='auto'
      enterDelay={400}
      arrow
      slots={{
        transition: Zoom
      }}
    >
      <div className={classNames(className, styles.text, styles.overflow)}>
        {icon}
        <Title />
        {clippedText}
      </div>
    </Wrapped>
  )
}
