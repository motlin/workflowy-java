set dotenv-filename := ".envrc"

group_id_with_slashes := "com/workflowy"

import ".just/console.just"
import ".just/maven.just"
import ".just/git.just"
import ".just/git-test.just"

# `just --list --unsorted`
[group('default')]
default:
    @just --list --unsorted

workflowy_backups_path := env('WORKFLOWY_BACKUPS_PATH', '')

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

workflowy_api_key := env('WORKFLOWY_API_KEY', '')

# Import Workflowy backup data into the database, then fetch latest from API
[group('data')]
import-data DAYS="10000" MVN=default_mvn:
    #!/usr/bin/env bash
    set -Eeuo pipefail
    {{MVN}} install \
        --projects workflowy-dropwizard-application \
        --also-make \
        -DskipTests
    {{MVN}} exec:exec@import-workflowy \
        --projects workflowy-dropwizard-application \
        --activate-profiles import-workflowy \
        -DbackupsPath={{workflowy_backups_path}} \
        -DdaysLimit={{DAYS}}
    if [[ -n "{{workflowy_api_key}}" ]]; then
        echo "📥 Fetching latest from Workflowy API..."
        EXPORT_FILE=$(mktemp).json
        curl -s -H "Authorization: Bearer {{workflowy_api_key}}" \
            "https://workflowy.com/api/v1/nodes-export" > "$EXPORT_FILE"
        {{MVN}} exec:exec@import-workflowy-api \
            --projects workflowy-dropwizard-application \
            --activate-profiles import-workflowy-api \
            -DapiExportFile="$EXPORT_FILE"
        rm "$EXPORT_FILE"
    fi

# Roll back to before the Nth-to-last backup import
[group('data')]
rollback-backups COUNT MVN=default_mvn:
    #!/usr/bin/env bash
    set -Eeuo pipefail
    echo "⏪ Rolling back {{COUNT}} backup(s)..."
    {{MVN}} install \
        --projects workflowy-dropwizard-application \
        --also-make \
        -DskipTests
    {{MVN}} exec:java \
        --projects workflowy-dropwizard-application \
        -Dexec.mainClass=com.workflowy.dropwizard.application.WorkflowyApplication \
        -Dexec.args="rollback-backups config.json5 --backups-path {{workflowy_backups_path}} --count {{COUNT}}"

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

# Demo CLI commands (shows JSON output from all 4 commands)
[group('cli')]
demo MVN=default_mvn:
    #!/usr/bin/env bash
    set -Eeuo pipefail
    echo "🔨 Building application..."
    {{MVN}} compile -pl workflowy-dropwizard-application -am -DskipTests --quiet

    cd workflowy-dropwizard-application

    echo ""
    echo "📊 cache-status: Show cache statistics"
    {{MVN}} exec:java \
        -Dexec.mainClass=com.workflowy.dropwizard.application.WorkflowyApplication \
        -Dexec.args="cache-status config.json5 --color" --quiet 2>&1 | sed -n '/^{$/,/^}$/p'

    echo ""
    echo "📂 list-by-id: List root nodes"
    ROOT_OUTPUT=$({{MVN}} exec:java \
        -Dexec.mainClass=com.workflowy.dropwizard.application.WorkflowyApplication \
        -Dexec.args="list-by-id config.json5 --color" --quiet 2>&1 | sed -n '/^\[$/,/^\]$/p')
    echo "$ROOT_OUTPUT"
    PLAIN_OUTPUT=$(echo "$ROOT_OUTPUT" | sed 's/\x1b\[[0-9;]*m//g')
    FIRST_ID=$(echo "$PLAIN_OUTPUT" | jq -r '.[0].id // empty' 2>/dev/null)
    FIRST_NAME=$(echo "$PLAIN_OUTPUT" | jq -r '.[0].name // empty' 2>/dev/null)

    if [[ -n "$FIRST_ID" ]]; then
        echo ""
        echo "📖 read-node: Read node '$FIRST_ID' with depth=1"
        {{MVN}} exec:java \
            -Dexec.mainClass=com.workflowy.dropwizard.application.WorkflowyApplication \
            -Dexec.args="read-node config.json5 --id \"$FIRST_ID\" --depth 1 --color" --quiet 2>&1 | sed -n '/^{$/,/^}$/p'

        if [[ -n "$FIRST_NAME" ]]; then
            echo ""
            echo "🗂️ list-by-path: Navigate to '$FIRST_NAME'"
            {{MVN}} exec:java \
                -Dexec.mainClass=com.workflowy.dropwizard.application.WorkflowyApplication \
                -Dexec.args="list-by-path config.json5 --path \"$FIRST_NAME\" --color" --quiet 2>&1 | sed -n '/^\[$/,/^\]$/p'
        fi
    else
        echo ""
        echo "ℹ️  Note: No nodes found. Run 'just import-data' to import your Workflowy backups first."
    fi

# Run a CLI command (e.g., `just cli cache-status`)
[group('cli')]
cli +ARGS:
    cd workflowy-dropwizard-application && mvn exec:java \
        -Dexec.mainClass=com.workflowy.dropwizard.application.WorkflowyApplication \
        -Dexec.args="{{ARGS}} config.json5" --quiet

# Roll back to keep data up to a specific backup date (deletes all data after)
[group('data')]
rollback-to-backup BACKUP_DATE MVN=default_mvn:
    #!/usr/bin/env bash
    set -Eeuo pipefail
    echo "🔍 Finding SYSTEM_TO for backup dated {{BACKUP_DATE}}..."

    # Query DATA_IMPORT_TIMESTAMP to find the SYSTEM_TO for this backup date
    SYSTEM_TO=$(java -cp ~/.m2/repository/com/h2database/h2/2.3.232/h2-2.3.232.jar \
        org.h2.tools.Shell \
        -url "jdbc:h2:file:./workflowy-dropwizard-application/target/h2db/workflowy-h2;MODE=LEGACY" \
        -user sa -password "" \
        -sql "SELECT SYSTEM_TO FROM DATA_IMPORT_TIMESTAMP WHERE CAST(timestamp AS DATE) = '{{BACKUP_DATE}}'" \
        | tail -1 | tr -d ' ')

    if [[ -z "$SYSTEM_TO" || "$SYSTEM_TO" == "(0rows,"* ]]; then
        echo "❌ No backup found for date {{BACKUP_DATE}}"
        exit 1
    fi

    echo "⏪ Rolling back to SYSTEM_FROM >= $SYSTEM_TO (keeping data up to {{BACKUP_DATE}})..."
    {{MVN}} exec:java \
        --projects workflowy-dropwizard-application \
        -Dexec.mainClass=com.workflowy.dropwizard.application.WorkflowyApplication \
        -Dexec.args="rollback-temporal config.json5 --date ${SYSTEM_TO}"

# Override this with a command called `woof` which notifies you in whatever ways you prefer.
# My `woof` command uses `echo`, `say`, and sends a Pushover notification.
echo_command := env('ECHO_COMMAND', "echo")
