# ---------- Etapa 1: build de Vue ----------
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# ---------- Etapa 2: servir con Nginx ----------
FROM nginx:alpine
RUN addgroup -g 1000 appuser && adduser -D -u 1000 -G appuser appuser
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
RUN chown -R appuser:appuser /usr/share/nginx/html /var/cache/nginx /var/log/nginx /etc/nginx/conf.d
EXPOSE 80
USER appuser
CMD ["nginx", "-g", "daemon off;"]