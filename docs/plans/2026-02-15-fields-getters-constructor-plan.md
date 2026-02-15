# Fields, Getters & Constructor Generation Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Extend the `@Immutable` processor to generate private final fields, getter methods, and an all-args constructor from interface method signatures.

**Architecture:** Two-phase strategy execution via Dagger qualifier annotations. A `PropertyDiscoveryStrategy` (analysis phase) validates and extracts properties from interface methods. Three generation-phase strategies (`FieldStrategy`, `GetterStrategy`, `ConstructorStrategy`) populate the `ClassModel` with JavaPoet specs.

**Tech Stack:** Dagger 2 (multibindings with qualifiers), Palantir JavaPoet (FieldSpec, MethodSpec), Spock + Google Compile Testing

---

### Task 1: Infrastructure — Phase Qualifiers, Property Model, ClassModel Extensions

No new behavior. Existing 3 tests must still pass after this refactoring.

**Files:**
- Create: `processor/src/main/java/io/github/joke/objects/immutable/AnalysisPhase.java`
- Create: `processor/src/main/java/io/github/joke/objects/immutable/GenerationPhase.java`
- Create: `processor/src/main/java/io/github/joke/objects/strategy/Property.java`
- Modify: `processor/src/main/java/io/github/joke/objects/strategy/ClassModel.java`
- Modify: `processor/src/main/java/io/github/joke/objects/immutable/ImmutableGenerator.java`
- Modify: `processor/src/main/java/io/github/joke/objects/immutable/ImmutableModule.java`

**Step 1: Create `@AnalysisPhase` qualifier**

`processor/src/main/java/io/github/joke/objects/immutable/AnalysisPhase.java`:

```java
package io.github.joke.objects.immutable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.inject.Qualifier;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface AnalysisPhase {}
```

**Step 2: Create `@GenerationPhase` qualifier**

`processor/src/main/java/io/github/joke/objects/immutable/GenerationPhase.java`:

```java
package io.github.joke.objects.immutable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.inject.Qualifier;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface GenerationPhase {}
```

**Step 3: Create `Property` class**

`processor/src/main/java/io/github/joke/objects/strategy/Property.java`:

```java
package io.github.joke.objects.strategy;

import com.palantir.javapoet.TypeName;

public class Property {

    private final String fieldName;
    private final TypeName type;
    private final String getterName;

    public Property(String fieldName, TypeName type, String getterName) {
        this.fieldName = fieldName;
        this.type = type;
        this.getterName = getterName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public TypeName getType() {
        return type;
    }

    public String getGetterName() {
        return getterName;
    }
}
```

**Step 4: Extend `ClassModel`**

Add these fields and accessors to `processor/src/main/java/io/github/joke/objects/strategy/ClassModel.java`:

```java
package io.github.joke.objects.strategy;

import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Modifier;

public class ClassModel {

    private String className = "";
    private boolean hasErrors = false;
    private final List<Modifier> modifiers = new ArrayList<>();
    private final List<TypeName> superinterfaces = new ArrayList<>();
    private final List<Property> properties = new ArrayList<>();
    private final List<FieldSpec> fields = new ArrayList<>();
    private final List<MethodSpec> methods = new ArrayList<>();

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public boolean hasErrors() {
        return hasErrors;
    }

    public void setHasErrors(boolean hasErrors) {
        this.hasErrors = hasErrors;
    }

    public List<Modifier> getModifiers() {
        return modifiers;
    }

    public List<TypeName> getSuperinterfaces() {
        return superinterfaces;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public List<FieldSpec> getFields() {
        return fields;
    }

    public List<MethodSpec> getMethods() {
        return methods;
    }
}
```

**Step 5: Update `ImmutableGenerator` for two-phase execution**

Replace `processor/src/main/java/io/github/joke/objects/immutable/ImmutableGenerator.java`:

```java
package io.github.joke.objects.immutable;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import io.github.joke.objects.strategy.ClassModel;
import io.github.joke.objects.strategy.GenerationStrategy;
import java.io.IOException;
import java.util.Set;
import javax.annotation.processing.Filer;
import javax.inject.Inject;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public class ImmutableGenerator {

    private final Set<GenerationStrategy> analysisStrategies;
    private final Set<GenerationStrategy> generationStrategies;
    private final Filer filer;

    @Inject
    ImmutableGenerator(
            @AnalysisPhase Set<GenerationStrategy> analysisStrategies,
            @GenerationPhase Set<GenerationStrategy> generationStrategies,
            Filer filer) {
        this.analysisStrategies = analysisStrategies;
        this.generationStrategies = generationStrategies;
        this.filer = filer;
    }

    public void generate(TypeElement source) throws IOException {
        ClassModel model = new ClassModel();

        for (GenerationStrategy strategy : analysisStrategies) {
            strategy.generate(source, model);
        }

        if (model.hasErrors()) {
            return;
        }

        for (GenerationStrategy strategy : generationStrategies) {
            strategy.generate(source, model);
        }

        TypeSpec.Builder builder = TypeSpec.classBuilder(model.getClassName());
        for (Modifier modifier : model.getModifiers()) {
            builder.addModifiers(modifier);
        }
        for (TypeName superinterface : model.getSuperinterfaces()) {
            builder.addSuperinterface(superinterface);
        }
        for (FieldSpec field : model.getFields()) {
            builder.addField(field);
        }
        for (MethodSpec method : model.getMethods()) {
            builder.addMethod(method);
        }
        TypeSpec typeSpec = builder.build();

        ClassName sourceClass = ClassName.get(source);
        JavaFile javaFile =
                JavaFile.builder(sourceClass.packageName(), typeSpec).build();
        javaFile.writeTo(filer);
    }
}
```

**Step 6: Update `ImmutableModule` with qualifiers**

Replace `processor/src/main/java/io/github/joke/objects/immutable/ImmutableModule.java`:

```java
package io.github.joke.objects.immutable;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import dagger.multibindings.Multibinds;
import io.github.joke.objects.strategy.ClassStructureStrategy;
import io.github.joke.objects.strategy.GenerationStrategy;
import java.util.Set;

@Module
public interface ImmutableModule {

    @Multibinds
    @AnalysisPhase
    Set<GenerationStrategy> analysisStrategies();

    @Binds
    @IntoSet
    @GenerationPhase
    GenerationStrategy classStructure(ClassStructureStrategy impl);
}
```

The `@Multibinds @AnalysisPhase` declares the empty analysis set (no analysis strategies yet). `ClassStructureStrategy` moves from unqualified to `@GenerationPhase`.

**Step 7: Run existing tests**

Run: `./gradlew :processor:test`
Expected: ALL 3 PASS (empty interface, class error, enum error)

**Step 8: Commit**

```bash
git add processor/src/main/java/
git commit -m "refactor: add two-phase strategy execution with Property model"
```

---

### Task 2: Single Getter Generates Field + Getter + Constructor

**Files:**
- Modify: `processor/src/test/groovy/io/github/joke/objects/ImmutableProcessorSpec.groovy`
- Create: `processor/src/main/java/io/github/joke/objects/strategy/PropertyDiscoveryStrategy.java`
- Create: `processor/src/main/java/io/github/joke/objects/strategy/FieldStrategy.java`
- Create: `processor/src/main/java/io/github/joke/objects/strategy/GetterStrategy.java`
- Create: `processor/src/main/java/io/github/joke/objects/strategy/ConstructorStrategy.java`
- Modify: `processor/src/main/java/io/github/joke/objects/immutable/ImmutableModule.java`

**Step 1: Write the failing test**

Add this test to `ImmutableProcessorSpec.groovy`:

```groovy
def 'generates field, getter, and constructor for single getter method'() {
    given:
    def source = JavaFileObjects.forSourceString('test.Person', '''\
        package test;
        import io.github.joke.objects.Immutable;
        @Immutable
        public interface Person {
            String getFirstName();
        }
    ''')

    when:
    def compilation = javac()
        .withProcessors(new ObjectsProcessor())
        .compile(source)

    then:
    compilation.status() == Compilation.Status.SUCCESS

    and:
    def generated = compilation.generatedSourceFile('test.PersonImpl')
        .get().getCharContent(true).toString()
    generated.contains('private final String firstName')
    generated.contains('public PersonImpl(String firstName)')
    generated.contains('this.firstName = firstName')
    generated.contains('public String getFirstName()')
    generated.contains('return this.firstName')
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :processor:test --tests 'io.github.joke.objects.ImmutableProcessorSpec'`
Expected: FAIL — generated class has no fields or methods

**Step 3: Create `PropertyDiscoveryStrategy`**

`processor/src/main/java/io/github/joke/objects/strategy/PropertyDiscoveryStrategy.java`:

```java
package io.github.joke.objects.strategy;

import com.palantir.javapoet.TypeName;
import javax.annotation.processing.Messager;
import javax.inject.Inject;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

public class PropertyDiscoveryStrategy implements GenerationStrategy {

    private final Messager messager;

    @Inject
    PropertyDiscoveryStrategy(Messager messager) {
        this.messager = messager;
    }

    @Override
    public void generate(TypeElement source, ClassModel model) {
        for (ExecutableElement method : ElementFilter.methodsIn(source.getEnclosedElements())) {
            if (!method.getParameters().isEmpty()) {
                messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "Methods in @Immutable interfaces must have no parameters",
                        method);
                model.setHasErrors(true);
                continue;
            }
            if (method.getReturnType().getKind() == TypeKind.VOID) {
                messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "Methods in @Immutable interfaces must not return void",
                        method);
                model.setHasErrors(true);
                continue;
            }

            String methodName = method.getSimpleName().toString();
            String fieldName;

            if (methodName.startsWith("get") && methodName.length() > 3) {
                fieldName = Character.toLowerCase(methodName.charAt(3))
                        + methodName.substring(4);
            } else if (methodName.startsWith("is") && methodName.length() > 2) {
                fieldName = Character.toLowerCase(methodName.charAt(2))
                        + methodName.substring(3);
            } else {
                messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "Methods in @Immutable interfaces must follow get*/is* naming convention",
                        method);
                model.setHasErrors(true);
                continue;
            }

            TypeName type = TypeName.get(method.getReturnType());
            model.getProperties().add(new Property(fieldName, type, methodName));
        }
    }
}
```

**Step 4: Create `FieldStrategy`**

`processor/src/main/java/io/github/joke/objects/strategy/FieldStrategy.java`:

```java
package io.github.joke.objects.strategy;

import com.palantir.javapoet.FieldSpec;
import javax.inject.Inject;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public class FieldStrategy implements GenerationStrategy {

    @Inject
    FieldStrategy() {}

    @Override
    public void generate(TypeElement source, ClassModel model) {
        for (Property property : model.getProperties()) {
            FieldSpec field = FieldSpec.builder(
                            property.getType(), property.getFieldName(),
                            Modifier.PRIVATE, Modifier.FINAL)
                    .build();
            model.getFields().add(field);
        }
    }
}
```

**Step 5: Create `GetterStrategy`**

`processor/src/main/java/io/github/joke/objects/strategy/GetterStrategy.java`:

```java
package io.github.joke.objects.strategy;

import com.palantir.javapoet.MethodSpec;
import javax.inject.Inject;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public class GetterStrategy implements GenerationStrategy {

    @Inject
    GetterStrategy() {}

    @Override
    public void generate(TypeElement source, ClassModel model) {
        for (Property property : model.getProperties()) {
            MethodSpec getter = MethodSpec.methodBuilder(property.getGetterName())
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(property.getType())
                    .addStatement("return this.$N", property.getFieldName())
                    .build();
            model.getMethods().add(getter);
        }
    }
}
```

**Step 6: Create `ConstructorStrategy`**

`processor/src/main/java/io/github/joke/objects/strategy/ConstructorStrategy.java`:

```java
package io.github.joke.objects.strategy;

import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import javax.inject.Inject;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public class ConstructorStrategy implements GenerationStrategy {

    @Inject
    ConstructorStrategy() {}

    @Override
    public void generate(TypeElement source, ClassModel model) {
        if (model.getProperties().isEmpty()) {
            return;
        }

        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);

        for (Property property : model.getProperties()) {
            constructor.addParameter(
                    ParameterSpec.builder(property.getType(), property.getFieldName())
                            .build());
            constructor.addStatement("this.$N = $N", property.getFieldName(), property.getFieldName());
        }

        model.getMethods().add(constructor.build());
    }
}
```

**Step 7: Wire new strategies in `ImmutableModule`**

Update `processor/src/main/java/io/github/joke/objects/immutable/ImmutableModule.java`:

```java
package io.github.joke.objects.immutable;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import dagger.multibindings.Multibinds;
import io.github.joke.objects.strategy.ClassStructureStrategy;
import io.github.joke.objects.strategy.ConstructorStrategy;
import io.github.joke.objects.strategy.FieldStrategy;
import io.github.joke.objects.strategy.GenerationStrategy;
import io.github.joke.objects.strategy.GetterStrategy;
import io.github.joke.objects.strategy.PropertyDiscoveryStrategy;
import java.util.Set;

@Module
public interface ImmutableModule {

    @Multibinds
    @AnalysisPhase
    Set<GenerationStrategy> analysisStrategies();

    @Binds
    @IntoSet
    @AnalysisPhase
    GenerationStrategy propertyDiscovery(PropertyDiscoveryStrategy impl);

    @Binds
    @IntoSet
    @GenerationPhase
    GenerationStrategy classStructure(ClassStructureStrategy impl);

    @Binds
    @IntoSet
    @GenerationPhase
    GenerationStrategy field(FieldStrategy impl);

    @Binds
    @IntoSet
    @GenerationPhase
    GenerationStrategy getter(GetterStrategy impl);

    @Binds
    @IntoSet
    @GenerationPhase
    GenerationStrategy constructor(ConstructorStrategy impl);
}
```

Note: `@Multibinds @AnalysisPhase` may no longer be strictly needed since `PropertyDiscoveryStrategy` contributes via `@IntoSet @AnalysisPhase`, but keeping it is harmless and makes the set declaration explicit.

**Step 8: Run tests to verify all pass**

Run: `./gradlew :processor:test`
Expected: ALL 4 PASS (3 existing + 1 new)

If the Palantir JavaPoet API for `FieldSpec.builder()`, `MethodSpec.methodBuilder()`, `MethodSpec.constructorBuilder()`, or `$N` format differs, adjust accordingly. The test assertions verify the generated source content, so the output must match.

**Step 9: Commit**

```bash
git add processor/src/
git commit -m "feat: generate fields, getters, and constructor from interface methods"
```

---

### Task 3: Multiple Getters + Boolean `is*` Prefix Tests

**Files:**
- Modify: `processor/src/test/groovy/io/github/joke/objects/ImmutableProcessorSpec.groovy`

**Step 1: Write the tests**

Add these tests to `ImmutableProcessorSpec.groovy`:

```groovy
def 'generates fields and constructor for multiple getter methods'() {
    given:
    def source = JavaFileObjects.forSourceString('test.Person', '''\
        package test;
        import io.github.joke.objects.Immutable;
        @Immutable
        public interface Person {
            String getFirstName();
            int getAge();
        }
    ''')

    when:
    def compilation = javac()
        .withProcessors(new ObjectsProcessor())
        .compile(source)

    then:
    compilation.status() == Compilation.Status.SUCCESS

    and:
    def generated = compilation.generatedSourceFile('test.PersonImpl')
        .get().getCharContent(true).toString()
    generated.contains('private final String firstName')
    generated.contains('private final int age')
    generated.contains('public String getFirstName()')
    generated.contains('public int getAge()')
    generated.contains('PersonImpl(String firstName, int age)')
}

def 'generates field from boolean is* getter'() {
    given:
    def source = JavaFileObjects.forSourceString('test.Status', '''\
        package test;
        import io.github.joke.objects.Immutable;
        @Immutable
        public interface Status {
            boolean isActive();
        }
    ''')

    when:
    def compilation = javac()
        .withProcessors(new ObjectsProcessor())
        .compile(source)

    then:
    compilation.status() == Compilation.Status.SUCCESS

    and:
    def generated = compilation.generatedSourceFile('test.StatusImpl')
        .get().getCharContent(true).toString()
    generated.contains('private final boolean active')
    generated.contains('public boolean isActive()')
    generated.contains('return this.active')
}
```

**Step 2: Run tests to verify they pass**

Run: `./gradlew :processor:test`
Expected: ALL 6 PASS

These tests exercise logic already implemented in Task 2 — `PropertyDiscoveryStrategy` handles `is*` prefix and multiple methods.

**Step 3: Commit**

```bash
git add processor/src/test/
git commit -m "test: add tests for multiple getters and boolean is* prefix"
```

---

### Task 4: Validation Error Tests

**Files:**
- Modify: `processor/src/test/groovy/io/github/joke/objects/ImmutableProcessorSpec.groovy`

**Step 1: Write the validation tests**

Add these tests to `ImmutableProcessorSpec.groovy`:

```groovy
def 'fails when method has parameters'() {
    given:
    def source = JavaFileObjects.forSourceString('test.Person', '''\
        package test;
        import io.github.joke.objects.Immutable;
        @Immutable
        public interface Person {
            String getName(int x);
        }
    ''')

    when:
    def compilation = javac()
        .withProcessors(new ObjectsProcessor())
        .compile(source)

    then:
    compilation.status() == Compilation.Status.FAILURE
    compilation.errors().any {
        it.getMessage(null).contains('must have no parameters')
    }
}

def 'fails when method returns void'() {
    given:
    def source = JavaFileObjects.forSourceString('test.Person', '''\
        package test;
        import io.github.joke.objects.Immutable;
        @Immutable
        public interface Person {
            void doSomething();
        }
    ''')

    when:
    def compilation = javac()
        .withProcessors(new ObjectsProcessor())
        .compile(source)

    then:
    compilation.status() == Compilation.Status.FAILURE
    compilation.errors().any {
        it.getMessage(null).contains('must not return void')
    }
}

def 'fails when method name does not follow get/is convention'() {
    given:
    def source = JavaFileObjects.forSourceString('test.Person', '''\
        package test;
        import io.github.joke.objects.Immutable;
        @Immutable
        public interface Person {
            String firstName();
        }
    ''')

    when:
    def compilation = javac()
        .withProcessors(new ObjectsProcessor())
        .compile(source)

    then:
    compilation.status() == Compilation.Status.FAILURE
    compilation.errors().any {
        it.getMessage(null).contains('must follow get*/is* naming convention')
    }
}
```

**Step 2: Run tests to verify they pass**

Run: `./gradlew :processor:test`
Expected: ALL 9 PASS

Validation logic is already implemented in `PropertyDiscoveryStrategy` from Task 2.

**Step 3: Commit**

```bash
git add processor/src/test/
git commit -m "test: add validation tests for invalid method signatures"
```
