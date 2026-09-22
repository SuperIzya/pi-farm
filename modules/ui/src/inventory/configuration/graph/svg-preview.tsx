import React from 'react'
import { toSvg } from 'html-to-image'
import { getNodesBounds, getViewportForBounds } from '@xyflow/react'
import { useReactFlow } from '@xyflow/react'
import { useSelector } from 'react-redux'
import styles from './svg-preview.scss'
import { useDispatch } from 'react-redux'
import { setPreviewSvg } from '../actions'
import { getNewEntity } from '../selectors'
import { bindActionCreators } from '@reduxjs/toolkit'

const IMAGE_WIDTH = 800
const IMAGE_HEIGHT = 600

export const generateSvgPreview = (): Promise<string> => {
  const flow = useReactFlow()
  const nodes = flow.getNodes()
  const viewport = document.querySelector('#graph-canvas .react-flow__viewport') as HTMLElement
  if (!flow || nodes.length === 0) return Promise.resolve('')

  const nodesBounds = getNodesBounds(nodes)
  const { x, y, zoom } = getViewportForBounds(nodesBounds, IMAGE_WIDTH, IMAGE_HEIGHT, 0.5, 2, 0.1)

  return toSvg(viewport, {
    width: IMAGE_WIDTH,
    height: IMAGE_HEIGHT,
    style: {
      width: `${IMAGE_WIDTH}px`,
      height: `${IMAGE_HEIGHT}px`,
      transform: `translate(${x}px, ${y}px) scale(${zoom})`
    }
  }).catch(() => '')
}

export const SvgPreview = () => {
  const setPreview = bindActionCreators(setPreviewSvg, useDispatch())
  const flow = useReactFlow()
  const nodes = flow.getNodes()
  const viewport = document.querySelector('#graph-canvas .react-flow__viewport') as HTMLElement
  if (!flow || nodes.length === 0) return Promise.resolve('')

  const nodesBounds = getNodesBounds(nodes)
  const { x, y, zoom } = getViewportForBounds(nodesBounds, IMAGE_WIDTH, IMAGE_HEIGHT, 0.5, 2, 0.1)

  toSvg(viewport, {
    width: IMAGE_WIDTH,
    height: IMAGE_HEIGHT,
    style: {
      width: `${IMAGE_WIDTH}px`,
      height: `${IMAGE_HEIGHT}px`,
      transform: `translate(${x}px, ${y}px) scale(${zoom})`
    }
  })
    .catch(() => '')
    .then(svg => {
      setPreview(svg)
      return svg
    })

  const svg = useSelector(getNewEntity)?.svg

  return (
    <div className={styles.preview}>
      <img src={svg} alt='SVG Preview' />
    </div>
  )
}
