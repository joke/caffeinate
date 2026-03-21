# Testing Strategy

## Framework

- **Spock 2.4** (Groovy 5.0) -- BDD testing framework
- **Google Compile Testing** -- for annotation processor integration tests
- Test files use `.groovy` extension and live in `processor/src/test/groovy/`

## Test Types

### Integration Tests

Located at the processor package root, these test the annotation processor end-to-end using Google Compile Testing:

| Spec                    | What it tests                              |
|-------------------------|--------------------------------------------|
| `ImmutableProcessorSpec` | Full @Immutable processing pipeline        |
| `MutableProcessorSpec`   | Full @Mutable processing pipeline          |

Integration tests:
1. Define Java source via `JavaFileObjects.forSourceString()`
2. Compile with `javac().withProcessors(new CaffeinateProcessor()).compile(...)`
3. Assert compilation status (`SUCCESS` or `FAILURE`)
4. Assert generated source content matches expected patterns

### Unit Tests

Organized by package, mirroring the production code structure. Each spec tests a single class in isolation using Spock's built-in mocking:

**Utility classes** (`strategy/`):

| Spec                       | Class Under Test        | Focus                                   |
|----------------------------|-------------------------|-----------------------------------------|
| `PropertyUtilsSpec`         | PropertyUtils           | Getter/setter name conventions, edge cases |
| `TypeHierarchyResolverSpec` | TypeHierarchyResolver   | Hierarchy traversal, deduplication       |

**Common strategies** (`strategy/`):

| Spec                        | Class Under Test         | Focus                                    |
|-----------------------------|--------------------------|------------------------------------------|
| `ClassStructureStrategySpec` | ClassStructureStrategy  | Class naming, modifiers, supertypes       |
| `FieldStrategySpec`          | FieldStrategy           | Private final fields, annotation propagation |
| `GetterStrategySpec`         | GetterStrategy          | @Override methods, return types            |
| `ConstructorStrategySpec`    | ConstructorStrategy     | All-args constructor, super(), empty case  |
| `PropertyDiscoveryStrategySpec` | PropertyDiscoveryStrategy | Error reporting for invalid methods    |

**Mutable-specific** (`mutable/`):

| Spec                              | Class Under Test              | Focus                            |
|-----------------------------------|-------------------------------|----------------------------------|
| `MutableFieldStrategySpec`         | MutableFieldStrategy         | Private (non-final) fields        |
| `MutableConstructorStrategySpec`   | MutableConstructorStrategy   | No-args + all-args constructors   |
| `SetterStrategySpec`               | SetterStrategy               | Setter generation, naming         |
| `SetterValidationStrategySpec`     | SetterValidationStrategy     | Setter-property matching, errors  |
| `MutablePropertyDiscoveryStrategySpec` | MutablePropertyDiscoveryStrategy | Getter/setter/unknown triaging |

**Orchestrators** (`immutable/`, `mutable/`):

| Spec                   | Class Under Test     | Focus                                        |
|------------------------|----------------------|----------------------------------------------|
| `ImmutableGeneratorSpec` | ImmutableGenerator | Phase ordering, error short-circuit, file writing |
| `MutableGeneratorSpec`   | MutableGenerator   | Phase ordering + validation phase              |

## Mocking Approach

Unit tests mock `javax.lang.model` types (`TypeElement`, `ExecutableElement`, `TypeMirror`, etc.) using Spock's built-in mocking. These are all interfaces, making them straightforward to mock.

Infrastructure dependencies (`Messager`, `Filer`, `Types`) are also mocked to verify error reporting and file writing without a real compilation environment.

`ClassModel` instances are real (not mocked), since they are simple data containers that strategies populate.

## Coverage

- **JaCoCo** enforces a minimum of **70% branch coverage**
- Coverage is verified by `./gradlew check` via `jacocoTestCoverageVerification`
- Current coverage: ~96.8% branch (153/158 branches)
