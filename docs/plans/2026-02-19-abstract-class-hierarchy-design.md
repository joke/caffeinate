# Abstract Class and Inheritance Chain Support — Design

**Date:** 2026-02-19

## Problem

The processor currently accepts only interfaces annotated with `@Immutable` or `@Mutable`.
Two gaps need to be closed:

1. **Abstract classes** cannot be annotated — the `ElementKind.INTERFACE` guard in
   `CaffeinateProcessor` rejects them immediately.
2. **Inherited abstract methods** are invisible — property discovery calls
   `source.getEnclosedElements()`, which returns only directly declared members.
   Methods inherited from parent interfaces or superclasses are silently ignored.

## Decision

Introduce a `TypeHierarchyResolver` service that walks the full `javax.lang.model` type
hierarchy and returns every abstract method that the generated class must implement.
Existing strategies consume this flat list unchanged; hierarchy traversal is isolated in
one place.

## Approach

**Approach B — TypeHierarchyResolver service.** The resolver is injected via Dagger into
both discovery strategies. All other strategies (field, getter, setter, constructor,
class structure) are modified only where strictly necessary.

## Architecture

### New file

| File | Description |
|------|-------------|
| `strategy/TypeHierarchyResolver.java` | Walks the full type hierarchy; returns all abstract methods |

### Modified files

| File | Change |
|------|--------|
| `component/ProcessorModule.java` | Add `@Provides Types` binding |
| `CaffeinateProcessor.java` | Accept abstract classes; validate no-args constructor |
| `strategy/ClassModel.java` | Add nullable `superclass` field |
| `strategy/ClassStructureStrategy.java` | Set `extends` vs `implements` by element kind |
| `strategy/PropertyDiscoveryStrategy.java` | Inject resolver; replace `getEnclosedElements()` |
| `strategy/ConstructorStrategy.java` | Add `super()` when source is abstract class |
| `mutable/MutablePropertyDiscoveryStrategy.java` | Same resolver injection as above |
| `mutable/MutableConstructorStrategy.java` | Add `super()` when source is abstract class |
| `immutable/ImmutableGenerator.java` | Apply `model.getSuperclass()` to TypeSpec builder |
| `mutable/MutableGenerator.java` | Same as ImmutableGenerator |

## Section 1 — TypeHierarchyResolver

**Package:** `io.github.joke.caffeinate.strategy`

**Dependency:** `javax.lang.model.util.Types` (new `@Provides` in `ProcessorModule`).

**API:**
```java
List<ExecutableElement> getAllAbstractMethods(TypeElement element)
```

**Traversal algorithm** (depth-first, declared members first):
1. Collect abstract methods declared directly on `element`.
2. Recurse into each interface from `element.getInterfaces()`.
3. If `element.getSuperclass()` resolves to an abstract class (not `Object`, not `NONE`),
   recurse into it.

**Deduplication:** skip a method if a more-specific type already declared it.
The seen-key is `methodName + "::" + erased-parameter-type-list`. This handles
re-declarations common in Java interface hierarchies.

**Stopping conditions:**
- Superclass `TypeKind` is `NONE` (interface root).
- Superclass is `java.lang.Object`.
- Superclass is a concrete (non-abstract) class — its methods are already implemented.

Both `PropertyDiscoveryStrategy` and `MutablePropertyDiscoveryStrategy` inject
`TypeHierarchyResolver` and replace `ElementFilter.methodsIn(source.getEnclosedElements())`
with `resolver.getAllAbstractMethods(source)`. All validation logic inside those strategies
is unchanged.

## Section 2 — Class Structure (`extends` vs `implements`)

**`ClassModel`** gains one nullable field:
```java
private TypeName superclass = null;  // null → implements interface
```

Both generators add a branch when building `TypeSpec`:
```java
if (model.getSuperclass() != null) {
    builder.superclass(model.getSuperclass());
}
```

**`ClassStructureStrategy`** adds an `ElementKind` check:
```java
if (source.getKind() == ElementKind.INTERFACE) {
    model.getSuperinterfaces().add(ClassName.get(source));
} else {
    model.setSuperclass(ClassName.get(source));
}
```

**Constructor strategies** — when the source is an abstract class, each emitted constructor
prepends `super()` as its first statement. For `@Immutable` with no properties no constructor
is emitted; for `@Mutable` with no properties the no-args constructor body gains `super()`.

## Section 3 — Validation

**`CaffeinateProcessor`** changes the kind guard:

```java
boolean isInterface = element.getKind() == ElementKind.INTERFACE;
boolean isAbstractClass = element.getKind() == ElementKind.CLASS
    && element.getModifiers().contains(Modifier.ABSTRACT);

if (!isInterface && !isAbstractClass) {
    // error: "@X can only be applied to interfaces or abstract classes"
    continue;
}
```

For abstract class sources an additional check runs before dispatching to the generator:

```java
if (isAbstractClass && !hasNoArgsConstructor(element)) {
    // error: "@X on abstract classes requires a no-args constructor"
    continue;
}
```

`hasNoArgsConstructor` uses `ElementFilter.constructorsIn(element.getEnclosedElements())`:
- Empty list → compiler provides default no-args constructor → passes.
- Non-empty list → at least one must have zero parameters.

## No behaviour changes for existing interface sources

Every existing test passes unchanged. The resolver returns the same methods that
`getEnclosedElements()` currently returns for single-level interfaces.
The `ElementKind.INTERFACE` path in `ClassStructureStrategy` is identical to today.
