# @Mutable Annotation Support Design

**Goal:** Add `@Mutable` annotation processing that generates mutable implementation classes with non-final fields, setters, and both no-args and all-args constructors.

## Requirements

- `@Mutable` applied to interfaces only (same as `@Immutable`)
- Generated class: `<Name>Impl implements <Interface>`
- Fields are `private` (not `private final`)
- Getters generated from `get*/is*` methods (same as `@Immutable`)
- Setters auto-generated for all properties (`void set<Name>(<Type> value)`)
- If user declares setters in the interface, they are validated against properties
- Both no-args and all-args constructors generated
- Same validation as `@Immutable` for getter methods (no params, non-void, get*/is* convention)
- Additional validation: declared setter methods must match a discovered property

## Architecture

### Dagger Wiring

New `MutableSubcomponent` mirrors `ImmutableSubcomponent`:

```
ProcessorComponent
+-- ImmutableSubcomponent (existing)
|   +-- ImmutableModule -> strategies for @Immutable
+-- MutableSubcomponent (new)
    +-- MutableModule -> strategies for @Mutable
```

- `ProcessorComponent` exposes `MutableSubcomponent.Factory mutable()`
- `ObjectsProcessor` adds `Mutable.class` to supported annotations
- `ObjectsProcessor.process()` handles `@Mutable`-annotated elements via `MutableSubcomponent`

### Strategy Reuse

| Concern | @Immutable | @Mutable | Shared? |
|---|---|---|---|
| Property discovery | `PropertyDiscoveryStrategy` | `MutablePropertyDiscoveryStrategy` | Shared via `PropertyUtils` |
| Setter validation | N/A | `SetterValidationStrategy` | New |
| Class structure | `ClassStructureStrategy` | `ClassStructureStrategy` | Reused directly |
| Fields | `FieldStrategy` (private final) | `MutableFieldStrategy` (private) | Different |
| Getters | `GetterStrategy` | `GetterStrategy` | Reused directly |
| Setters | N/A | `SetterStrategy` | New |
| Constructor | `ConstructorStrategy` (all-args) | `MutableConstructorStrategy` (no-args + all-args) | Different |

### Property Discovery Refactoring

Extract shared getter-recognition logic into `PropertyUtils`:

- `isGetterMethod(ExecutableElement)` -- no params, non-void, get*/is* pattern
- `extractProperty(ExecutableElement)` -- extracts `Property` from getter
- `isSetterMethod(ExecutableElement)` -- void return, single param, set* pattern

`PropertyDiscoveryStrategy` (immutable): for each method, if getter extract property, otherwise error.

`MutablePropertyDiscoveryStrategy`: for each method, if getter extract property, if setter store on model for later validation, otherwise error.

### Setter Validation

`SetterValidationStrategy` (analysis phase): runs after property discovery. For each stored declared setter, checks that a matching property exists (field name and parameter type match). Errors on mismatched setters.

### ClassModel Extension

Add `List<ExecutableElement> declaredSetters` to `ClassModel` for setter validation.

### MutableModule Bindings

| Phase | Strategy |
|---|---|
| `@AnalysisPhase` | `MutablePropertyDiscoveryStrategy` |
| `@AnalysisPhase` | `SetterValidationStrategy` |
| `@GenerationPhase` | `ClassStructureStrategy` (reused) |
| `@GenerationPhase` | `MutableFieldStrategy` |
| `@GenerationPhase` | `GetterStrategy` (reused) |
| `@GenerationPhase` | `SetterStrategy` |
| `@GenerationPhase` | `MutableConstructorStrategy` |

## Generated Output Example

Input:
```java
@Mutable
public interface Person {
    String getFirstName();
    int getAge();
}
```

Output:
```java
public class PersonImpl implements Person {
    private String firstName;
    private int age;

    public PersonImpl() {}

    public PersonImpl(String firstName, int age) {
        this.firstName = firstName;
        this.age = age;
    }

    @Override
    public String getFirstName() {
        return this.firstName;
    }

    @Override
    public int getAge() {
        return this.age;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
```
