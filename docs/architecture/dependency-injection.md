# Dependency Injection

Caffeinate uses **Dagger 2** for compile-time dependency injection. The DI graph wires strategies into phase-qualified sets and provides shared infrastructure (Filer, Messager, Types) to all components.

## Component Hierarchy

```
ProcessorComponent (@Component)
  |
  +-- ProcessorModule
  |     provides: Filer, Messager, Types (from ProcessingEnvironment)
  |
  +-- ImmutableSubcomponent.Factory
  |     |
  |     +-- ImmutableSubcomponent (@Subcomponent)
  |           +-- ImmutableModule
  |           +-- ImmutableGenerator
  |
  +-- MutableSubcomponent.Factory
        |
        +-- MutableSubcomponent (@Subcomponent)
              +-- MutableModule
              +-- MutableGenerator
```

### ProcessorComponent

Root component created once during `init()`. Provides:
- `Filer` -- for writing generated source files
- `Messager` -- for reporting diagnostic messages
- `Types` -- for type utility operations
- Factory methods for creating subcomponents

### Subcomponents

Each annotation type has a subcomponent that receives the `TypeElement` being processed as a `@BindsInstance` parameter:

```java
@Subcomponent(modules = ImmutableModule.class)
public interface ImmutableSubcomponent {
    ImmutableGenerator generator();

    @Subcomponent.Factory
    interface Factory {
        ImmutableSubcomponent create(@BindsInstance TypeElement source);
    }
}
```

This allows each generator invocation to have its own scoped `TypeElement`, while sharing the processor-level infrastructure.

## Phase Qualifiers

Three custom `@Qualifier` annotations categorize strategies by execution phase:

| Qualifier          | Purpose                              | Used By              |
|--------------------|--------------------------------------|----------------------|
| `@AnalysisPhase`   | Property discovery from source       | Both                 |
| `@ValidationPhase` | Constraint validation                | Mutable only         |
| `@GenerationPhase` | Code generation (fields, methods)    | Both                 |

## Strategy Binding

Strategies are bound into `Set<GenerationStrategy>` using Dagger's `@IntoSet`:

```java
// ImmutableModule.java
@Provides @IntoSet @AnalysisPhase
static GenerationStrategy analysisStrategy(PropertyDiscoveryStrategy s) { return s; }

@Provides @IntoSet @GenerationPhase
static GenerationStrategy fieldStrategy(FieldStrategy s) { return s; }
```

Generators receive these sets via constructor injection:

```java
@Inject
ImmutableGenerator(
    @AnalysisPhase Set<GenerationStrategy> analysisStrategies,
    @GenerationPhase Set<GenerationStrategy> generationStrategies,
    Filer filer,
    TypeElement source
) { ... }
```

## Shared vs. Specific Strategies

Some strategy classes are shared across both modules:

| Strategy                    | Immutable | Mutable |
|-----------------------------|-----------|---------|
| ClassStructureStrategy      | Yes       | Yes     |
| GetterStrategy              | Yes       | Yes     |
| PropertyDiscoveryStrategy   | Yes       | --      |
| FieldStrategy (final)       | Yes       | --      |
| ConstructorStrategy         | Yes       | --      |
| MutablePropertyDiscoveryStrategy | --   | Yes     |
| MutableFieldStrategy        | --        | Yes     |
| MutableConstructorStrategy  | --        | Yes     |
| SetterStrategy              | --        | Yes     |
| SetterValidationStrategy    | --        | Yes     |
