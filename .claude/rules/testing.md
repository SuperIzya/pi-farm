# Testing Guidelines

## Scala (Backend) — ZIO Test

### Framework
- **ZIO Test** (`dev.zio:zio-test`) with `ZIOSpecDefault` base trait
- Test framework registered in `build.sbt`: `testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")`
- Run all tests: `sbt test`
- Run single module: `sbt "common/test"` or `sbt "server/test"`
- Run single spec: `sbt "common/testOnly org.pi.farm.storage.ConfigurationRepositorySpec"`

### File Naming & Location
- Test files: `<ClassName>Spec.scala`
- Mirror source package structure under `src/test/scala/`
- Example: `modules/common/src/test/scala/org/pi/farm/storage/ConfigurationRepositorySpec.scala`

### Test Structure
```scala
object ConfigurationRepositorySpec extends PiFarmSpec {
  def spec = suite("ConfigurationRepository")(
    test("should return empty list when no configurations exist") {
      for {
        configs <- ConfigurationRepository.getAll
      } yield assertTrue(configs.isEmpty)
    },
    test("should persist and retrieve a configuration") {
      for {
        _      <- ConfigurationRepository.create(sampleConfig)
        result <- ConfigurationRepository.getById(sampleConfig.id)
      } yield assertTrue(result.contains(sampleConfig))
    }
  ).provideSomeShared[Any](testDbLayer, ConfigurationRepository.live)
}  
```

### Layer Provisioning
- Use `provideSomeShared` for layers shared across tests in a suite (e.g., database)
- Use `provideSomeLayer` for per-test layers
- Create test-specific layers in companion objects or shared test utilities
- For database tests, use in-memory H2 with Flyway migrations applied

### Property-Based Testing
- Use `check` with `Gen` generators for property-based tests
- Place reusable generators in a `generators` package: `src/test/scala/org/pi/farm/generators/`
- Example:
```scala
test("roundtrip JSON serialization") {
  check(genFlowConfiguration) { config =>
    val json = config.toJson
    val decoded = json.fromJson[FlowConfiguration]
    assertTrue(decoded == Right(config))
  }
}
```

### Fake / Test Double Patterns
- Place fake implementations in `src/test/scala/org/pi/farm/fake/`
- Fakes implement the service trait with in-memory state (e.g., `Ref[Map[...]]`)
- Prefer fakes over mocks — they are more idiomatic in ZIO

### Test Aspects
- Use `TestAspect.sequential` when tests share mutable state (e.g., database)
- Use `TestAspect.timeout(duration)` for tests that might hang
- Use `TestAspect.ignore` to temporarily skip tests (with a comment explaining why)

### What Must Be Tested
- All repository operations (CRUD) against an in-memory database
- All JSON serialization roundtrips for domain models
- Plugin processor logic (inputs → outputs)
- WebSocket command deserialization and data serialization
- Configuration validation in the processing engine

## TypeScript / React (Frontend)

### Current State
- No frontend test framework is currently configured
- When adding tests, use **Vitest** (compatible with Webpack + TypeScript)

### Recommended Setup
- Test files: `<component-name>.test.tsx` or `<module>.test.ts`
- Locate tests in a `ui/tests/<folder>/<module>.spec.ts` per tested component/module
- Example: `ui/tests/inventory/controller-types/controller-types.spec.ts`

### What Should Be Tested
- Redux slice reducers (pure functions — easy to test)
- WebSocket message parsing and dispatch logic
- Utility functions and type guards
- Component rendering with mocked Redux store for critical views
