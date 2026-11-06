#!/usr/bin/env bash
# Renames the starter to your app: package/applicationId, app name, class and theme names, deep-link scheme,
# convention plugin ids and the Gradle project name. Run it once, on a fresh copy, before your own changes.
#
#   bash scripts/rename-project.sh <package> "<App Name>" [dir]     # dir default: the current directory
#   bash scripts/rename-project.sh com.acme.shop "Acme Shop"
#
# With "com.acme.shop" and "Acme Shop" it replaces, in every text file (docs and .examples/ included):
#   com.primestrider.kotlinstarter -> com.acme.shop   (packages, namespaces, applicationId, R8 rules)
#   com/primestrider/kotlinstarter -> com/acme/shop   (paths in docs and lint baselines)
#   Kotlin Starter -> Acme Shop                       (app_name, README title, tests)
#   KotlinStarter  -> AcmeShop                        (KotlinStarterApp, Theme.KotlinStarter)
#   kotlin-starter -> acme-shop                       (rootProject.name)
#   kotlinstarter  -> acmeshop                        (plugin ids kotlinstarter.*, deep-link scheme, key alias)
# then moves every source folder com/primestrider/kotlinstarter to com/acme/shop, renames files named after the old
# package or class (Room schemas, KotlinStarterApp.kt), deletes build outputs and runs ktlintFormat (import order
# changes with the package). Requires bash and perl (both ship with Git for Windows; on Windows run it from Git Bash).
set -euo pipefail

[ $# -ge 2 ] || { sed -n '2,6p' "$0" >&2; exit 1; }
package="$1"
name="$2"
script="$(cd "$(dirname "$0")" && pwd)/$(basename "$0")"
cd "${3:-.}"
[ -f settings.gradle.kts ] || { echo "not a project root: $(pwd)" >&2; exit 1; }

[[ "$package" =~ ^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$ ]] ||
    { echo "invalid package (lowercase, at least two segments, e.g. com.acme.shop): $package" >&2; exit 1; }
[ "$package" != "com.primestrider.kotlinstarter" ] || { echo "that is the current package" >&2; exit 1; }

pascal=""
for word in $(printf '%s' "$name" | tr -c 'A-Za-z0-9' ' '); do pascal+="$(printf '%s' "${word:0:1}" | tr 'a-z' 'A-Z')${word:1}"; done
[[ "$pascal" =~ ^[A-Za-z] ]] || { echo "the app name must start with a letter: $name" >&2; exit 1; }
lower="$(printf '%s' "$pascal" | tr 'A-Z' 'a-z')"
kebab="$(printf '%s' "$name" | tr 'A-Z' 'a-z' | tr -cs 'a-z0-9' '-' | sed 's/^-*//; s/-*$//')"
path="${package//.//}"

old_dirs=$(find . \( -name build -o -name .git -o -name .gradle -o -name .kotlin -o -name .idea \) -prune -o \
    -type d -path '*/com/primestrider/kotlinstarter' -print)
[ -n "$old_dirs" ] || { echo "no com/primestrider/kotlinstarter sources found: already renamed?" >&2; exit 1; }

echo "package $package, app name \"$name\", classes ${pascal}App / Theme.$pascal, scheme $lower, project $kebab"

# Build outputs hold classes and generated code under the old package. They go first, before any source changes: a
# running Gradle daemon (Android Studio's too; it restarts on the next sync) keeps them open, and Windows cannot delete
# open files.
./gradlew --stop >/dev/null 2>&1 || true
find . \( -name .git -prune \) -o -type d \( -name build -o -name .kotlin \) -prune -exec rm -rf {} + ||
    { echo "could not delete the build outputs (a file is still open): close Android Studio and run again. Nothing was renamed." >&2; exit 1; }

# 1. Text. The escaped forms come first: remove-examples.sh matches packages with regexes (com\.primestrider\...).
export R_PKG="$package" R_PATH="$path" R_NAME="$name" R_PASCAL="$pascal" R_LOWER="$lower" R_KEBAB="$kebab"
export R_PKG_RE="${package//./\\.}" R_LAST_RE="${package##*.}\\."
find . \( -name build -o -name .git -o -name .gradle -o -name .kotlin -o -name .idea \) -prune -o -type f -print0 |
    xargs -0 grep -IlE 'kotlinstarter|Kotlin Starter|KotlinStarter|kotlin-starter' 2>/dev/null |
    while IFS= read -r file; do
        [ "$(cd "$(dirname "$file")" && pwd)/$(basename "$file")" = "$script" ] && continue
        perl -pi -e '
            s/\Qcom\.primestrider\.kotlinstarter\E/$ENV{R_PKG_RE}/g;
            s/\Qkotlinstarter\.\E/$ENV{R_LAST_RE}/g;
            s/\Qcom.primestrider.kotlinstarter\E/$ENV{R_PKG}/g;
            s/\Qcom\/primestrider\/kotlinstarter\E/$ENV{R_PATH}/g;
            s/Kotlin Starter/$ENV{R_NAME}/g;
            s/KotlinStarter/$ENV{R_PASCAL}/g;
            s/kotlin-starter/$ENV{R_KEBAB}/g;
            s/kotlinstarter/$ENV{R_LOWER}/g;
        ' "$file"
    done

# 2. Source folders: <src>/com/primestrider/kotlinstarter -> <src>/<new/path>, through a temp name so a new package
#    that shares a prefix with the old one (com.primestrider.shop) works too.
while IFS= read -r dir; do
    base="${dir%/com/primestrider/kotlinstarter}"
    mv "$dir" "$base/.rename-tmp"
    rmdir "$base/com/primestrider" 2>/dev/null && rmdir "$base/com" 2>/dev/null || true
    mkdir -p "$base/$(dirname "$path")"
    mv "$base/.rename-tmp" "$base/$path"
done <<< "$old_dirs"

# 3. File names: Room schema folders named after the database class, KotlinStarterApp.kt.
find . \( -name .git -o -name .gradle -o -name .idea \) -prune -o \
    \( -name '*com.primestrider.kotlinstarter*' -o -name '*KotlinStarter*' \) -print | sort -r |
    while IFS= read -r file; do
        dir="$(dirname "$file")"
        new="$(basename "$file" | sed "s/com\.primestrider\.kotlinstarter/$package/; s/KotlinStarter/$pascal/")"
        mv "$file" "$dir/$new"
    done

# Never from the build cache: the cached result would not touch the edited sources.
./gradlew --quiet --no-build-cache ktlintFormat --rerun-tasks
echo "Renamed in $(pwd). Check with: ./gradlew test assembleDevDebug"
echo "Left as is: the upstream GitHub links in the docs and this script (it only works on the original names)."
