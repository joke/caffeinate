package io.github.joke.caffeinate.mutable;

import dagger.Subcomponent;

@Subcomponent(modules = MutableModule.class)
public interface MutableSubcomponent {

    MutableGenerator generator();

    @Subcomponent.Factory
    interface Factory {
        MutableSubcomponent create();
    }
}
