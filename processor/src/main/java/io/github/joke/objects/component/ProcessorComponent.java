package io.github.joke.objects.component;

import dagger.Component;
import io.github.joke.objects.immutable.ImmutableSubcomponent;
import io.github.joke.objects.mutable.MutableSubcomponent;

@Component(modules = ProcessorModule.class)
public interface ProcessorComponent {
    ImmutableSubcomponent.Factory immutable();

    MutableSubcomponent.Factory mutable();
}
