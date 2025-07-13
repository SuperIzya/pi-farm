import { addEpic } from '../../../store/epics'

export { PeripheryTypeForm } from './form'
export { PeripheryTypesList } from './list'

import { newTypeCanBeSaved } from './epics'

addEpic(newTypeCanBeSaved)
