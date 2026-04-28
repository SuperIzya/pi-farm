import React, { useEffect, useLayoutEffect, useRef } from 'react'
import classNames from 'classnames'
import * as styles from './text.scss'
import Tooltip, { tooltipClasses, TooltipProps } from '@mui/material/Tooltip'
import Zoom from '@mui/material/Zoom'
import { styled } from '@mui/material/styles'

function findOverflowPosition(inner: HTMLElement, outer: HTMLElement) {
  const outerRect = outer.getBoundingClientRect()
  const text = inner.textContent || ''
  let length = text.length
  let left = 0

  while (left < length) {
    const mid = Math.floor((left + length + 1) / 2)
    inner.textContent = text.slice(0, mid)
    const innerRect = inner.getBoundingClientRect()
    if (innerRect.width <= outerRect.width && innerRect.height <= outerRect.height) {
      left = mid
    } else {
      length = mid - 1
    }
  }

  return left // Returns the last character index that fits
}

type Props = {
  text: string
  className?: string
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

export const Text = ({ text, className }: Props) => {
  const [state, setState] = React.useState<TextState>('unknown')
  const innerSpanRef = useRef<HTMLSpanElement>(null)
  const outerDivRef = useRef<HTMLDivElement>(null)
  const [clippedText, setClippedText] = React.useState(text)
  useLayoutEffect(() => {
    if (innerSpanRef.current && outerDivRef.current && state === 'unknown') {
      const innerSpan = innerSpanRef.current
      const outerSpan = outerDivRef.current
      const inner = innerSpan.getBoundingClientRect()
      const outer = outerSpan.getBoundingClientRect()
      setState('known')
      if (inner.height > outer.height || inner.width > outer.width) {
        const overflowAt = findOverflowPosition(innerSpan, outerSpan)
        const clipped = text.slice(0, overflowAt - 3) + '...'
        setClippedText(clipped)
      }
    }
  }, [text, className])

  useEffect(() => {
    setState('unknown')
    setClippedText(text)
  }, [text])

  if (state === 'unknown') {
    return (
      <div ref={outerDivRef} className={classNames(className, styles.text)}>
        <span ref={innerSpanRef}>{text}</span>
      </div>
    )
  }
  if (text === clippedText) {
    return <div className={classNames(className, styles.text)}>{text}</div>
  }
  return (
    <Wrapped
      title={
        <span className={styles.textContainer}>
          <span className={styles.fullText}>{text}</span>
        </span>
      }
      placement='auto'
      enterDelay={200}
      arrow
      slots={{
        transition: Zoom
      }}
    >
      <div className={classNames(className, styles.text, styles.overflow)}>{clippedText}</div>
    </Wrapped>
  )
}
