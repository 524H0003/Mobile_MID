#!/usr/bin/env python3
"""
Script tạo ảnh mẫu cho benchmark với cấu trúc thư mục:
docker-images/
  ├── 0/
  │   ├── 100.jpg
  │   ├── 500.jpg
  │   ├── 1000.jpg
  │   └── 2000.jpg
  ├── 1/
  │   ├── 100.jpg
  │   ├── 500.jpg
  │   ├── 1000.jpg
  │   └── 2000.jpg
  └── ...
"""

import os
import random
from PIL import Image, ImageDraw, ImageFont
import argparse


def generate_random_image(width, height, seed=None):
    """Tạo ảnh ngẫu nhiên với seed cố định để đảm bảo ảnh giống nhau mỗi lần chạy"""
    if seed is not None:
        random.seed(seed)

    # Tạo ảnh màu ngẫu nhiên
    img = Image.new('RGB', (width, height), color=(
        random.randint(0, 255),
        random.randint(0, 255),
        random.randint(0, 255)
    ))

    # Thêm một số hình dạng ngẫu nhiên
    draw = ImageDraw.Draw(img)

    # Vẽ hình chữ nhật
    for _ in range(5):
        x1 = random.randint(0, width - 100)
        y1 = random.randint(0, height - 100)
        x2 = x1 + random.randint(50, 150)
        y2 = y1 + random.randint(50, 150)
        draw.rectangle(
            [x1, y1, x2, y2],
            fill=(
                random.randint(0, 255),
                random.randint(0, 255),
                random.randint(0, 255)
            )
        )

    # Vẽ hình tròn
    for _ in range(3):
        x = random.randint(0, width - 100)
        y = random.randint(0, height - 100)
        radius = random.randint(20, 50)
        draw.ellipse(
            [x - radius, y - radius, x + radius, y + radius],
            fill=(
                random.randint(0, 255),
                random.randint(0, 255),
                random.randint(0, 255)
            )
        )

    return img


def generate_images(start_id, end_id, output_dir):
    """Tạo ảnh cho tất cả các ID từ start_id đến end_id"""
    sizes = [100, 500, 1000, 2000]

    print(f"Đang tạo ảnh từ ID {start_id} đến {end_id}...")

    for img_id in range(start_id, end_id + 1):
        # Tạo thư mục cho ID này
        id_dir = os.path.join(output_dir, str(img_id))
        os.makedirs(id_dir, exist_ok=True)

        # Tạo ảnh với seed cố định để đảm bảo ảnh giống nhau
        for size in sizes:
            img = generate_random_image(size, size, seed=img_id * 100 + size)
            output_path = os.path.join(id_dir, f"{size}.jpg")
            img.save(output_path, quality=95)
            print(f"  Tạo: {output_path}")

    print(f"\n✓ Hoàn thành! Đã tạo {end_id - start_id + 1} ảnh với {len(sizes)} kích thước mỗi ảnh.")


def main():
    parser = argparse.ArgumentParser(description='Tạo ảnh mẫu cho benchmark')
    parser.add_argument('--start', type=int, default=0, help='ID ảnh bắt đầu (mặc định: 0)')
    parser.add_argument('--end', type=int, default=999, help='ID ảnh kết thúc (mặc định: 999)')
    parser.add_argument('--output', type=str, default='docker-images', help='Thư mục đầu ra (mặc định: docker-images)')

    args = parser.parse_args()

    if args.start > args.end:
        print(f"Lỗi: start ({args.start}) không thể lớn hơn end ({args.end})")
        return

    total_images = args.end - args.start + 1
    print(f"📊 Tổng số ảnh cần tạo: {total_images}")
    print(f"📐 Kích thước ảnh: 100x100, 500x500, 1000x1000, 2000x2000")
    print(f"📁 Thư mục đầu ra: {os.path.abspath(args.output)}\n")

    generate_images(args.start, args.end, args.output)


if __name__ == '__main__':
    main()