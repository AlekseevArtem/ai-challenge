FROM nginx:1.25-alpine

# Копируем все файлы проекта в стандартную папку Nginx
COPY . /usr/share/nginx/html/

# Устанавливаем правильные права доступа
RUN chmod -R 755 /usr/share/nginx/html/

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]