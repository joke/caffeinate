package io.github.joke.caffeinate.immutable;

import dagger.Subcomponent;

@Subcomponent(modules = ImmutableModule.class)
public interface ImmutableSubcomponent {

    ImmutableGenerator generator();

    @Subcomponent.Factory
    interface Factory {
        ImmutableSubcomponent create();
    }
}
