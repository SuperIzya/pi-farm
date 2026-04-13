Scan the project for undocumented or poorly documented code and generate or update documentation.

## Steps

1. Scan all public traits, classes, objects, and methods in `modules/common/src/main/scala/` and `modules/server/src/main/scala/`.
2. Identify:
   - Public API methods missing Scaladoc comments
   - Trait/class definitions without a description of their purpose
   - Complex methods (>15 lines) without inline comments explaining logic
   - Plugin API surface (`plugin/` package) — this must be fully documented as it is the extension point
3. For each finding, generate appropriate Scaladoc:
   - Trait/Class: Purpose, usage context, and key methods
   - Methods: `@param`, `@return`, side effects, and ZIO environment requirements
   - Keep comments concise — one line for simple methods, 2-4 lines for complex ones
4. Check `README.md` for accuracy:
   - Does it reflect the current module structure?
   - Are the development commands up to date?
   - Are the architecture descriptions accurate?
5. For frontend code (`modules/ui/src/`):
   - Check that exported components and hooks have JSDoc comments
   - Verify Redux slice descriptions explain the state shape
   - Check that WebSocket command/data types are documented

## Output

List all files updated with a brief summary of what was added or changed.
