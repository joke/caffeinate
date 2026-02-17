# README, Badges & Repo Polish Design

## Goal

Add badges, LICENSE file, update README with Caffeinate branding, link GitHub Pages on the repo, and fix the docs site URL.

## Deliverables

### 1. README.md

Replace current one-liner with Caffeinate-branded minimal README:

```markdown
# Caffeinate

[![build](https://github.com/joke/caffeinate/actions/workflows/build.yml/badge.svg)](https://github.com/joke/caffeinate/actions/workflows/build.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

A Java annotation processor that generates implementation classes from annotated interfaces.

[Documentation](https://joke.github.io/caffeinate/)
```

### 2. LICENSE

Standard Apache License 2.0 text. Copyright 2025 joke.

### 3. settings.yml update

Add `homepage` field to link GitHub Pages on the repo:

```yaml
repository:
  homepage: https://joke.github.io/caffeinate/
```

### 4. mkdocs.yml fix

Update `site_url` from `https://joke.github.io/objects2/` to `https://joke.github.io/caffeinate/`.

## Decisions

- Minimal README — docs site handles installation/usage details
- Apache 2.0 license to match the Java library ecosystem (Dagger, Guava, etc.)
- GitHub repo name is `joke/caffeinate`, docs URL is `https://joke.github.io/caffeinate/`
