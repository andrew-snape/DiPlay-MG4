#!/bin/sh
# Usage: scripts/bump_version.sh major|minor|patch
# Bumps versionName (MAJOR.MINOR.PATCH) and versionCode (+1) in version.properties and turns the
# CHANGELOG "Unreleased" heading into the release heading. Does not commit or tag.
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
FILE="$ROOT/version.properties"
KIND="${1:-}"
case "$KIND" in major|minor|patch) ;; *) echo "Usage: $0 major|minor|patch" >&2; exit 1 ;; esac

NAME=$(sed -n 's/^versionName=//p' "$FILE")
CODE=$(sed -n 's/^versionCode=//p' "$FILE")
case "$NAME" in [0-9]*.[0-9]*.[0-9]*) ;; *) echo "Error: versionName '$NAME' is not MAJOR.MINOR.PATCH" >&2; exit 1 ;; esac

MAJOR=${NAME%%.*}; REST=${NAME#*.}; MINOR=${REST%%.*}; PATCH=${REST#*.}
case "$KIND" in
  major) MAJOR=$((MAJOR + 1)); MINOR=0; PATCH=0 ;;
  minor) MINOR=$((MINOR + 1)); PATCH=0 ;;
  patch) PATCH=$((PATCH + 1)) ;;
esac
NEW="$MAJOR.$MINOR.$PATCH"
NEWCODE=$((CODE + 1))

sed -i.bak "s/^versionName=.*/versionName=$NEW/; s/^versionCode=.*/versionCode=$NEWCODE/" "$FILE" && rm -f "$FILE.bak"

# "# Unreleased" becomes the release heading, with a fresh "# Unreleased" above it.
DATE=$(date +%Y-%m-%d)
CHANGELOG="$ROOT/CHANGELOG.md"
if head -1 "$CHANGELOG" | grep -q '^# Unreleased'; then
  { printf '# Unreleased\n\n# MG4CPlay %s (SWI165 fork), %s\n' "$NEW" "$DATE"; tail -n +2 "$CHANGELOG"; } > "$CHANGELOG.new"
  mv "$CHANGELOG.new" "$CHANGELOG"
else
  echo "Warning: CHANGELOG.md does not start with '# Unreleased'; add the $NEW heading by hand." >&2
fi

echo "$NAME ($CODE) -> $NEW ($NEWCODE)"
echo "Next: fill in CHANGELOG.md, then commit 'chore: release $NEW', merge to main and run:"
echo "  gh workflow run release.yml -R andrew-snape/DiPlay-MG4 --ref main -f tag=v$NEW"
