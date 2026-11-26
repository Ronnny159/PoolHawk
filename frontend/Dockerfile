# ---------- Etapa 1: build de Vue ----------
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# ---------- Etapa 2: servir con Nginx ----------
FROM nginx:alpine

# Configurar permisos para el usuario nginx (ya existe en alpine: uid 101)
# Necesita escribir en /var/cache/nginx, /var/run y leer /usr/share/nginx/html
RUN touch /var/run/nginx.pid && \
    chown -R nginx:nginx /var/cache/nginx /var/run/nginx.pid /usr/share/nginx/html /etc/nginx/conf.d

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

# El usuario nginx (no root) ya tiene permisos gracias al RUN anterior
EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]