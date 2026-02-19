package io.github.joke.caffeinate.component;

import dagger.Module;
import dagger.Provides;
import io.github.joke.caffeinate.immutable.ImmutableSubcomponent;
import io.github.joke.caffeinate.mutable.MutableSubcomponent;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.util.Types;

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

    @Provides
    Types types() {
        return processingEnvironment.getTypeUtils();
    }
}
