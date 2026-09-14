FROM ghcr.io/k1low/tbls:v1.96.0 AS tbls
FROM node:24.14.0-slim AS node
FROM debian:13.1-slim

# A fixed snapshot makes the distribution toolchain reproducible.
RUN rm /etc/apt/sources.list.d/debian.sources \
    && echo 'deb [check-valid-until=no] http://snapshot.debian.org/archive/debian/20260901T000000Z trixie main' > /etc/apt/sources.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates chromium nodejs npm pandoc \
       texlive-luatex texlive-latex-extra texlive-lang-japanese fonts-noto-cjk \
       librsvg2-bin graphviz poppler-utils \
    && rm -rf /var/lib/apt/lists/*
COPY --from=tbls /usr/bin/tbls /usr/local/bin/tbls
COPY --from=node /usr/local/bin/node /usr/local/bin/node
ENV PUPPETEER_SKIP_DOWNLOAD=true
COPY package.json package-lock.json /opt/docs/
RUN cd /opt/docs && npm ci --omit=dev && npm cache clean --force
ENV PATH="/opt/docs/node_modules/.bin:${PATH}"
COPY puppeteer.json mermaid.json /opt/docs/
WORKDIR /workspace
