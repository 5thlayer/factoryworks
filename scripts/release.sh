#!/usr/bin/env bash
# Release FactoryWorks Core at <version> from HEAD (ADR-0101): publish/core/changelog.md's Unreleased
# entries become <version>'s, the build and unit tests pass, and the jar is published to the local
# maven repository, tagged core-v<version>, and uploaded to CurseForge and Modrinth by scripts/upload.py.
#
#   scripts/release.sh [--no-upload] <version>
#
# It commits and tags but pushes nothing. $MAVEN_REPO_LOCAL publishes somewhere other than
# ~/.m2/repository, to try the script out, and then the upload is only a dry run.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "release: $*" >&2; exit 1; }

property() { sed -n "s/^$1 *= *//p" gradle.properties; }
name="$(property mod_name)"
group="$(property maven_group)"
artifact="$(property mod_id)"
[[ -n "$name" && -n "$group" && -n "$artifact" ]] || fail "gradle.properties must name mod_name, maven_group and mod_id."

changelog=publish/core/changelog.md
upload_now=1
if [[ "${1:-}" == --no-upload ]]; then upload_now=; shift; fi
version="${1:-}"
[[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || fail "usage: scripts/release.sh [--no-upload] <major.minor.patch>"
# The pack's own tags are not the core mod's.
tag="core-v$version"
repo="${MAVEN_REPO_LOCAL:-$HOME/.m2/repository}"
published="$repo/${group//.//}/$artifact/$version"

[[ -z "$(git status --porcelain)" ]] || fail "the working tree has changes; commit or stash them first."
! git rev-parse -q --verify "refs/tags/$tag" > /dev/null || fail "$tag already exists."
[[ ! -e "$published" ]] || fail "$version is already in $published, and a published version never changes."

entries="$(awk '/^## /{on = ($0 == "## Unreleased"); next} on && NF' "$changelog")"
[[ -n "$entries" ]] || fail "$changelog has nothing under \"## Unreleased\"; a release ships what it lists."

trap 'git checkout -- gradle.properties "$changelog"' EXIT
sed -i.bak "s/^mod_version *=.*/mod_version=$version/" gradle.properties && rm gradle.properties.bak
awk -v v="$version" '{print} $0 == "## Unreleased" {print ""; print "## " v}' "$changelog" > "$changelog.new"
mv "$changelog.new" "$changelog"

./gradlew :factoryworks_core:build
git commit -q -m "chore: release $name $version" -- gradle.properties "$changelog"
trap - EXIT

./gradlew "-Dmaven.repo.local=$repo" :factoryworks_core:publishToMavenLocal
sha="$(shasum -a 256 "$published/$artifact-$version.jar" | cut -d' ' -f1)"
git tag -a "$tag" -m "$name $version" -m "jar sha256 $sha"

echo "Published $published"
echo "jar sha256 $sha"

# Last, so a failed upload leaves the local release and its tag as they are, to retry on its own.
upload=(scripts/upload.py)
[[ -z "${MAVEN_REPO_LOCAL:-}" ]] || upload+=(--dry-run)
if [[ -z "$upload_now" ]]; then
    echo "Upload with: scripts/upload.py $version"
elif ! "${upload[@]}" "$version"; then
    echo "release: $version is released and tagged, but the upload failed; retry it with" >&2
    echo "release:   scripts/upload.py --site <site> $version, for each site named above" >&2
fi
echo "Push with: git push origin HEAD $tag"
