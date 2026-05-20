# =========================================================================
# Dockerfile — SIFAP 2.0 Frontend (Next.js 15 + React 19)
# Build em duas etapas: instalação + build → runtime Node 20 slim.
# =========================================================================

FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm install --no-audit --no-fund

COPY . .
RUN npm run build

# -------------------------------------------------------------------------
FROM node:20-alpine AS runtime
WORKDIR /app

ENV NODE_ENV=production \
    PORT=3000

COPY --from=build /app/.next ./.next
COPY --from=build /app/public ./public
COPY --from=build /app/package.json ./package.json
COPY --from=build /app/node_modules ./node_modules

RUN addgroup -S sifap && adduser -S sifap -G sifap
USER sifap

EXPOSE 3000

CMD ["npx", "next", "start", "-p", "3000"]
