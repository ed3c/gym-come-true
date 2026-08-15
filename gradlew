#!/usr/bin/env sh
set -eu

required="9.5.0"

if ! command -v gradle >/dev/null 2>&1; then
  echo "Gradle $required is required but no 'gradle' executable was found." >&2
  echo "Install the pinned version or let CI provision it through gradle/actions/setup-gradle." >&2
  exit 64
fi

actual="$(gradle --version | awk '/^Gradle / { print $2; exit }')"
if [ "$actual" != "$required" ]; then
  echo "Expected Gradle $required, found $actual. Refusing an unpinned build." >&2
  exit 64
fi

exec gradle "$@"
