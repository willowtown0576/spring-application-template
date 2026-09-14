#!/bin/sh
set -eu

tbls doc --config .tbls.yml --force --rm-dist
# Render tbls DOT with native font metrics to prevent overlapping SVG labels.
for diagram in build/documentation/database/*.svg; do
    table=$(basename "$diagram" .svg)
    set --
    if [ "$table" != schema ]; then
        set -- --table "$table"
    fi
    tbls out -t dot --config .tbls.yml --dsn json:///workspace/build/documentation/database/schema.json "$@" -o /tmp/documentation.dot
    dot -Tsvg /tmp/documentation.dot -o "$diagram"
done
