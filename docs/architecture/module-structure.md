# Module Structure

## Modules

```
caffeinate/
  dependencies/     Java platform BOM for centralized version management
  annotations/      Annotation definitions (@Immutable, @Mutable, @Annotate, etc.)
  processor/        Annotation processor implementation
```

Planned modules (`bom`, `tests`) are commented out in `settings.gradle`.

### dependencies

A Gradle Java Platform that centralizes dependency versions across all modules. Uses `allowDependencies` to enforce version constraints for:

| Library                | Version     |
|------------------------|-------------|
| Groovy BOM             | 5.0.4       |
| Spock BOM              | 2.4-groovy-5.0 |
| Google Auto Service    | 1.1.1       |
| Dagger                 | 2.59.2      |
| Palantir JavaPoet      | 0.11.0      |
| Google Compile Testing | 0.23.0      |
| JSpecify               | 1.0.0       |

### annotations

Defines the public API that users depend on. Contains:

**Core annotations** (`io.github.joke.caffeinate`):
- `@Immutable` -- marks interfaces/abstract classes for immutable implementation generation
- `@Mutable` -- marks interfaces/abstract classes for mutable implementation generation
- `Target` -- enum discriminator (CONSTRUCTABLE, IMMUTABLE, MUTABLE)

**Customization annotations** (`io.github.joke.caffeinate.customize`):
- `@Annotate` -- add custom annotations to generated code (repeatable, targets: AUTO, CONSTRUCTOR, FIELD, GETTER, SETTER)
- `@Name` -- customize generated class or field naming
- `@ToString` -- customize toString() style (STRING_JOINER or TO_STRING_BUILDER)

All packages are annotated with `@NullMarked` for JSpecify compatibility.

### processor

The annotation processor implementation. Key package structure:

```
io.github.joke.caffeinate/
  CaffeinateProcessor.java          Entry point (AbstractProcessor)
  component/
    ProcessorComponent.java          Root Dagger component
    ProcessorModule.java             Provides Filer, Messager, Types
  phase/
    AnalysisPhase.java               Qualifier for analysis strategies
    ValidationPhase.java             Qualifier for validation strategies
    GenerationPhase.java             Qualifier for generation strategies
  strategy/
    GenerationStrategy.java          Strategy interface
    ClassModel.java                  Shared mutable model
    Property.java                    Discovered property record
    PropertyUtils.java               Getter/setter name utilities
    TypeHierarchyResolver.java       Collects abstract methods from type hierarchy
    PropertyDiscoveryStrategy.java   Discovers properties from abstract getters
    ClassStructureStrategy.java      Sets class name, modifiers, supertypes
    FieldStrategy.java               Generates private final fields
    GetterStrategy.java              Generates @Override getter methods
    ConstructorStrategy.java         Generates all-args constructor
  immutable/
    ImmutableSubcomponent.java       Dagger subcomponent
    ImmutableModule.java             Binds immutable strategies into phase sets
    ImmutableGenerator.java          Orchestrates immutable generation
  mutable/
    MutableSubcomponent.java         Dagger subcomponent
    MutableModule.java               Binds mutable strategies into phase sets
    MutableGenerator.java            Orchestrates mutable generation
    MutableFieldStrategy.java        Generates private (non-final) fields
    MutableConstructorStrategy.java  Generates no-args + all-args constructors
    SetterStrategy.java              Generates setter methods
    SetterValidationStrategy.java    Validates declared setters match properties
    MutablePropertyDiscoveryStrategy.java  Discovers properties + setters
```

## Dependency Graph

```
processor ---> annotations
processor ---> dependencies (platform)
annotations -> dependencies (platform)
```

The processor depends on annotations at runtime (it needs the annotation classes on the classpath to process them). Annotations has no dependency on the processor.
