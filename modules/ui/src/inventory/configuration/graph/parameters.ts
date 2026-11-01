import type { FieldType } from '../../../types'

type ParameterSchema = Readonly<Record<string, FieldType>>

export const parameterDraft = (
  schema: ParameterSchema,
  parameters: Record<string, unknown>
): Record<string, unknown> =>
  Object.entries(schema).reduce(
    (draft, [name, type]) => {
      const value = parameters[name] ?? (type === 'Boolean' ? false : '')
      return {
        ...draft,
        [name]: typeof value === 'number' ? String(value) : value
      }
    },
    { ...parameters }
  )

export const parseParameters = (
  schema: ParameterSchema,
  draft: Record<string, unknown>
): Record<string, unknown> =>
  Object.entries(draft).reduce(
    (parameters, [name, value]) => ({
      ...parameters,
      [name]:
        ['Int', 'Float', 'Double'].includes(schema[name])
        && typeof value === 'string'
        && value.trim() !== ''
          ? Number(value)
          : value
    }),
    {}
  )

export const validateParams = (
  schema: ParameterSchema,
  parameters: Record<string, unknown>
): boolean =>
  Object.entries(schema).every(([name, type]) => {
    const value = parameters[name]
    switch (type) {
      case 'Int':
        return (
          typeof value === 'number'
          && Number.isInteger(value)
          && value >= -2147483648
          && value <= 2147483647
        )
      case 'Float':
        return typeof value === 'number' && Number.isFinite(Math.fround(value))
      case 'Double':
        return typeof value === 'number' && Number.isFinite(value)
      case 'Boolean':
        return typeof value === 'boolean'
      case 'String':
        return typeof value === 'string'
      default:
        return false
    }
  }) && Object.keys(parameters).every(key => Object.hasOwn(schema, key))
