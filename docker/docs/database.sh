#!/bin/sh
set -eu

tbls doc --config .tbls.yml --force --rm-dist
# Mermaid CLI replaces ER blocks with SVG references in the generated Markdown.
for document in build/documentation/database/*.md; do
    mmdc -p /opt/docs/puppeteer.json -c /opt/docs/mermaid.json -i "$document" -o "$document" -b white
done
