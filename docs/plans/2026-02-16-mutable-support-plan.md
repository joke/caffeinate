# @Mutable Annotation Support Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add `@Mutable` annotation processing that generates mutable implementation classes with non-final fields, setters, and both no-args and all-args constructors.

**Architecture:** New `MutableSubcomponent` with its own Dagger module, reusing shared strategies (`ClassStructureStrategy`, `GetterStrategy`) and introducing mutable-specific ones (`MutableFieldStrategy`, `SetterStrategy`, `MutableConstructorStrategy`). Property discovery is refactored via `PropertyUtils` to share getter-extraction logic between `@Immutable` and `@Mutable`.

**Tech Stack:** Dagger 2 (subcomponent, multibindings with qualifiers), Palantir JavaPoet (FieldSpec, MethodSpec), Spock + Google Compile Testing

---

### Task 1: PropertyUtils extraction and PropertyDiscoveryStrategy refactoring

Refactor the getter-recognition logic out of `PropertyDiscoveryStrategy` into a reusable `PropertyUtils` utility class. Then simplify `PropertyDiscoveryStrategy` to use it. No new behavior — existing 9 tests must still pass.

**Files:**
- Create: `processor/src/main/java/io/github/joke/objects/strategy/PropertyUtils.java`
- Modify: `processor/src/main/java/io/github/joke/objects/strategy/PropertyDiscoveryStrategy.java`

**Step 1: Create `PropertyUtils`**

`processor/src/main/java/io/github/joke/objects/strategy/PropertyUtils.java`:

```java
package io.github.joke.objects.strategy;

import com.palantir.javapoet.TypeName;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;

public final class PropertyUtils {

    private PropertyUtils() {}

    public static boolean isGetterMethod(ExecutableElement method) {
        if (!method.getParameters().isEmpty()) {
            return false;
        }
        if (method.getReturnType().getKind() == TypeKind.VOID) {
            return false;
        }
        String name = method.getSimpleName().toString();
        return (name.startsWith("get") && name.length() > 3)
                || (name.startsWith("is") && name.length() > 2);
    }

    public static boolean isSetterMethod(ExecutableElement method) {
        String name = method.getSimpleName().toString();
        return method.getParameters().size() == 1
                && method.getReturnType().getKind() == TypeKind.VOID
                && name.startsWith("set")
                && name.length() > 3;
    }

    public static Property extractProperty(ExecutableElement method) {
        String methodName = method.getSimpleName().toString();
        String fieldName;

        if (methodName.startsWith("get") && methodName.length() > 3) {
            fieldName =
                    Character.toLowerCase(methodName.charAt(3))
                            + methodName.substring(4);
        } else if (methodName.startsWith("is") && methodName.length() > 2) {
            fieldName =
                    Character.toLowerCase(methodName.charAt(2))
                            + methodName.substring(3);
        } else {
            throw new IllegalArgumentException(
                    "Not a getter method: " + methodName);
        }

        TypeName type = TypeName.get(method.getReturnType());
        return new Property(fieldName, type, methodName);
    }

    public static String setterNameForField(String fieldName) {
        return "set"
                + Character.toUpperCase(fieldName.charAt(0))
                + fieldName.substring(1);
    }
}
```

**Step 2: Refactor `PropertyDiscoveryStrategy` to use `PropertyUtils`**

Replace `processor/src/main/java/io/github/joke/objects/strategy/PropertyDiscoveryStrategy.java`:

```java
package io.github.joke.objects.strategy;

import javax.annotation.processing.Messager;
import javax.inject.Inject;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
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
        for (ExecutableElement method :
                ElementFilter.methodsIn(source.getEnclosedElements())) {
            if (PropertyUtils.isGetterMethod(method)) {
                model.getProperties().add(PropertyUtils.extractProperty(method));
            } else {
                reportError(method, model);
            }
        }
    }

    private void reportError(ExecutableElement method, ClassModel model) {
        if (!method.getParameters().isEmpty()) {
            messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Methods in @Immutable interfaces must have no parameters",
                    method);
        } else if (method.getReturnType().getKind()
                == javax.lang.model.type.TypeKind.VOID) {
            messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Methods in @Immutable interfaces must not return void",
                    method);
        } else {
            messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Methods in @Immutable interfaces must follow"
                            + " get*/is* naming convention",
                    method);
        }
        model.setHasErrors(true);
    }
}
```

**Step 3: Run all existing tests**

Run: `./gradlew :processor:test`
Expected: ALL 9 PASS (no behavior change)

**Step 4: Commit**

```bash
git add processor/src/main/java/io/github/joke/objects/strategy/PropertyUtils.java processor/src/main/java/io/github/joke/objects/strategy/PropertyDiscoveryStrategy.java
git commit -m "refactor: extract PropertyUtils for shared getter/setter recognition"
```

---

### Task 2: Mutable Dagger wiring and basic generation (TDD)

Add the `MutableSubcomponent`, `MutableModule`, `MutableGenerator`, and wire into `ObjectsProcessor`. Use `ClassStructureStrategy` and `GetterStrategy` (reused). Write a test for a basic `@Mutable` interface with a single getter — should generate `PersonImpl implements Person` with a non-final field, getter, setter, and both constructors.

**Files:**
- Create: `processor/src/main/java/io/github/joke/objects/mutable/MutableSubcomponent.java`
- Create: `processor/src/main/java/io/github/joke/objects/mutable/MutableModule.java`
- Create: `processor/src/main/java/io/github/joke/objects/mutable/MutableGenerator.java`
- Create: `processor/src/main/java/io/github/joke/objects/mutable/package-info.java`
- Create: `processor/src/main/java/io/github/joke/objects/mutable/MutablePropertyDiscoveryStrategy.java`
- Create: `processor/src/main/java/io/github/joke/objects/mutable/SetterValidationStrategy.java`
- Create: `processor/src/main/java/io/github/joke/objects/mutable/MutableFieldStrategy.java`
- Create: `processor/src/main/java/io/github/joke/objects/mutable/SetterStrategy.java`
- Create: `processor/src/main/java/io/github/joke/objects/mutable/MutableConstructorStrategy.java`
- Modify: `processor/src/main/java/io/github/joke/objects/strategy/ClassModel.java`
- Modify: `processor/src/main/java/io/github/joke/objects/component/ProcessorComponent.java`
- Modify: `processor/src/main/java/io/github/joke/objects/component/ProcessorModule.java`
- Modify: `processor/src/main/java/io/github/joke/objects/ObjectsProcessor.java`
- Create: `processor/src/test/groovy/io/github/joke/objects/MutableProcessorSpec.groovy`

**Step 1: Write the failing test**

Create `processor/src/test/groovy/io/github/joke/objects/MutableProcessorSpec.groovy`:

```groovy
package io.github.joke.objects

import com.google.testing.compile.Compilation
import com.google.testing.compile.JavaFileObjects
import spock.lang.Specification

import static com.google.testing.compile.Compiler.javac

class MutableProcessorSpec extends Specification {

    def 'generates mutable implementation for @Mutable interface with single getter'() {
        given:
        def source = JavaFileObjects.forSourceString('test.Person', '''\
            package test;
            import io.github.joke.objects.Mutable;
            @Mutable
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
        generated.contains('public class PersonImpl implements Person')
        generated.contains('private String firstName')
        !generated.contains('private final String firstName')
        generated.contains('public PersonImpl()')
        generated.contains('public PersonImpl(String firstName)')
        generated.contains('this.firstName = firstName')
        generated.contains('public String getFirstName()')
        generated.contains('return this.firstName')
        generated.contains('public void setFirstName(String firstName)')
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :processor:test --tests 'io.github.joke.objects.MutableProcessorSpec'`
Expected: FAIL — compilation fails because `@Mutable` is not processed

**Step 3: Extend `ClassModel` with `declaredSetters`**

Add to `processor/src/main/java/io/github/joke/objects/strategy/ClassModel.java`:

Add this import and field:
```java
import javax.lang.model.element.ExecutableElement;
```

Add field:
```java
private final List<ExecutableElement> declaredSetters = new ArrayList<>();
```

Add accessor:
```java
public List<ExecutableElement> getDeclaredSetters() {
    return declaredSetters;
}
```

**Step 4: Create `package-info.java` for mutable package**

`processor/src/main/java/io/github/joke/objects/mutable/package-info.java`:

```java
@org.jspecify.annotations.NullMarked
package io.github.joke.objects.mutable;
```

**Step 5: Create `MutablePropertyDiscoveryStrategy`**

`processor/src/main/java/io/github/joke/objects/mutable/MutablePropertyDiscoveryStrategy.java`:

```java
package io.github.joke.objects.mutable;

import io.github.joke.objects.strategy.ClassModel;
import io.github.joke.objects.strategy.GenerationStrategy;
import io.github.joke.objects.strategy.PropertyUtils;
import javax.annotation.processing.Messager;
import javax.inject.Inject;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

public class MutablePropertyDiscoveryStrategy implements GenerationStrategy {

    private final Messager messager;

    @Inject
    MutablePropertyDiscoveryStrategy(Messager messager) {
        this.messager = messager;
    }

    @Override
    public void generate(TypeElement source, ClassModel model) {
        for (ExecutableElement method :
                ElementFilter.methodsIn(source.getEnclosedElements())) {
            if (PropertyUtils.isGetterMethod(method)) {
                model.getProperties().add(PropertyUtils.extractProperty(method));
            } else if (PropertyUtils.isSetterMethod(method)) {
                model.getDeclaredSetters().add(method);
            } else {
                reportError(method, model);
            }
        }
    }

    private void reportError(ExecutableElement method, ClassModel model) {
        if (!method.getParameters().isEmpty()) {
            messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Methods in @Mutable interfaces must have no parameters"
                            + " (except setters)",
                    method);
        } else if (method.getReturnType().getKind() == TypeKind.VOID) {
            messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Void methods in @Mutable interfaces"
                            + " must follow set* naming convention",
                    method);
        } else {
            messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Methods in @Mutable interfaces must follow"
                            + " get*/is*/set* naming convention",
                    method);
        }
        model.setHasErrors(true);
    }
}
```

**Step 6: Create `SetterValidationStrategy`**

`processor/src/main/java/io/github/joke/objects/mutable/SetterValidationStrategy.java`:

```java
package io.github.joke.objects.mutable;

import com.palantir.javapoet.TypeName;
import io.github.joke.objects.strategy.ClassModel;
import io.github.joke.objects.strategy.GenerationStrategy;
import io.github.joke.objects.strategy.Property;
import io.github.joke.objects.strategy.PropertyUtils;
import javax.annotation.processing.Messager;
import javax.inject.Inject;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

public class SetterValidationStrategy implements GenerationStrategy {

    private final Messager messager;

    @Inject
    SetterValidationStrategy(Messager messager) {
        this.messager = messager;
    }

    @Override
    public void generate(TypeElement source, ClassModel model) {
        for (ExecutableElement setter : model.getDeclaredSetters()) {
            String setterName = setter.getSimpleName().toString();
            String expectedField =
                    Character.toLowerCase(setterName.charAt(3))
                            + setterName.substring(4);
            TypeName paramType =
                    TypeName.get(setter.getParameters().get(0).asType());

            boolean matched = false;
            for (Property property : model.getProperties()) {
                if (property.getFieldName().equals(expectedField)
                        && property.getType().equals(paramType)) {
                    matched = true;
                    break;
                }
            }

            if (!matched) {
                messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "Setter " + setterName
                                + " does not match any getter-derived property",
                        setter);
                model.setHasErrors(true);
            }
        }
    }
}
```

**Step 7: Create `MutableFieldStrategy`**

`processor/src/main/java/io/github/joke/objects/mutable/MutableFieldStrategy.java`:

```java
package io.github.joke.objects.mutable;

import com.palantir.javapoet.FieldSpec;
import io.github.joke.objects.strategy.ClassModel;
import io.github.joke.objects.strategy.GenerationStrategy;
import io.github.joke.objects.strategy.Property;
import javax.inject.Inject;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public class MutableFieldStrategy implements GenerationStrategy {

    @Inject
    MutableFieldStrategy() {}

    @Override
    public void generate(TypeElement source, ClassModel model) {
        for (Property property : model.getProperties()) {
            FieldSpec field =
                    FieldSpec.builder(
                                    property.getType(),
                                    property.getFieldName(),
                                    Modifier.PRIVATE)
                            .build();
            model.getFields().add(field);
        }
    }
}
```

**Step 8: Create `SetterStrategy`**

`processor/src/main/java/io/github/joke/objects/mutable/SetterStrategy.java`:

```java
package io.github.joke.objects.mutable;

import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import io.github.joke.objects.strategy.ClassModel;
import io.github.joke.objects.strategy.GenerationStrategy;
import io.github.joke.objects.strategy.Property;
import io.github.joke.objects.strategy.PropertyUtils;
import javax.inject.Inject;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public class SetterStrategy implements GenerationStrategy {

    @Inject
    SetterStrategy() {}

    @Override
    public void generate(TypeElement source, ClassModel model) {
        for (Property property : model.getProperties()) {
            MethodSpec setter =
                    MethodSpec.methodBuilder(
                                    PropertyUtils.setterNameForField(
                                            property.getFieldName()))
                            .addModifiers(Modifier.PUBLIC)
                            .returns(void.class)
                            .addParameter(
                                    ParameterSpec.builder(
                                                    property.getType(),
                                                    property.getFieldName())
                                            .build())
                            .addStatement(
                                    "this.$N = $N",
                                    property.getFieldName(),
                                    property.getFieldName())
                            .build();
            model.getMethods().add(setter);
        }
    }
}
```

**Step 9: Create `MutableConstructorStrategy`**

`processor/src/main/java/io/github/joke/objects/mutable/MutableConstructorStrategy.java`:

```java
package io.github.joke.objects.mutable;

import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import io.github.joke.objects.strategy.ClassModel;
import io.github.joke.objects.strategy.GenerationStrategy;
import io.github.joke.objects.strategy.Property;
import javax.inject.Inject;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public class MutableConstructorStrategy implements GenerationStrategy {

    @Inject
    MutableConstructorStrategy() {}

    @Override
    public void generate(TypeElement source, ClassModel model) {
        MethodSpec noArgs =
                MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .build();
        model.getMethods().add(noArgs);

        if (!model.getProperties().isEmpty()) {
            MethodSpec.Builder allArgs =
                    MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);

            for (Property property : model.getProperties()) {
                allArgs.addParameter(
                        ParameterSpec.builder(
                                        property.getType(),
                                        property.getFieldName())
                                .build());
                allArgs.addStatement(
                        "this.$N = $N",
                        property.getFieldName(),
                        property.getFieldName());
            }

            model.getMethods().add(allArgs.build());
        }
    }
}
```

**Step 10: Create `MutableGenerator`**

`processor/src/main/java/io/github/joke/objects/mutable/MutableGenerator.java`:

```java
package io.github.joke.objects.mutable;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import io.github.joke.objects.immutable.AnalysisPhase;
import io.github.joke.objects.immutable.GenerationPhase;
import io.github.joke.objects.strategy.ClassModel;
import io.github.joke.objects.strategy.GenerationStrategy;
import java.io.IOException;
import java.util.Set;
import javax.annotation.processing.Filer;
import javax.inject.Inject;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public class MutableGenerator {

    private final Set<GenerationStrategy> analysisStrategies;
    private final Set<GenerationStrategy> generationStrategies;
    private final Filer filer;

    @Inject
    MutableGenerator(
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

**Step 11: Create `MutableSubcomponent`**

`processor/src/main/java/io/github/joke/objects/mutable/MutableSubcomponent.java`:

```java
package io.github.joke.objects.mutable;

import dagger.Subcomponent;

@Subcomponent(modules = MutableModule.class)
public interface MutableSubcomponent {

    MutableGenerator generator();

    @Subcomponent.Factory
    interface Factory {
        MutableSubcomponent create();
    }
}
```

**Step 12: Create `MutableModule`**

`processor/src/main/java/io/github/joke/objects/mutable/MutableModule.java`:

```java
package io.github.joke.objects.mutable;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import dagger.multibindings.Multibinds;
import io.github.joke.objects.immutable.AnalysisPhase;
import io.github.joke.objects.immutable.GenerationPhase;
import io.github.joke.objects.strategy.ClassStructureStrategy;
import io.github.joke.objects.strategy.GenerationStrategy;
import io.github.joke.objects.strategy.GetterStrategy;
import java.util.Set;

@Module
public interface MutableModule {

    @Multibinds
    @AnalysisPhase
    Set<GenerationStrategy> analysisStrategies();

    @Binds
    @IntoSet
    @AnalysisPhase
    GenerationStrategy mutablePropertyDiscovery(
            MutablePropertyDiscoveryStrategy impl);

    @Binds
    @IntoSet
    @AnalysisPhase
    GenerationStrategy setterValidation(SetterValidationStrategy impl);

    @Binds
    @IntoSet
    @GenerationPhase
    GenerationStrategy classStructure(ClassStructureStrategy impl);

    @Binds
    @IntoSet
    @GenerationPhase
    GenerationStrategy mutableField(MutableFieldStrategy impl);

    @Binds
    @IntoSet
    @GenerationPhase
    GenerationStrategy getter(GetterStrategy impl);

    @Binds
    @IntoSet
    @GenerationPhase
    GenerationStrategy setter(SetterStrategy impl);

    @Binds
    @IntoSet
    @GenerationPhase
    GenerationStrategy mutableConstructor(MutableConstructorStrategy impl);
}
```

**Step 13: Update `ProcessorModule` to register `MutableSubcomponent`**

Modify `processor/src/main/java/io/github/joke/objects/component/ProcessorModule.java` — add `MutableSubcomponent` to the `subcomponents` list:

```java
package io.github.joke.objects.component;

import dagger.Module;
import dagger.Provides;
import io.github.joke.objects.immutable.ImmutableSubcomponent;
import io.github.joke.objects.mutable.MutableSubcomponent;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;

@Module(subcomponents = {ImmutableSubcomponent.class, MutableSubcomponent.class})
public class ProcessorModule {

    private final ProcessingEnvironment processingEnvironment;

    public ProcessorModule(ProcessingEnvironment processingEnvironment) {
        this.processingEnvironment = processingEnvironment;
    }

    @Provides
    Filer filer() {
        return processingEnvironment.getFiler();
    }

    @Provides
    Messager messager() {
        return processingEnvironment.getMessager();
    }
}
```

**Step 14: Update `ProcessorComponent` to expose `MutableSubcomponent.Factory`**

Replace `processor/src/main/java/io/github/joke/objects/component/ProcessorComponent.java`:

```java
package io.github.joke.objects.component;

import dagger.Component;
import io.github.joke.objects.immutable.ImmutableSubcomponent;
import io.github.joke.objects.mutable.MutableSubcomponent;

@Component(modules = ProcessorModule.class)
public interface ProcessorComponent {
    ImmutableSubcomponent.Factory immutable();

    MutableSubcomponent.Factory mutable();
}
```

**Step 15: Update `ObjectsProcessor` to handle `@Mutable`**

Replace `processor/src/main/java/io/github/joke/objects/ObjectsProcessor.java`:

```java
package io.github.joke.objects;

import com.google.auto.service.AutoService;
import io.github.joke.objects.component.DaggerProcessorComponent;
import io.github.joke.objects.component.ProcessorModule;
import io.github.joke.objects.immutable.ImmutableSubcomponent;
import io.github.joke.objects.mutable.MutableSubcomponent;
import java.io.IOException;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
public class ObjectsProcessor extends AbstractProcessor {

    private ImmutableSubcomponent immutableSubcomponent;
    private MutableSubcomponent mutableSubcomponent;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        var component = DaggerProcessorComponent.builder()
                .processorModule(new ProcessorModule(processingEnv))
                .build();
        immutableSubcomponent = component.immutable().create();
        mutableSubcomponent = component.mutable().create();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(
                Immutable.class.getCanonicalName(),
                Mutable.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(
            Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            String annotationName = annotation.getQualifiedName().toString();

            for (Element element :
                    roundEnv.getElementsAnnotatedWith(annotation)) {
                if (element.getKind() != ElementKind.INTERFACE) {
                    processingEnv
                            .getMessager()
                            .printMessage(
                                    Diagnostic.Kind.ERROR,
                                    "@" + annotation.getSimpleName()
                                            + " can only be applied"
                                            + " to interfaces",
                                    element);
                    continue;
                }
                try {
                    if (annotationName.equals(
                            Immutable.class.getCanonicalName())) {
                        immutableSubcomponent
                                .generator()
                                .generate((TypeElement) element);
                    } else if (annotationName.equals(
                            Mutable.class.getCanonicalName())) {
                        mutableSubcomponent
                                .generator()
                                .generate((TypeElement) element);
                    }
                } catch (IOException e) {
                    processingEnv
                            .getMessager()
                            .printMessage(
                                    Diagnostic.Kind.ERROR,
                                    "Failed to generate implementation: "
                                            + e.getMessage(),
                                    element);
                }
            }
        }
        return false;
    }
}
```

Note: The error message for non-interface types now uses the annotation's simple name dynamically (e.g., `@Immutable can only be applied to interfaces` or `@Mutable can only be applied to interfaces`). This means existing `@Immutable` tests that check for `@Immutable can only be applied to interfaces` will still pass.

**Step 16: Run all tests**

Run: `./gradlew :processor:test`
Expected: ALL 10 PASS (9 existing + 1 new mutable test)

**Step 17: Commit**

```bash
git add processor/src/main/java/io/github/joke/objects/ processor/src/test/groovy/io/github/joke/objects/MutableProcessorSpec.groovy
git commit -m "feat: add @Mutable annotation processing with setters and dual constructors"
```

---

### Task 3: Additional @Mutable happy path tests

Add tests for multiple getters, `is*` prefix, and declared setters in the interface.

**Files:**
- Modify: `processor/src/test/groovy/io/github/joke/objects/MutableProcessorSpec.groovy`

**Step 1: Add the tests**

Append to `MutableProcessorSpec.groovy`:

```groovy
def 'generates mutable fields and both constructors for multiple getters'() {
    given:
    def source = JavaFileObjects.forSourceString('test.Person', '''\
        package test;
        import io.github.joke.objects.Mutable;
        @Mutable
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
    generated.contains('private String firstName')
    generated.contains('private int age')
    !generated.contains('private final')
    generated.contains('public PersonImpl()')
    generated.contains('PersonImpl(String firstName, int age)')
    generated.contains('public void setFirstName(String firstName)')
    generated.contains('public void setAge(int age)')
}

def 'generates setter for boolean is* getter'() {
    given:
    def source = JavaFileObjects.forSourceString('test.Status', '''\
        package test;
        import io.github.joke.objects.Mutable;
        @Mutable
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
    generated.contains('private boolean active')
    generated.contains('public boolean isActive()')
    generated.contains('public void setActive(boolean active)')
}

def 'succeeds when interface declares matching setter'() {
    given:
    def source = JavaFileObjects.forSourceString('test.Person', '''\
        package test;
        import io.github.joke.objects.Mutable;
        @Mutable
        public interface Person {
            String getFirstName();
            void setFirstName(String firstName);
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
    generated.contains('public void setFirstName(String firstName)')
}

def 'generates only no-args constructor for empty interface'() {
    given:
    def source = JavaFileObjects.forSourceString('test.Empty', '''\
        package test;
        import io.github.joke.objects.Mutable;
        @Mutable
        public interface Empty {}
    ''')

    when:
    def compilation = javac()
        .withProcessors(new ObjectsProcessor())
        .compile(source)

    then:
    compilation.status() == Compilation.Status.SUCCESS

    and:
    def generated = compilation.generatedSourceFile('test.EmptyImpl')
        .get().getCharContent(true).toString()
    generated.contains('public class EmptyImpl implements Empty')
    generated.contains('public EmptyImpl()')
}
```

**Step 2: Run all tests**

Run: `./gradlew :processor:test`
Expected: ALL 14 PASS (9 immutable + 5 mutable)

**Step 3: Commit**

```bash
git add processor/src/test/groovy/io/github/joke/objects/MutableProcessorSpec.groovy
git commit -m "test: add happy path tests for @Mutable generation"
```

---

### Task 4: @Mutable validation error tests

Add tests for error cases: non-interface types, invalid method signatures, and mismatched declared setters.

**Files:**
- Modify: `processor/src/test/groovy/io/github/joke/objects/MutableProcessorSpec.groovy`

**Step 1: Add the validation tests**

Append to `MutableProcessorSpec.groovy`:

```groovy
def 'fails when @Mutable applied to a class'() {
    given:
    def source = JavaFileObjects.forSourceString('test.Person', '''\
        package test;
        import io.github.joke.objects.Mutable;
        @Mutable
        public class Person {}
    ''')

    when:
    def compilation = javac()
        .withProcessors(new ObjectsProcessor())
        .compile(source)

    then:
    compilation.status() == Compilation.Status.FAILURE
    compilation.errors().any {
        it.getMessage(null).contains('@Mutable can only be applied to interfaces')
    }
}

def 'fails when method name does not follow naming convention'() {
    given:
    def source = JavaFileObjects.forSourceString('test.Person', '''\
        package test;
        import io.github.joke.objects.Mutable;
        @Mutable
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
        it.getMessage(null).contains('must follow get*/is*/set* naming convention')
    }
}

def 'fails when declared setter does not match any property'() {
    given:
    def source = JavaFileObjects.forSourceString('test.Person', '''\
        package test;
        import io.github.joke.objects.Mutable;
        @Mutable
        public interface Person {
            String getFirstName();
            void setLastName(String lastName);
        }
    ''')

    when:
    def compilation = javac()
        .withProcessors(new ObjectsProcessor())
        .compile(source)

    then:
    compilation.status() == Compilation.Status.FAILURE
    compilation.errors().any {
        it.getMessage(null).contains('does not match any getter-derived property')
    }
}

def 'fails when declared setter has wrong parameter type'() {
    given:
    def source = JavaFileObjects.forSourceString('test.Person', '''\
        package test;
        import io.github.joke.objects.Mutable;
        @Mutable
        public interface Person {
            String getFirstName();
            void setFirstName(int wrongType);
        }
    ''')

    when:
    def compilation = javac()
        .withProcessors(new ObjectsProcessor())
        .compile(source)

    then:
    compilation.status() == Compilation.Status.FAILURE
    compilation.errors().any {
        it.getMessage(null).contains('does not match any getter-derived property')
    }
}
```

**Step 2: Run all tests**

Run: `./gradlew :processor:test`
Expected: ALL 18 PASS (9 immutable + 9 mutable)

**Step 3: Commit**

```bash
git add processor/src/test/groovy/io/github/joke/objects/MutableProcessorSpec.groovy
git commit -m "test: add validation error tests for @Mutable processing"
```
