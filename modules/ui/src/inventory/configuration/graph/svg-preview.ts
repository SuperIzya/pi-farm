import { toSvg } from 'html-to-image'
import { getNodesBounds, getViewportForBounds } from '@xyflow/react'
import type { Node } from '@xyflow/react'

const IMAGE_WIDTH = 800
const IMAGE_HEIGHT = 600

export const generateSvgPreview = (nodes: Node[]): Promise<string> => {
  const viewport = document.querySelector('#graph-canvas .react-flow__viewport') as HTMLElement
  if (!viewport || nodes.length === 0) return Promise.resolve('')

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
