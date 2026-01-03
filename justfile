set dotenv-filename := ".envrc"

group_id_with_slashes := "com/workflowy"

import ".just/console.just"
import ".just/maven.just"
import ".just/git.just"
import ".just/git-test.just"

# `just --list--unsorted`
default:
    @just --list --unsorted

workflowy_backups_path := env('WORKFLOWY_BACKUPS_PATH')

# Full data pipeline: download backups, import data
[group('data')]
workflowy DAYS="10000": download-backups (import-data DAYS)

# Download Workflowy backups from Dropbox via ../workflowy CLI
[group('data')]
download-backups:
    #!/usr/bin/env bash
    set -Eeuo pipefail
    echo "📥 Downloading Workflowy backups via ../workflowy CLI"
    cd ../workflowy && just download-backups

# Import Workflowy backup data into the database
[group('data')]
import-data DAYS="10000" MVN=default_mvn:
    {{MVN}} package \
        --projects workflowy-dropwizard-application \
        --also-make \
        -DskipTests
    {{MVN}} exec:exec@import-workflowy \
        --projects workflowy-dropwizard-application \
        --activate-profiles import-workflowy \
        -DbackupsPath={{workflowy_backups_path}} \
        -DdaysLimit={{DAYS}}

# `mise install`
mise:
    mise install --quiet
    mise current

# clean (maven and git)
@clean: _clean-git _clean-maven _clean-m2

markdownlint:
    markdownlint --config .markdownlint.jsonc  --fix .

# Run all formatting tools for pre-commit
precommit: mvn
    uv tool run pre-commit run --all-files

# Override this with a command called `woof` which notifies you in whatever ways you prefer.
# My `woof` command uses `echo`, `say`, and sends a Pushover notification.
echo_command := env('ECHO_COMMAND', "echo")
