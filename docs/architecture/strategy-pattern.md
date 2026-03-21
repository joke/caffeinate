# Strategy Pattern

## GenerationStrategy Interface

All strategies implement a single interface:

```java
public interface GenerationStrategy {
    void generate(TypeElement source, ClassModel model);
}
```

- `source` -- the annotated interface or abstract class being processed
- `model` -- the shared mutable model that strategies populate

Strategies are side-effecting: they read from `source` and mutate `model`. This design allows strategies to build on each other's output within the same phase and across phases.

## ClassModel

The shared data structure passed through the strategy pipeline:

```java
public class ClassModel {
    String className;              // e.g., "PersonImpl"
    boolean hasErrors;             // short-circuit flag
    List<Modifier> modifiers;      // e.g., [PUBLIC]
    List<TypeName> superinterfaces; // interfaces to implement
    TypeName superclass;           // class to extend (nullable)
    List<Property> properties;     // discovered from abstract getters
    List<ExecutableElement> declaredSetters; // setter methods (mutable only)
    List<FieldSpec> fields;        // generated field definitions
    List<MethodSpec> methods;      // generated method definitions
}
```

## Property

Represents a property discovered from an abstract getter method:

```java
public class Property {
    String fieldName;               // e.g., "firstName"
    TypeName type;                  // e.g., String
    String getterName;              // e.g., "getFirstName"
    List<AnnotationSpec> annotations; // e.g., [@Nullable]
}
```

Properties are extracted by `PropertyUtils.extractProperty()`, which derives the field name from the getter name (`getFirstName` -> `firstName`, `isActive` -> `active`).

## Analysis Strategies

### PropertyDiscoveryStrategy (Immutable)

Discovers properties from abstract methods in the type hierarchy:

1. Uses `TypeHierarchyResolver` to collect all abstract methods from the source element and its supertypes
2. Filters for getter methods using `PropertyUtils.isGetterMethod()` (methods matching `get*`/`is*` convention with no parameters and non-void return)
3. Extracts `Property` objects from valid getters
4. Reports diagnostic errors for abstract methods that don't match the getter convention
5. Populates `model.properties`

### MutablePropertyDiscoveryStrategy (Mutable)

Extends the discovery to also recognize setter methods:

1. Collects all abstract methods from the type hierarchy
2. Classifies each method as getter, setter, or unknown:
   - Getters: `get*`/`is*` with no parameters and non-void return
   - Setters: `set*` with one parameter and void return
3. Stores setters in `model.declaredSetters` for later validation
4. Reports errors for methods that match neither convention

## Validation Strategies

### SetterValidationStrategy (Mutable only)

Validates that declared setters correspond to discovered properties:

1. For each setter in `model.declaredSetters`:
   - Derives the expected field name from the setter name (`setFirstName` -> `firstName`)
   - Looks for a matching property by field name
   - Verifies the setter parameter type matches the property type
2. Reports a diagnostic error if no matching property is found
3. Sets `model.hasErrors = true` on validation failure

## Generation Strategies

### ClassStructureStrategy

Sets up the generated class skeleton:
- Class name: source simple name + `"Impl"` suffix (e.g., `Person` -> `PersonImpl`)
- Modifier: `PUBLIC`
- If source is interface: adds source as superinterface
- If source is abstract class: sets source as superclass

### FieldStrategy (Immutable)

Generates `private final` fields for each property:
```java
private final String firstName;
```
Propagates `@Nullable` annotations from the property to the field.

### MutableFieldStrategy (Mutable)

Generates `private` (non-final) fields for each property:
```java
private String firstName;
```

### GetterStrategy

Generates `@Override` getter methods returning `this.fieldName`:
```java
@Override
public String getFirstName() {
    return this.firstName;
}
```

### ConstructorStrategy (Immutable)

Generates a single all-args constructor:
```java
public PersonImpl(String firstName, String lastName) {
    super();  // only if source is abstract class
    this.firstName = firstName;
    this.lastName = lastName;
}
```
Skipped entirely if there are no properties.

### MutableConstructorStrategy (Mutable)

Generates two constructors:
```java
// Always generated
public PersonImpl() {
    super();  // only if source is abstract class
}

// Only if properties exist
public PersonImpl(String firstName, String lastName) {
    super();  // only if source is abstract class
    this.firstName = firstName;
    this.lastName = lastName;
}
```

### SetterStrategy (Mutable)

Generates setter methods for each property:
```java
@Override
public void setFirstName(String firstName) {
    this.firstName = firstName;
}
```
Setter name derived via `PropertyUtils.setterNameForField()`.

## Utility Classes

### PropertyUtils

Static utility methods for property name conventions:

| Method                | Purpose                                                |
|-----------------------|--------------------------------------------------------|
| `isGetterMethod(e)`   | True if method matches `get*`/`is*`, no params, non-void |
| `isSetterMethod(e)`   | True if method matches `set*`, one param, void return    |
| `extractProperty(e)`  | Creates `Property` from a getter method                  |
| `setterNameForField(name)` | Converts field name to setter name (`name` -> `setName`) |

### TypeHierarchyResolver

Collects all abstract methods from a type element's full hierarchy:

1. Starts with the source element's declared methods
2. Recursively walks all implemented interfaces
3. Recursively walks the superclass chain (stops at `Object` or non-abstract classes)
4. Deduplicates by method signature (name + erased parameter types)
5. Returns an ordered list of unique abstract methods

This ensures that properties inherited from parent interfaces or abstract classes are included in the generated implementation.
