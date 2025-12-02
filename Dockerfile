FROM nginx:stable

# Копируем все файлы проекта в стандартную папку Nginx
COPY . /usr/share/nginx/html/

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]