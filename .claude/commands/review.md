Review the current changeset or staged files and provide a structured analysis.

## Steps

1. Identify all modified, added, or deleted files in the current changeset (staged or unstaged).
2. For each changed file, analyze:
   - **Bugs**: Logic errors, null/None handling, off-by-one errors, resource leaks, incorrect ZIO layer wiring.
   - **Security**: Hardcoded secrets, SQL injection (non-parameterized queries), unsanitized input, missing validation.
   - **Style**: Violations of project formatting rules (see `.claude/rules/format.md`), inconsistent naming, missing type annotations on public APIs.
   - **Error Handling**: Uncaught exceptions, swallowed errors, missing `.orDie` / `.catchAll` in ZIO effects, bare `.get` on Options.
   - **Edge Cases**: Empty collections, missing database records, WebSocket disconnections, malformed UDP packets.
3. Check that new or modified code has corresponding test coverage. Flag untested public methods.
4. Verify Flyway migrations are sequential and non-breaking if any SQL files changed.
5. For frontend changes: verify TypeScript types are correct, Redux actions are dispatched properly, no `any` types introduced.

## Output Format

Provide a summary table:

| File | Severity | Category | Finding |
|------|----------|----------|---------|
| ... | Critical/Warning/Info | Bug/Security/Style/Test | Description |

Follow with detailed explanations for Critical and Warning items, including suggested fixes.
