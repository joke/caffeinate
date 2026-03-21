# Processing Pipeline

## Entry Point

`CaffeinateProcessor` extends `javax.annotation.processing.AbstractProcessor` and is registered via `@AutoService(Processor.class)`, which generates the `META-INF/services` entry automatically.

### Initialization

During `init(ProcessingEnvironment)`, the processor creates the Dagger component graph:

```java
ProcessorComponent component = DaggerProcessorComponent.builder()
    .processorModule(new ProcessorModule(processingEnv))
    .build();
```

This provides access to `ImmutableSubcomponent` and `MutableSubcomponent` factories, which create per-element generators with the correct strategy sets.

### Supported Annotations

The processor handles two annotation types:
- `io.github.joke.caffeinate.Immutable`
- `io.github.joke.caffeinate.Mutable`

### Processing Flow

```
process(annotations, roundEnv)
  |
  for each annotation type:
  |   for each annotated element:
  |     |
  |     +-- Validate: must be interface or abstract class
  |     +-- Validate: abstract class must have no-args constructor
  |     +-- Create subcomponent with TypeElement
  |     +-- Call generator.generate()
  |
  return false (allow other processors to run)
```

### Validation Rules

Before generating code, the processor validates:

1. **Element kind** -- must be an interface (`ElementKind.INTERFACE`) or abstract class (class with `Modifier.ABSTRACT`). Other element kinds produce a diagnostic error.

2. **No-args constructor** -- abstract classes must have a no-args constructor (needed for `super()` calls in generated subclass). Missing no-args constructor produces a diagnostic error.

## Generator Pipeline

Each generator orchestrates strategy execution in phases. The key difference between `ImmutableGenerator` and `MutableGenerator` is that mutable adds a validation phase.

### Immutable Pipeline

```
1. Create ClassModel
2. Run @AnalysisPhase strategies
     PropertyDiscoveryStrategy: discover properties from abstract getters
3. Check hasErrors() --> short-circuit if true
4. Run @GenerationPhase strategies
     ClassStructureStrategy:  set class name, modifiers, supertypes
     FieldStrategy:           generate private final fields
     GetterStrategy:          generate @Override getter methods
     ConstructorStrategy:     generate all-args constructor
5. Assemble TypeSpec from ClassModel
6. Write JavaFile to Filer
```

### Mutable Pipeline

```
1. Create ClassModel
2. Run @AnalysisPhase strategies
     MutablePropertyDiscoveryStrategy: discover properties + setters
3. Run @ValidationPhase strategies
     SetterValidationStrategy: validate setter-property matching
4. Check hasErrors() --> short-circuit if true
5. Run @GenerationPhase strategies
     ClassStructureStrategy:      set class name, modifiers, supertypes
     MutableFieldStrategy:        generate private (non-final) fields
     GetterStrategy:              generate @Override getter methods
     SetterStrategy:              generate setter methods
     MutableConstructorStrategy:  generate no-args + all-args constructors
6. Assemble TypeSpec from ClassModel
7. Write JavaFile to Filer
```

### Error Short-Circuit

If any strategy sets `model.setHasErrors(true)` (typically during analysis or validation), the generator skips the generation phase entirely. This prevents generating invalid code when the source has issues.

### TypeSpec Assembly

After all strategies have populated the `ClassModel`, the generator builds the final `TypeSpec`:

```java
TypeSpec.Builder builder = TypeSpec.classBuilder(model.getClassName())
    .addModifiers(model.getModifiers())
    .addSuperinterfaces(model.getSuperinterfaces())
    .addFields(model.getFields())
    .addMethods(model.getMethods());

if (model.getSuperclass() != null) {
    builder.superclass(model.getSuperclass());
}

JavaFile.builder(packageName, builder.build())
    .build()
    .writeTo(filer);
```
