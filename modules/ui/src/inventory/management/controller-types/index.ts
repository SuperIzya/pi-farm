import { addEpic } from '../../../store/epics'

export { ControllerTypesList } from './list'
export { ControllerTypeForm } from './form'

import { newTypeCanBeSaved } from './epics'

addEpic(newTypeCanBeSaved)
