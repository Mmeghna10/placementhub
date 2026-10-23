# syntax=docker/dockerfile:1

# ---------- Build stage: produce the static React bundle ----------
FROM node:22.23.2-alpine AS build
WORKDIR /app

# Install exactly what package-lock.json pins; cached until it changes.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
# Empty = call the API on the same origin (/api/...), which nginx forwards to the backend.
# Vite bakes this into the bundle at build time, so it is a build argument, not a runtime
# variable.
ARG VITE_API_BASE_URL=
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN npm run build

# ---------- Runtime stage: nginx serving files and proxying /api ----------
# The "unprivileged" image runs nginx as a non-root user, listening on 8080.
FROM nginxinc/nginx-unprivileged:1.28.0-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --start-period=10s --retries=3 \
    CMD wget -qO- http://127.0.0.1:8080/healthz || exit 1
