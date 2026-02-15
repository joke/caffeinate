# Fields, Getters & Constructor Generation Design

## Goal

Extend the `@Immutable` annotation processor to generate fields, getter methods, and an all-args constructor based on the interface's method signatures.

**Input:**
```java
@Immutable
public interface Person {
    String getFirstName();
    boolean isActive();
}
```

**Output:**
```java
public class PersonImpl implements Person {
    private final String firstName;
    private final boolean active;

    public PersonImpl(String firstName, boolean active) {
        this.firstName = firstName;
        this.active = active;
    }

    public String getFirstName() {
        return this.firstName;
    }

    public boolean isActive() {
        return this.active;
    }
}
```

## Property Model

A `Property` class captures a single property derived from an interface method:

```java
public class Property {
    private final String fieldName;    // "firstName"
    private final TypeName type;       // String
    private final String getterName;   // "getFirstName"
}
```

**Name derivation rules:**
- `getFirstName()` → strip `get`, lowercase first char → `firstName`
- `isActive()` → strip `is`, lowercase first char → `active`

**Validation rules (compile error if violated):**
- Method must have zero parameters
- Method must not return `void`
- Method name must start with `get` (3+ chars after) or `is` (2+ chars after)

On any validation failure, all errors are emitted but no class is generated for that element.

## Two-Phase Strategy Execution

Strategies are organized into two phases via Dagger qualifier annotations:

```
Phase 1 — @AnalysisPhase:
  PropertyDiscoveryStrategy  → validates methods, populates model.properties

Phase 2 — @GenerationPhase:
  ClassStructureStrategy     → class name, visibility, implements (existing)
  FieldStrategy              → private final fields from properties
  GetterStrategy             → getter methods from properties
  ConstructorStrategy        → all-args constructor from properties
```

The `ImmutableGenerator` runs analysis strategies first, checks `model.hasErrors()`, then runs generation strategies only if no errors were found.

## ClassModel Extensions

New fields added to `ClassModel`:

- `boolean hasErrors` — set by any strategy encountering validation failures
- `List<Property> properties` — populated by `PropertyDiscoveryStrategy`
- `List<FieldSpec> fields` — populated by `FieldStrategy`
- `List<MethodSpec> methods` — populated by `GetterStrategy` and `ConstructorStrategy`

Generation-phase strategies build JavaPoet `FieldSpec`/`MethodSpec` objects and add them to the model. The generator adds them to the `TypeSpec.Builder`.

## Dagger Wiring

Two qualifier annotations: `@AnalysisPhase` and `@GenerationPhase`.

`ImmutableModule` binds:
- `PropertyDiscoveryStrategy` into `@AnalysisPhase Set<GenerationStrategy>`
- `ClassStructureStrategy` into `@GenerationPhase Set<GenerationStrategy>`
- `FieldStrategy` into `@GenerationPhase Set<GenerationStrategy>`
- `GetterStrategy` into `@GenerationPhase Set<GenerationStrategy>`
- `ConstructorStrategy` into `@GenerationPhase Set<GenerationStrategy>`

`ImmutableGenerator` injects both sets and runs them in order.

## ImmutableGenerator Flow

```
1. Create ClassModel
2. Run @AnalysisPhase strategies
3. If model.hasErrors() → return (don't write file)
4. Run @GenerationPhase strategies
5. Build TypeSpec from model (className, modifiers, superinterfaces, fields, methods)
6. Write JavaFile via Filer
```

## Validation

`PropertyDiscoveryStrategy` validates each method on the interface:
- Has parameters → compile error via `Messager`
- Returns void → compile error
- Name doesn't match `get*`/`is*` → compile error
- On any error, sets `model.setHasErrors(true)`

Errors are emitted for all invalid methods (not just the first), then generation is skipped entirely.

## New Files

```
strategy/
├── Property.java                   # Property record (fieldName, type, getterName)
├── PropertyDiscoveryStrategy.java  # Analysis: validates methods, populates properties
├── FieldStrategy.java              # Generates private final fields
├── GetterStrategy.java             # Generates getter methods
└── ConstructorStrategy.java        # Generates all-args constructor

immutable/
├── AnalysisPhase.java              # Dagger qualifier annotation
└── GenerationPhase.java            # Dagger qualifier annotation
```

## Modified Files

- `ClassModel.java` — add properties, fields, methods, hasErrors
- `ImmutableModule.java` — bind new strategies into phase-qualified sets
- `ImmutableGenerator.java` — two-phase execution, add fields/methods to TypeSpec
- `ImmutableProcessorSpec.groovy` — add 7 new tests

## Testing

**Happy path:**
1. Single `get*` getter → field + getter + constructor
2. Multiple getters → all fields, getters, multi-arg constructor
3. `is*` boolean getter → field `active`, getter `isActive()`
4. Empty interface (no methods) → class with no fields/constructor (existing test)

**Validation errors:**
5. Method with parameters → compile error
6. Void return type → compile error
7. Invalid method name (no get/is prefix) → compile error
