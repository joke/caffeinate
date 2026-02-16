# GitHub Project Setup & Rename to Caffeinate — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Rename the project from "Objects" to "Caffeinate" (packages, group ID, docs) and set up GitHub infrastructure (README, CI, Dependabot, settings).

**Architecture:** Full rename of Java packages from `io.github.joke.objects` to `io.github.joke.caffeinate`, class rename `ObjectsProcessor` → `CaffeinateProcessor`, then add GitHub Actions build workflow with mise, Dependabot, and probot/settings. Docs site and CLAUDE.md updated throughout.

**Tech Stack:** Gradle, mise, GitHub Actions (`jdkr/setup-mise`), Dependabot, probot/settings

---

### Task 1: Rename Java packages (annotations module)

The annotations module has source files under `annotations/src/main/java/io/github/joke/objects/`. Move them to `io/github/joke/caffeinate/` and update package declarations.

**Files:**
- Move: `annotations/src/main/java/io/github/joke/objects/` → `annotations/src/main/java/io/github/joke/caffeinate/`

**Step 1: Move the annotations directory**

```bash
cd /home/joke/Projects/joke/objects2
mkdir -p annotations/src/main/java/io/github/joke/caffeinate/customize
git mv annotations/src/main/java/io/github/joke/objects/package-info.java annotations/src/main/java/io/github/joke/caffeinate/
git mv annotations/src/main/java/io/github/joke/objects/Immutable.java annotations/src/main/java/io/github/joke/caffeinate/
git mv annotations/src/main/java/io/github/joke/objects/Mutable.java annotations/src/main/java/io/github/joke/caffeinate/
git mv annotations/src/main/java/io/github/joke/objects/Target.java annotations/src/main/java/io/github/joke/caffeinate/
git mv annotations/src/main/java/io/github/joke/objects/customize/package-info.java annotations/src/main/java/io/github/joke/caffeinate/customize/
git mv annotations/src/main/java/io/github/joke/objects/customize/Name.java annotations/src/main/java/io/github/joke/caffeinate/customize/
git mv annotations/src/main/java/io/github/joke/objects/customize/ToString.java annotations/src/main/java/io/github/joke/caffeinate/customize/
git mv annotations/src/main/java/io/github/joke/objects/customize/Annotate.java annotations/src/main/java/io/github/joke/caffeinate/customize/
```

**Step 2: Update package declarations in all moved annotation files**

Find and replace `io.github.joke.objects` → `io.github.joke.caffeinate` in every file under `annotations/src/main/java/io/github/joke/caffeinate/`. This includes:
- `package io.github.joke.objects;` → `package io.github.joke.caffeinate;`
- `package io.github.joke.objects.customize;` → `package io.github.joke.caffeinate.customize;`
- Any import referencing `io.github.joke.objects` (e.g., in `ToString.java` line 3: `import static io.github.joke.objects.customize.ToString.Style.STRING_JOINER;`)

**Step 3: Remove empty old directories**

```bash
rm -rf annotations/src/main/java/io/github/joke/objects
```

**Step 4: Do NOT commit yet** — the build will be broken until the processor module is also renamed.

---

### Task 2: Rename Java packages (processor module)

The processor module has source files under `processor/src/main/java/io/github/joke/objects/` in subpackages: root, `component/`, `strategy/`, `immutable/`, `mutable/`. Move everything and update all package declarations and imports.

**Files:**
- Move: `processor/src/main/java/io/github/joke/objects/` → `processor/src/main/java/io/github/joke/caffeinate/`

**Step 1: Move the processor directories**

```bash
cd /home/joke/Projects/joke/objects2
mkdir -p processor/src/main/java/io/github/joke/caffeinate/{component,strategy,immutable,mutable}

# Root package
git mv processor/src/main/java/io/github/joke/objects/package-info.java processor/src/main/java/io/github/joke/caffeinate/
git mv processor/src/main/java/io/github/joke/objects/ObjectsProcessor.java processor/src/main/java/io/github/joke/caffeinate/CaffeinateProcessor.java

# Component package
git mv processor/src/main/java/io/github/joke/objects/component/package-info.java processor/src/main/java/io/github/joke/caffeinate/component/
git mv processor/src/main/java/io/github/joke/objects/component/ProcessorComponent.java processor/src/main/java/io/github/joke/caffeinate/component/
git mv processor/src/main/java/io/github/joke/objects/component/ProcessorModule.java processor/src/main/java/io/github/joke/caffeinate/component/

# Strategy package (all files)
for f in processor/src/main/java/io/github/joke/objects/strategy/*.java; do
    git mv "$f" processor/src/main/java/io/github/joke/caffeinate/strategy/
done

# Immutable package (all files)
for f in processor/src/main/java/io/github/joke/objects/immutable/*.java; do
    git mv "$f" processor/src/main/java/io/github/joke/caffeinate/immutable/
done

# Mutable package (all files)
for f in processor/src/main/java/io/github/joke/objects/mutable/*.java; do
    git mv "$f" processor/src/main/java/io/github/joke/caffeinate/mutable/
done
```

Note: `ObjectsProcessor.java` is renamed to `CaffeinateProcessor.java` in the move.

**Step 2: Bulk-replace package names and imports in all processor source files**

In every `.java` file under `processor/src/main/java/io/github/joke/caffeinate/`, replace:
- `io.github.joke.objects` → `io.github.joke.caffeinate` (covers package declarations and imports)
- `ObjectsProcessor` → `CaffeinateProcessor` (class name and references)

Specific files to watch:
- `CaffeinateProcessor.java` (formerly ObjectsProcessor): update class declaration, all `io.github.joke.objects.*` imports
- `ProcessorComponent.java`: imports from immutable/mutable subpackages
- `ProcessorModule.java`: imports from immutable/mutable subpackages
- `ImmutableGenerator.java`: imports from strategy subpackage
- `MutableGenerator.java`: imports from strategy and immutable subpackages
- `ImmutableModule.java`: imports from strategy subpackage
- `MutableModule.java`: imports from strategy and immutable subpackages
- All mutable strategy files: imports from strategy subpackage

**Step 3: Remove empty old directories**

```bash
rm -rf processor/src/main/java/io/github/joke/objects
```

**Step 4: Do NOT commit yet** — tests still reference old packages.

---

### Task 3: Rename test packages and update test sources

The Spock specs live under `processor/src/test/groovy/io/github/joke/objects/` and contain both their own package declarations and inline Java source strings that import `io.github.joke.objects.*`.

**Files:**
- Move: `processor/src/test/groovy/io/github/joke/objects/` → `processor/src/test/groovy/io/github/joke/caffeinate/`

**Step 1: Move test files**

```bash
cd /home/joke/Projects/joke/objects2
mkdir -p processor/src/test/groovy/io/github/joke/caffeinate
git mv processor/src/test/groovy/io/github/joke/objects/ImmutableProcessorSpec.groovy processor/src/test/groovy/io/github/joke/caffeinate/
git mv processor/src/test/groovy/io/github/joke/objects/MutableProcessorSpec.groovy processor/src/test/groovy/io/github/joke/caffeinate/
```

**Step 2: Update package declarations and imports in test files**

In both spec files, replace:
- `package io.github.joke.objects` → `package io.github.joke.caffeinate`
- `import io.github.joke.objects.Immutable` → `import io.github.joke.caffeinate.Immutable` (in inline Java source strings)
- `import io.github.joke.objects.Mutable` → `import io.github.joke.caffeinate.Mutable` (in inline Java source strings)
- `new ObjectsProcessor()` → `new CaffeinateProcessor()` (in test setup code)
- Any `import io.github.joke.objects.ObjectsProcessor` → `import io.github.joke.caffeinate.CaffeinateProcessor`

**IMPORTANT:** The test files contain inline Java source strings (inside `JavaFileObjects.forSourceString(...)` calls) that have their own `import` statements. These are string literals — make sure to update the import lines inside the source strings too, not just the Groovy-level imports.

**Step 3: Remove empty old directories**

```bash
rm -rf processor/src/test/groovy/io/github/joke/objects
```

---

### Task 4: Update build configuration and verify

**Files:**
- Modify: `build.gradle` (line 9)
- Modify: `settings.gradle` (line 13)

**Step 1: Update `build.gradle`**

Change line 9:
```groovy
group 'io.github.joke.caffeinate'
```

**Step 2: Update `settings.gradle`**

Change line 13:
```groovy
rootProject.name = 'caffeinate'
```

**Step 3: Clean and build**

Run: `cd /home/joke/Projects/joke/objects2 && ./gradlew clean build 2>&1`
Expected: BUILD SUCCESSFUL with all tests passing (18 tests: 9 Immutable + 9 Mutable)

**Step 4: Commit the entire rename**

```bash
git add -A
git commit -m "refactor: rename project from Objects to Caffeinate

Rename Java packages from io.github.joke.objects to
io.github.joke.caffeinate. Rename ObjectsProcessor to
CaffeinateProcessor. Update group ID and rootProject.name."
```

---

### Task 5: Update documentation site

**Files:**
- Modify: `docs/mkdocs.yml`
- Modify: `docs/docs/index.md`
- Modify: `docs/docs/getting-started.md`
- Modify: `docs/docs/immutable.md`
- Modify: `docs/docs/mutable.md`
- Modify: `docs/docs/reference.md`

**Step 1: Update `docs/mkdocs.yml`**

- Line 1: `site_name: Objects` → `site_name: Caffeinate`
- Line 2: `site_description: An annotation processor for generating Java boilerplate` (keep as-is or update)

**Step 2: Update `docs/docs/index.md`**

- Line 1: `# Objects` → `# Caffeinate`
- Line 3: `Objects is a Java annotation processor` → `Caffeinate is a Java annotation processor`
- Line 7: Update intro sentence to mention "Caffeinate" instead of "Objects"
- Line 14: `import io.github.joke.objects.Immutable;` → `import io.github.joke.caffeinate.Immutable;`

**Step 3: Update `docs/docs/getting-started.md`**

Replace all occurrences of:
- `Objects consists of` → `Caffeinate consists of`
- `io.github.joke.objects:annotations` → `io.github.joke.caffeinate:annotations` (appears in text, Gradle, and Maven blocks)
- `io.github.joke.objects:processor` → `io.github.joke.caffeinate:processor`
- `<groupId>io.github.joke.objects</groupId>` → `<groupId>io.github.joke.caffeinate</groupId>` (2 occurrences)
- `import io.github.joke.objects.Immutable;` → `import io.github.joke.caffeinate.Immutable;`

**Step 4: Update `docs/docs/immutable.md`**

- `import io.github.joke.objects.Immutable;` → `import io.github.joke.caffeinate.Immutable;`

**Step 5: Update `docs/docs/mutable.md`**

- `import io.github.joke.objects.Mutable;` → `import io.github.joke.caffeinate.Mutable;`

**Step 6: Update `docs/docs/reference.md`**

- Line 3: `in the Objects annotation processor` → `in the Caffeinate annotation processor`

**Step 7: Verify docs build**

Run: `cd /home/joke/Projects/joke/objects2/docs && mkdocs build --strict 2>&1`
Expected: Builds successfully

**Step 8: Commit**

```bash
git add docs/
git commit -m "docs: update documentation site for Caffeinate rename"
```

---

### Task 6: Update CLAUDE.md

**Files:**
- Modify: `CLAUDE.md`

**Step 1: Update all references**

- Line 5: `Objects is a Java annotation processor` → `Caffeinate is a Java annotation processor`
- Line 42: `Group ID: \`io.github.joke.objects\`` → `Group ID: \`io.github.joke.caffeinate\``
- Line 43: `Package root: \`io.github.joke.objects\`` → `Package root: \`io.github.joke.caffeinate\``

**Step 2: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: update CLAUDE.md for Caffeinate rename"
```

---

### Task 7: Write README.md

**Files:**
- Modify: `README.md`

**Step 1: Write concise README**

```markdown
# Caffeinate

A Java annotation processor that generates implementation classes from annotated interfaces — in the style of Lombok and Immutables.

[![Build](https://github.com/joke/objects2/actions/workflows/build.yml/badge.svg)](https://github.com/joke/objects2/actions/workflows/build.yml)

## Quick Example

```java
import io.github.joke.caffeinate.Immutable;

@Immutable
public interface Person {
    String getFirstName();
    int getAge();
}
```

At compile time, Caffeinate generates `PersonImpl` with `private final` fields, an all-args constructor, and getter methods.

## Documentation

Full documentation is available at [joke.github.io/objects2](https://joke.github.io/objects2/).
```

**Step 2: Commit**

```bash
git add README.md
git commit -m "docs: write concise README for Caffeinate"
```

---

### Task 8: Build workflow

**Files:**
- Create: `.github/workflows/build.yml`

**Step 1: Create the workflow**

```yaml
name: Build

on:
  push:
    branches: [main]
  pull_request:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: jdkr/setup-mise@v1
      - run: ./gradlew build
```

Key points:
- Job name is `build` (matches the required status check in settings.yml)
- `jdkr/setup-mise` reads `.mise.toml` automatically and installs `java = "liberica-25.0.2+12"`
- `./gradlew build` runs compile, test, ErrorProne, NullAway

**Step 2: Commit**

```bash
git add .github/workflows/build.yml
git commit -m "ci: add build workflow with mise and Gradle"
```

---

### Task 9: Dependabot configuration

**Files:**
- Create: `.github/dependabot.yml`

**Step 1: Create the config**

```yaml
version: 2
updates:
  - package-ecosystem: github-actions
    directory: /
    schedule:
      interval: weekly
    commit-message:
      prefix: chore
      include: scope
  - package-ecosystem: gradle
    directory: /
    schedule:
      interval: weekly
    commit-message:
      prefix: chore
      include: scope
```

**Step 2: Commit**

```bash
git add .github/dependabot.yml
git commit -m "chore: add Dependabot configuration for Actions and Gradle"
```

---

### Task 10: Repository settings

**Files:**
- Create: `.github/settings.yml`

**Step 1: Create the settings file**

Based on spock-deepmock, adapted for this project:

```yaml
# These settings are synced to GitHub by https://probot.github.io/apps/settings/
repository:
  name: objects2
  description: Caffeinate — a Java annotation processor that generates implementation classes from annotated interfaces
  topics: annotation-processor, java, code-generation, immutable, mutable
  private: false
  has_issues: true
  has_projects: false
  has_wiki: false
  has_downloads: true
  default_branch: main
  allow_squash_merge: false
  allow_merge_commit: false
  allow_rebase_merge: true
  allow_auto_merge: true
  allow_update_branch: true
  delete_branch_on_merge: true
  enable_automated_security_fixes: true
  enable_vulnerability_alerts: true

branches:
  - name: main
    protection:
      required_pull_request_reviews:
        required_approving_review_count: 1
        dismiss_stale_reviews: true
        require_code_owner_reviews: true
      required_status_checks:
        strict: true
        checks:
          - context: build
            app_id: 15368
      enforce_admins: false
      required_linear_history: true
      required_conversation_resolution: true
      restrictions:
```

Note: `repository.name` stays `objects2` (the actual GitHub repo name). The description includes "Caffeinate" as the branding.

**Step 2: Commit**

```bash
git add .github/settings.yml
git commit -m "chore: add repository settings via probot/settings"
```

---

### Task 11: Final verification

**Step 1: Full build**

Run: `cd /home/joke/Projects/joke/objects2 && ./gradlew clean build 2>&1`
Expected: BUILD SUCCESSFUL, all 18 tests pass

**Step 2: Docs build**

Run: `cd /home/joke/Projects/joke/objects2/docs && mkdocs build --strict 2>&1`
Expected: Builds with no warnings

**Step 3: Verify no stale references**

Search for any remaining `io.github.joke.objects` references in tracked files:

```bash
git grep 'io\.github\.joke\.objects' -- ':!docs/plans/'
```

Expected: No matches (plan docs are excluded — they're historical).

**Step 4: Verify git status is clean**

```bash
git status
```

Expected: Only untracked IDE files (.project, .settings, etc.) and the untracked tests/ module.
