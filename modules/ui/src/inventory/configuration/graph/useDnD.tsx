import { useReactFlow, XYPosition } from '@xyflow/react'
import React, {
  createContext,
  Dispatch,
  SetStateAction,
  useCallback,
  useContext,
  useEffect,
  useState
} from 'react'
import { WithItemKey } from '../../../utils/list-mixin'

export type OnDropAction = ({ position }: { position: XYPosition }) => void

export type DragData = WithItemKey & {
  type: string
}

type DnDContextType = {
  dragData?: DragData
  setStartDragging: (dragData?: DragData) => void
  // The action to be performed when something is dropped on the flow.
  dropAction: OnDropAction | null
  setDropAction: Dispatch<SetStateAction<OnDropAction | null>>
}

export const DnDContext = createContext<DnDContextType | null>(null)

// The DnDProvider is used to provide the context for the DnD functionality.
// This allows you to wrap your `ReactFlow` component instance in the `DnDProvider`,
// so you do not need to register any callback in `App.tsx`.
// You can just use the `useDnD` hook in your components that need to start dragging a new node into the flow.
// In our case, it will be the `Sidebar` component.
export function DnDProvider({ children }: { children: React.ReactNode }) {
  const [dragData, setDragData] = useState<DragData | undefined>(undefined)
  const [dropAction, setDropAction] = useState<OnDropAction | null>(null)

  const setStartDragging = useCallback((dragData?: DragData) => {
    setDragData(dragData)
  }, [])

  return (
    <DnDContext.Provider
      value={{
        dragData,
        setStartDragging,
        dropAction,
        // This is a workaround to ensure that the drop action is not treated as a lazy function.
        setDropAction: action => setDropAction(() => action)
      }}
    >
      {children}
    </DnDContext.Provider>
  )
}

export const useDnD = () => {
  const { screenToFlowPosition } = useReactFlow()

  const context = useContext(DnDContext)

  if (!context) {
    throw new Error('useDnD must be used within a DnDProvider')
  }

  const { setStartDragging, setDropAction, dropAction, dragData } = context

  // This callback will be returned by the `useDnD` hook, and can be used in your UI,
  // when you want to start dragging a node into the flow.
  // For example, this is used in the `Sidebar` component.
  const onDragStart = useCallback(
    (event: React.PointerEvent<HTMLDivElement>, dragData: DragData, onDrop: OnDropAction) => {
      event.preventDefault();
      
      (event.target as HTMLElement).setPointerCapture(event.pointerId)

      setStartDragging(dragData)
      setDropAction(onDrop)
    },
    [setStartDragging, setDropAction]
  )

  const onDragEnd = useCallback(
    (event: PointerEvent) => {
      if (dragData === undefined) {
        setStartDragging()
        return
      }

      (event.target as HTMLElement).releasePointerCapture(event.pointerId)

      // Use elementFromPoint to get the actual element under the pointer
      const elementUnderPointer = document.elementFromPoint(event.clientX, event.clientY)
      const isDroppingOnFlow = elementUnderPointer?.closest('.react-flow')
      event.preventDefault()

      // Only allow dropping on the flow area
      if (isDroppingOnFlow) {
        const flowPosition = screenToFlowPosition({ x: event.clientX, y: event.clientY })
        dropAction?.({ position: flowPosition })
      }

      setStartDragging()
    },
    [screenToFlowPosition, setStartDragging, dropAction]
  )

  // Add global touch event listeners
  useEffect(() => {
    if (dragData === undefined) return

    document.addEventListener('pointerup', onDragEnd)

    return () => {
      document.removeEventListener('pointerup', onDragEnd)
    }
  }, [onDragEnd, dragData])

  return {
    dragData,
    onDragStart
  }
}

export const useDnDPosition = () => {
  const [position, setPosition] = useState<XYPosition | undefined>(undefined)

  // By default, the pointer move event sets the position of the dragged element in the context.
  // This will be used to display the `DragGhost` component.
  const onDrag = useCallback((event: PointerEvent) => {
    event.preventDefault()
    setPosition({ x: event.clientX, y: event.clientY })
  }, [])

  useEffect(() => {
    document.addEventListener('pointermove', onDrag)
    return () => {
      document.removeEventListener('pointermove', onDrag)
    }
  }, [onDrag])

  return { position }
}
