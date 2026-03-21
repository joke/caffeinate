# Architecture Overview

Caffeinate is a Java annotation processor that generates boilerplate code at compile time, similar to Lombok and Immutables. It processes `@Immutable` and `@Mutable` annotations on interfaces and abstract classes to generate concrete implementations with fields, constructors, getters, and setters.

## Table of Contents

- [Module Structure](module-structure.md)
- [Processing Pipeline](processing-pipeline.md)
- [Dependency Injection](dependency-injection.md)
- [Strategy Pattern](strategy-pattern.md)
- [Code Generation](code-generation.md)
- [Testing Strategy](testing-strategy.md)
- [Build and Quality](build-and-quality.md)

## High-Level Architecture

```
                         javac
                           |
                           v
                  +------------------+
                  | CaffeinateProcessor |
                  +------------------+
                           |
               +-----------+-----------+
               |                       |
               v                       v
    +-------------------+   +-------------------+
    | ImmutableGenerator |   |  MutableGenerator  |
    +-------------------+   +-------------------+
               |                       |
               v                       v
    +-------------------+   +-------------------+
    | Strategy Pipeline  |   | Strategy Pipeline  |
    | 1. Analysis        |   | 1. Analysis        |
    | 2. Generation      |   | 2. Validation      |
    +-------------------+   | 3. Generation      |
               |            +-------------------+
               v                       |
    +-------------------+              v
    |   ClassModel       |<------------+
    | (fields, methods,  |
    |  properties, etc.) |
    +-------------------+
               |
               v
    +-------------------+
    |  JavaPoet / Filer  |
    |  (write .java)     |
    +-------------------+
```

## Key Design Decisions

1. **Strategy pattern over monolithic generation** -- Each aspect of code generation (fields, getters, constructors, setters) is an independent strategy. This makes the system extensible and testable.

2. **Dagger 2 for dependency injection** -- Strategies are bound into phase-qualified sets via Dagger modules. Generators receive `Set<GenerationStrategy>` for each phase, making the pipeline configurable per annotation type.

3. **Phase-based processing** -- Strategies execute in ordered phases (Analysis, Validation, Generation). Analysis discovers properties, validation checks constraints, and generation produces code. The pipeline short-circuits on errors.

4. **Shared ClassModel** -- All strategies mutate a shared `ClassModel` instance that accumulates the generated class structure. This avoids complex return types and allows strategies to build on each other's output.

5. **Immutable/Mutable as subcomponents** -- Each annotation type has its own Dagger subcomponent with a tailored set of strategies, while sharing common strategies and infrastructure.
