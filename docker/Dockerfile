# Stage 1: Build images với Python
FROM python:3.11-slim as builder

WORKDIR /app

# Cài Pillow
RUN pip install --no-cache-dir Pillow

# Copy script tạo ảnh
COPY generate_images.py .

# Tạo ảnh mẫu
RUN python generate_images.py --start 0 --end 999

# Stage 2: Serve ảnh với Nginx
FROM nginx:alpine

# Copy cấu hình Nginx
COPY nginx.conf /etc/nginx/nginx.conf

# Copy ảnh đã tạo từ stage builder
COPY --from=builder /app/docker-images /usr/share/nginx/html/images

# Expose port 80
EXPOSE 80

# Chạy Nginx
CMD ["nginx", "-g", "daemon off;"]