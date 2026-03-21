# Code Generation

## JavaPoet

Caffeinate uses [Palantir JavaPoet](https://github.com/palantir/javapoet) (a maintained fork of Square JavaPoet) for type-safe Java source code generation. JavaPoet provides builder APIs for constructing Java source files programmatically.

### Key JavaPoet Types Used

| Type             | Purpose                                      |
|------------------|----------------------------------------------|
| `TypeSpec`       | Builds class/interface definitions            |
| `FieldSpec`      | Builds field declarations                     |
| `MethodSpec`     | Builds method declarations                    |
| `ParameterSpec`  | Builds method parameter declarations          |
| `AnnotationSpec` | Builds annotation instances                   |
| `ClassName`      | Represents fully-qualified class references   |
| `TypeName`       | Represents Java types (primitives, classes, etc.) |
| `JavaFile`       | Wraps a TypeSpec with package declaration     |

## Generation Flow

```
Strategies populate ClassModel
         |
         v
Generator builds TypeSpec from ClassModel
         |
         v
JavaFile wraps TypeSpec with package
         |
         v
JavaFile.writeTo(Filer) writes the .java file
```

### TypeSpec Assembly

The generator reads the fully-populated `ClassModel` and assembles the final class:

```java
TypeSpec.Builder builder = TypeSpec.classBuilder(model.getClassName())
    .addModifiers(model.getModifiers().toArray(new Modifier[0]))
    .addSuperinterfaces(model.getSuperinterfaces())
    .addFields(model.getFields())
    .addMethods(model.getMethods());

if (model.getSuperclass() != null) {
    builder.superclass(model.getSuperclass());
}
```

### File Writing

Generated source files are written via the `javax.annotation.processing.Filer` API:

```java
JavaFile javaFile = JavaFile.builder(packageName, typeSpec)
    .build();
javaFile.writeTo(filer);
```

The Filer integrates with the compiler's file management, ensuring generated sources are available for subsequent compilation rounds.

## Annotation Propagation

`@Nullable` annotations (and potentially other annotations via `@Annotate`) are propagated from the source getter method to all generated sites:

```
Source:                        Generated:
@Nullable String getName();   @Nullable private final String name;    // field
                               @Nullable public String getName() ...  // getter return
                               public Impl(@Nullable String name) ... // constructor param
                               public void setName(@Nullable String name) // setter param
```

This is handled by each strategy independently reading `property.getAnnotations()` and applying them to the appropriate `FieldSpec`, `MethodSpec`, or `ParameterSpec`.

## Example Output

### Input (Interface with @Immutable)

```java
@Immutable
public interface Person {
    String getFirstName();
    String getLastName();
}
```

### Generated Output

```java
package io.github.joke.tests;

import java.lang.Override;
import java.lang.String;

public class PersonImpl implements Person {
    private final String firstName;
    private final String lastName;

    public PersonImpl(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Override
    public String getFirstName() {
        return this.firstName;
    }

    @Override
    public String getLastName() {
        return this.lastName;
    }
}
```

### Input (Interface with @Mutable)

```java
@Mutable
public interface Person {
    String getFirstName();
    void setFirstName(String firstName);
    String getLastName();
    void setLastName(String lastName);
}
```

### Generated Output

```java
package io.github.joke.tests;

import java.lang.Override;
import java.lang.String;

public class PersonImpl implements Person {
    private String firstName;
    private String lastName;

    public PersonImpl() {
    }

    public PersonImpl(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Override
    public String getFirstName() {
        return this.firstName;
    }

    @Override
    public String getLastName() {
        return this.lastName;
    }

    @Override
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Override
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
```
