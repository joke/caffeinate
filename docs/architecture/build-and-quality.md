# Build and Quality

## Build Commands

```bash
./gradlew build          # Full build (compile, test, check)
./gradlew check          # Run all checks (ErrorProne, NullAway, coverage)
./gradlew test           # Run all tests
./gradlew :processor:test                        # Processor tests only
./gradlew :processor:test --tests 'SpecName'     # Single spec
```

## Compilation

- **Java release target:** 11
- **Compiler flags:** `-parameters` (preserve method parameter names), `-Werror` (warnings are errors)
- **Annotation processors (build-time):** Google Auto Service (META-INF/services), Dagger Compiler (DI code generation)

## Static Analysis

### ErrorProne

Applied automatically to all Java subprojects. Catches common Java programming errors at compile time. Disabled for generated code via `-XepDisableWarningsInGeneratedCode`.

### NullAway

Runs in **JSpecify mode** with `onlyNullMarked = true`. Only classes and packages annotated with `@NullMarked` are checked. All production packages in this project are `@NullMarked`.

## Code Formatting

### Palantir Java Formatter

Enforces consistent Java formatting via the `com.palantir.baseline` plugin.

### Spotless

Additional formatting rules:
- Removes wildcard imports
- Removes unused imports
- Applied to all Java sources

## Test Coverage

### JaCoCo

- Generates coverage reports after test execution
- Enforces a minimum **70% branch coverage** threshold
- Verification runs as part of `./gradlew check`
- Violation fails the build

Configuration:
```groovy
jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                counter = 'BRANCH'
                minimum = 0.70
            }
        }
    }
}
```

## Dependency Management

All dependency versions are centralized in the `dependencies/` module (Java Platform BOM). Subprojects reference the platform:

```groovy
dependencies {
    implementation platform(project(':dependencies'))
}
```

This ensures consistent versions across all modules and makes upgrades a single-point change.

## Project Coordinates

- **Group ID:** `io.github.joke.caffeinate`
- **Package root:** `io.github.joke.caffeinate`
