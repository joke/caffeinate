package io.github.joke.caffeinate.component;

import dagger.Component;
import io.github.joke.caffeinate.immutable.ImmutableSubcomponent;
import io.github.joke.caffeinate.mutable.MutableSubcomponent;

@Component(modules = ProcessorModule.class)
public interface ProcessorComponent {
    ImmutableSubcomponent.Factory immutable();

    MutableSubcomponent.Factory mutable();
}
