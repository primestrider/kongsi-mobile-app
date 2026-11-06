#!/usr/bin/env bash
# Removes the example features from a checkout and leaves a runnable app (placeholder Home, one tab).
# Only the seven examples listed below are touched; your own feature modules stay.
#
#   bash scripts/remove-examples.sh [--keep <keep-dir>] [dir]     # dir default: the current directory
#
# Run it on a copy first (e.g. `git worktree add ../no-examples`) and build there:
#   ./gradlew test assembleDevDebug
# What it does:
#   1. deletes feature/<example>/ for each example and its include lines in settings.gradle.kts;
#   2. deletes every `// region example:<feature>` ... `// endregion` block in Kotlin and Gradle files
#      (nested regions included) - that is all the wiring :app has for the examples;
#   3. deletes :app tests that exercise the examples: tests in a `feature.*` package, tests importing an example
#      module's package, and tests that start with the line comment `// remove-examples: ...` (add it to an :app test
#      that drives example screens);
#   4. runs ktlintFormat, which drops the imports the deleted blocks used.
# With --keep <keep-dir> (a path inside the project, e.g. .examples), everything it deletes is copied there first, as
# reference code: the feature modules (without build outputs), the :app files that had example regions (whole, before
# stripping) and the deleted :app tests, each at its original path. The keep dir is added to .gitignore and is not part
# of the Gradle build.
set -euo pipefail

EXAMPLES=(catalog products todos account settings devices animation gestures)

keep=""
if [ "${1:-}" = "--keep" ]; then
    keep="${2:?--keep needs a directory, e.g. --keep .examples}"
    shift 2
fi
root="${1:-.}"
cd "$root"
[ -f settings.gradle.kts ] || { echo "not a project root: $(pwd)" >&2; exit 1; }

if [ -n "$keep" ]; then
    keep="${keep%/}"
    case "$keep" in
        /* | [A-Za-z]:* | .. | ../* | */../* | */..) echo "--keep must be a path inside the project: $keep" >&2; exit 1 ;;
    esac
    if [ -e "$keep" ] && [ -n "$(ls -A "$keep" 2>/dev/null)" ]; then
        echo "--keep dir already exists and is not empty: $keep" >&2
        exit 1
    fi
    mkdir -p "$keep"
fi

# Copies a project file or directory to the keep dir at the same relative path (no-op without --keep).
keep_copy() {
    [ -n "$keep" ] && [ -e "$1" ] || return 0
    mkdir -p "$keep/$(dirname "$1")"
    cp -r "$1" "$keep/$1"
}

# Build outputs of the examples go first, before any source changes: a running Gradle daemon (Android Studio's too; it
# restarts on the next sync) keeps them open, and Windows cannot delete open files.
./gradlew --stop >/dev/null 2>&1 || true
for feature in "${EXAMPLES[@]}"; do
    [ -d "feature/$feature" ] || continue
    find "feature/$feature" -type d \( -name build -o -name .kotlin \) -prune -exec rm -rf {} + ||
        { echo "could not delete feature/$feature build outputs (a file is still open): close Android Studio and run again. Nothing was removed." >&2; exit 1; }
done

for feature in "${EXAMPLES[@]}"; do
    keep_copy "feature/$feature"
    rm -rf "feature/$feature"
    sed -i "/include(\":feature:$feature:/d" settings.gradle.kts
done
rmdir feature 2>/dev/null || true # only when no feature of your own is left

strip_regions() {
    # A depth counter so a region inside an example region goes with it. Markers are whole lines, so the text
    # "// region example:" quoted inside a comment is not a marker.
    awk '
        /^[ \t]*\/\/ region example:[^ ]*\r?$/ { depth++; next }
        depth > 0 && /^[ \t]*\/\/ region / { depth++; next }
        depth > 0 && /^[ \t]*\/\/ endregion\r?$/ { depth--; next }
        depth > 0 { next }
        { print }
    ' "$1" > "$1.tmp" && mv "$1.tmp" "$1"
}

exclude_keep=()
[ -n "$keep" ] && exclude_keep=(--exclude-dir="$(basename "$keep")")
{ grep -rlE --include='*.kt' --include='*.kts' "^[[:space:]]*// region example:" . --exclude-dir=build --exclude-dir=.gradle "${exclude_keep[@]}" || true; } |
    while IFS= read -r file; do
        file="${file#./}"
        keep_copy "$file"
        strip_regions "$file"
    done

if [ -d app/src/test ]; then
    modules=$(IFS='|'; echo "${EXAMPLES[*]}")
    # Example code keeps the package feature.example(.<sub>); core:ui's helpers share that root package, so a plain
    # `feature.example` import is not enough to delete a test: it takes an example sub-package or module package.
    pattern="^package com\.primestrider\.kotlinstarter\.feature\.|^// remove-examples: |kotlinstarter\.feature\.($modules)\.|kotlinstarter\.feature\.example\.(products|todos|auth|settings|sdk|animation|gestures|components|styling|form)\."
    { grep -rlE "$pattern" app/src/test || true; } | while IFS= read -r file; do
        keep_copy "$file"
        rm -f "$file"
    done
fi

if [ -n "$keep" ]; then
    find "$keep" -type d \( -name build -o -name .gradle -o -name .kotlin \) -prune -exec rm -rf {} +
    cat > "$keep/README.md" <<NOTE
# Removed examples (reference only)

Written by \`scripts/remove-examples.sh --keep $keep\`. Git-ignored and not part of the Gradle build: read it, copy
patterns from it, never include it in settings.gradle.kts or import from it.

- \`feature/<example>/\`: the example modules (\`api\` routes + \`impl\` screens, ViewModels, data, Hilt modules, tests).
- \`app/\`: the :app files as they were before their \`// region example:<feature>\` blocks were stripped, i.e. how a
  feature is wired into \`:app\` (dependencies in \`build.gradle.kts\`, destinations and tabs in \`AppNavHost.kt\`).
- \`app/src/test/\`: the deleted :app tests that drove example screens.
NOTE
    grep -qxF "/$keep/" .gitignore 2>/dev/null || printf '\n# Example code kept by scripts/remove-examples.sh --keep (reference only)\n/%s/\n' "$keep" >> .gitignore
fi

# Never from the build cache: the cached result would not touch the edited sources.
./gradlew --quiet --no-build-cache ktlintFormat --rerun-tasks
echo "Examples removed from $(pwd). Build with: ./gradlew test assembleDevDebug"
[ -z "$keep" ] || echo "Removed code kept for reference in $keep/ (git-ignored)."
