# GitHub Actions & Repository Settings Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a CI build workflow and repository-settings app configuration to the Caffeinate project.

**Architecture:** Two static YAML files in `.github/`. The build workflow uses mise for Java setup and Gradle for the build. The settings file configures repo metadata and branch protection via the probot settings app.

**Tech Stack:** GitHub Actions, mise, Gradle, probot/settings

---

### Task 1: Create CI build workflow

**Files:**
- Create: `.github/workflows/build.yml`

**Step 1: Create the workflow file**

```yaml
name: build

on:
  pull_request:
  push:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: jdkr/setup-mise@v1
      - uses: gradle/actions/setup-gradle@v4
      - run: ./gradlew build
```

**Step 2: Commit**

```bash
git add .github/workflows/build.yml
git commit -m "ci: add GitHub Actions build workflow"
```

---

### Task 2: Create repository settings

**Files:**
- Create: `.github/settings.yml`

**Step 1: Create the settings file**

```yaml
# These settings are synced to GitHub by https://probot.github.io/apps/settings/
repository:
  name: caffeinate
  description: A Java annotation processor that generates implementation classes from annotated interfaces
  topics: java, annotation-processor, code-generation
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

**Step 2: Commit**

```bash
git add .github/settings.yml
git commit -m "ci: add repository settings for probot/settings app"
```
