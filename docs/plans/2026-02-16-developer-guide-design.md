# Developer Guide Design

## Goal

Create a developer manual for the Objects annotation processor, hosted as GitHub Pages, with input/output source code examples and Java syntax highlighting.

## Technology

- **MkDocs** with **Material for MkDocs** theme
- **GitHub Pages** deployment via GitHub Actions
- **Pygments** for Java syntax highlighting (built into Material)

## Site Structure

```
docs/
├── mkdocs.yml
└── docs/
    ├── index.md           # Home — what Objects is, quick example
    ├── getting-started.md # Dependency setup (Gradle/Maven)
    ├── immutable.md       # @Immutable guide with input → output examples
    ├── mutable.md         # @Mutable guide with input → output examples
    └── reference.md       # All annotations, validation rules, error messages
```

## Page Content

### index.md
- One-sentence description of what Objects does
- Quick before/after code example showing an `@Immutable` interface and its generated implementation
- Links to getting-started and annotation guides

### getting-started.md
- Gradle dependency setup (annotation processor + annotations)
- Maven dependency setup
- Minimum Java version requirement (11)

### immutable.md
- What `@Immutable` generates: final fields, all-args constructor, getters
- Input interface example → generated output class
- Multiple properties example
- `boolean is*()` getter convention
- What happens with `@ToString` customization

### mutable.md
- What `@Mutable` generates: non-final fields, no-args + all-args constructors, getters, setters
- Input interface example → generated output class
- Declaring setters in the interface (optional, validated if present)
- `boolean is*()` getter convention with setters

### reference.md
- Table of all annotations with brief descriptions
- Naming conventions for getters/setters
- Validation rules and their error messages
- Generated class naming convention (`<Interface>Impl`)

## Content Style

Each annotation guide uses a consistent pattern:
1. Brief description of what the annotation does
2. Input interface (annotated)
3. Generated output class (complete, compilable)
4. Variations and edge cases

Code examples use fenced blocks with `java` language tag. Input and output are shown side-by-side or sequentially with clear labels.
