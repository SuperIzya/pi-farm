import type { Connection, FlowDirection } from "../../types";
import type { ProcessorEndpoint } from "./types";

export const puConnectionToEndpoint =
  (processingUnitId: string, direction: FlowDirection) =>
  ({ name, type, units }: Connection): ProcessorEndpoint => ({
    name,
    units,
    type,
    direction,
    processor: { name, unit: processingUnitId, id: processingUnitId }
  })