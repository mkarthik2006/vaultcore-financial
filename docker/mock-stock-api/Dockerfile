FROM node:20-alpine

WORKDIR /app

COPY package.json package-lock.json* ./
# Prefer a reproducible install from the lockfile when present; fall back otherwise.
RUN if [ -f package-lock.json ]; then npm ci --omit=dev; else npm install --omit=dev; fi

COPY index.js .

# node:* images ship a non-root "node" user; run as it.
USER node

EXPOSE 8080
CMD ["node", "index.js"]