# Changelog

## 1.0.0 (2026-10-17)


### Features

* accept abstract classes in CaffeinateProcessor with no-args constructor validation ([033c9ae](https://github.com/joke/caffeinate/commit/033c9aedb4a33fb23e648094941a2e1aaa04d296))
* add @Mutable annotation processing with setters and dual constructors ([a446d76](https://github.com/joke/caffeinate/commit/a446d7646512e3421cc68effd29cb425867581f5))
* add property discovery, field, getter, and constructor strategies ([738c5ec](https://github.com/joke/caffeinate/commit/738c5ec8ac82aa19685bb5af3c80caefd99cf34b))
* add super() call in constructors when source is abstract class ([7ef6d1d](https://github.com/joke/caffeinate/commit/7ef6d1d1a9b461dcc8eb3afcb0384f1541ef87c5))
* add superclass field to ClassModel and detect extends vs implements in ClassStructureStrategy ([279bc2e](https://github.com/joke/caffeinate/commit/279bc2e124f67f5e7ddc698c421d9bf9452949e8))
* add TypeHierarchyResolver and wire into discovery strategies ([bb8e444](https://github.com/joke/caffeinate/commit/bb8e444091d0edb3e000403fd82b382f18fdc3db))
* apply @Nullable annotations to generated constructor parameters ([dad5cd8](https://github.com/joke/caffeinate/commit/dad5cd8ad255df62ea570e6bec11800388a8861b))
* apply @Nullable annotations to generated fields ([d93e03d](https://github.com/joke/caffeinate/commit/d93e03d22e9adc113058f7714c4b5f22a9f2e5a8))
* apply @Nullable annotations to generated getters ([e10ab4c](https://github.com/joke/caffeinate/commit/e10ab4c1054b9a9a808647e146118c0259787e74))
* apply @Nullable annotations to generated setter parameters ([1854320](https://github.com/joke/caffeinate/commit/1854320f8653c11115f27cdbc5e92077f461f844))
* apply superclass from ClassModel in generators ([f7cef1a](https://github.com/joke/caffeinate/commit/f7cef1ae01244328b08ee9c51e96485eca18be48))
* enrich Property with nullable annotations list and extract in PropertyUtils ([98879a0](https://github.com/joke/caffeinate/commit/98879a02b8ba4c5869a4da75fcf0527217640609))
* implement core annotation processor for @Immutable interfaces ([5186c87](https://github.com/joke/caffeinate/commit/5186c87b9319db6874199b952e6d72b7acd73438))


### Bug Fixes

* guarantee setter validation runs after property discovery ([122846c](https://github.com/joke/caffeinate/commit/122846cf896833641b3907acce67d80b7c81e9f0))
* null-safe interface element cast in TypeHierarchyResolver and import TypeKind in PropertyDiscoveryStrategy ([b17ecb7](https://github.com/joke/caffeinate/commit/b17ecb7042f381874d8409e74d70db3e112e27e5))
