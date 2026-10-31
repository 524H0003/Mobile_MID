const express = require('express');
const sharp = require('sharp');
const app = express();
const PORT = 80;

// Simple in-memory cache with TTL
const cache = new Map();
const CACHE_TTL = 30 * 24 * 60 * 60 * 1000; // 30 days in milliseconds

// Bật Cache-Control 30 ngày cho toàn bộ response
app.use((req, res, next) => {
    res.setHeader('Cache-Control', 'public, max-age=2592000'); // 30 ngày
    next();
});

// Endpoint khớp với cấu trúc URL trong test case Android: /id/{id}/{width}/{height}
app.get('/id/:id/:width/:height', async (req, res) => {
    try {
        const width = parseInt(req.params.width) || 200;
        const height = parseInt(req.params.height) || 200;
        const id = req.params.id || '0';

        // Giới hạn kích thước tránh quá tải RAM
        const safeWidth = Math.min(Math.max(width, 10), 4000);
        const safeHeight = Math.min(Math.max(height, 10), 4000);

        // Check cache
        const cacheKey = `${id}:${safeWidth}:${safeHeight}`;
        const cached = cache.get(cacheKey);
        if (cached && Date.now() - cached.timestamp < CACHE_TTL) {
            console.log(`[CACHE HIT] ${cacheKey}`);
            res.set('X-Cache-Status', 'CACHE-HIT');
            res.send(cached.buffer);
            return;
        }

        // Tạo gradient màu ngẫu nhiên dựa trên ID để mỗi ảnh có màu khác nhau dễ nhận biết
        const hash = id.toString().split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
        const r1 = (hash * 37) % 200 + 30;
        const g1 = (hash * 53) % 200 + 30;
        const b1 = (hash * 79) % 200 + 30;
        const r2 = ((hash * 61) % 200 + 30);
        const g2 = ((hash * 73) % 200 + 30);
        const b2 = ((hash * 89) % 200 + 30);

        // Dùng sharp tạo một ảnh SVG với gradient và text mô tả kích thước rồi convert sang JPEG cực nhanh
        const svgImage = `
            <svg width="${safeWidth}" height="${safeHeight}">
                <defs>
                    <linearGradient id="grad" x1="0%" y1="0%" x2="100%" y2="100%">
                        <stop offset="0%" style="stop-color:rgb(${r1},${g1},${b1});stop-opacity:1" />
                        <stop offset="100%" style="stop-color:rgb(${r2},${g2},${b2});stop-opacity:1" />
                    </linearGradient>
                </defs>
                <rect width="100%" height="100%" fill="url(#grad)" />
                <text x="50%" y="50%" font-size="${Math.max(16, safeWidth / 12)}" fill="#ffffff" dominant-baseline="middle" text-anchor="middle" font-family="sans-serif">
                    ID: ${id} (${safeWidth}x${safeHeight})
                </text>
            </svg>
        `;

        const buffer = await sharp(Buffer.from(svgImage))
            .jpeg({ quality: 80 })
            .toBuffer();

        // Store in cache
        cache.set(cacheKey, {
            buffer,
            timestamp: Date.now()
        });

        res.set('Content-Type', 'image/jpeg');
        res.set('X-Cache-Status', 'LOCAL-GENERATED');
        res.send(buffer);
    } catch (error) {
        console.error(error);
        res.status(500).send('Error generating image');
    }
});

app.listen(PORT, () => {
    console.log(`Local Image Provider running on port ${PORT}`);
});