---
name: debug
description: Systematic debugging skill for diagnosing and fixing issues in the PiFarm IoT platform
---

# Debugging Skill — PiFarm

## When to Use
Use this skill when encountering runtime errors, test failures, unexpected behavior, or performance issues in the PiFarm project.

## Step 1: Reproduce
- Identify the exact command or action that triggers the issue
- For backend: run `sbt test` or `sbt "server/run"` and capture the full stack trace
- For frontend: check browser console, network tab, and WebSocket frames
- For integration issues: check both server logs and browser console simultaneously
- Note the exact error message, stack trace, and any relevant log output

## Step 2: Isolate
- Determine which module is affected: `common`, `common-plugins`, `server`, or `ui`
- For ZIO layer errors: trace the layer dependency graph from the error message — ZIO reports exactly which layer is missing
- For database errors: check if Flyway migrations ran successfully (look for migration logs at startup)
- For WebSocket errors: check if the issue is in serialization (`ws/` package) or in processing (`processing/` package)
- For UDP errors: verify the microcontroller address and decoder configuration
- Run the most specific test: `sbt "common/testOnly *RepositorySpec"` to narrow down

## Step 3: Diagnose
- Read the relevant source code, starting from the error location
- For ZIO effects: trace the effect chain — `.flatMap` / `for` comprehension — to find where the failure originates
- Check recent changes: `git diff` and `git log --oneline -10`
- For type errors in Scala 3: use `-explain` flag (already enabled in build.sbt) for detailed compiler explanations
- For frontend: use Redux DevTools to inspect state transitions and action payloads
- Common root causes:
  - Missing ZIO layer in `provideSomeLayer` / `provideSomeShared`
  - Doobie query returning wrong column types (check SQL against case class fields)
  - JSON codec mismatch (field names, enum discriminators)
  - WebSocket message fragmentation (check reassembly in `socket.ts`)
  - Plugin manifest not discovered (classpath/module dependency issue)

## Step 4: Fix
- Make the minimal change that addresses the root cause
- Ensure the fix follows project conventions (see `.claude/rules/format.md`)
- If the fix involves a schema change, create a new Flyway migration — never modify existing migrations
- If the fix involves a new dependency, add it to `project/Dependencies.scala` with a pinned version

## Step 5: Verify
- Run the failing test to confirm the fix: `sbt "module/testOnly *SpecName"`
- Run the full module test suite: `sbt "module/test"`
- For cross-module changes, run all tests: `sbt test`
- For frontend fixes: run `npm run type-check && npm run lint`
- Manually verify the fix in the running application if the issue was user-facing

## Logging Guidelines
- Backend: ZIO Logging with SLF4J2 bridge → Logback. Check `logback.xml` for log levels
- Add temporary `ZIO.logDebug` / `ZIO.logInfo` statements to trace execution flow
- Remove debug logging before committing
- For Doobie SQL debugging: use `.logHandler(LogHandler.jdkLogHandler)` temporarily

## Regression Test
- After fixing, write a test that would have caught the bug
- Place it in the corresponding `*Spec.scala` file
- Use property-based testing if the bug was related to edge-case input
