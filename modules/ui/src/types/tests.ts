import type { CtlAddress, DataConnection, FromProcessor, ProcessorAddress, ToProcessor } from "../types"

const isCtlAddress = (address: CtlAddress | ProcessorAddress): address is CtlAddress =>
  'controllerId' in address && 'peripheryId' in address && 'name' in address

const isProcessorAddress = (address: CtlAddress | ProcessorAddress): address is ProcessorAddress =>
  'name' in address && 'unit' in address

export const isToProcessor = (connection: DataConnection): connection is ToProcessor =>
  isCtlAddress(connection.from) && isProcessorAddress(connection.to)

export const isFromProcessor = (connection: DataConnection): connection is FromProcessor =>
  isProcessorAddress(connection.from) && isCtlAddress(connection.to)