const express = require("express");
const sharp = require("sharp");
const fs = require("fs");
const path = require("path");
const app = express();
const PORT = 8686;

// Cache configuration
const CACHE_DIR = path.join(__dirname, "cache");
const CACHE_TTL = 30 * 24 * 60 * 60 * 1000; // 30 days in milliseconds

// Ensure cache directory exists
if (!fs.existsSync(CACHE_DIR)) {
  fs.mkdirSync(CACHE_DIR, { recursive: true });
}

// Simple in-memory cache with TTL
const cache = new Map();

async function getImageBuffer(id, width, height) {
  const cacheKey = `${id}:${width}:${height}`;
  const cacheFilePath = path.join(
    CACHE_DIR,
    cacheKey.replace(/:/g, "_") + ".cache",
  );

  // Check in-memory cache first
  const cached = cache.get(cacheKey);
  if (cached && Date.now() - cached.timestamp < CACHE_TTL) {
    return { buffer: cached.buffer, cacheStatus: "CACHE-HIT" };
  }

  // Check disk cache
  if (fs.existsSync(cacheFilePath)) {
    try {
      const stats = fs.statSync(cacheFilePath);
      if (Date.now() - stats.mtimeMs < CACHE_TTL) {
        const buffer = fs.readFileSync(cacheFilePath);
        cache.set(cacheKey, { buffer, timestamp: Date.now() });
        return { buffer, cacheStatus: "CACHE-HIT" };
      }
    } catch (err) {
      // If disk cache read fails, continue to generate
    }
  }

  const hash = id
    .toString()
    .split("")
    .reduce((acc, char) => acc + char.charCodeAt(0), 0);
  const r1 = ((hash * 37) % 200) + 30;
  const g1 = ((hash * 53) % 200) + 30;
  const b1 = ((hash * 79) % 200) + 30;
  const r2 = ((hash * 61) % 200) + 30;
  const g2 = ((hash * 73) % 200) + 30;
  const b2 = ((hash * 89) % 200) + 30;

  const svgImage = `
        <svg width="${width}" height="${height}">
            <defs>
                <linearGradient id="grad" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" style="stop-color:rgb(${r1},${g1},${b1});stop-opacity:1" />
                    <stop offset="100%" style="stop-color:rgb(${r2},${g2},${b2});stop-opacity:1" />
                </linearGradient>
            </defs>
            <rect width="100%" height="100%" fill="url(#grad)" />
            <text x="50%" y="50%" font-size="${Math.max(16, width / 12)}" fill="#ffffff" dominant-baseline="middle" text-anchor="middle" font-family="sans-serif">
                ID: ${id} (${width}x${height})
            </text>
        </svg>
    `;

  const buffer = await sharp(Buffer.from(svgImage))
    .jpeg({ quality: 80 })
    .toBuffer();

  // Save to disk cache
  try {
    fs.writeFileSync(cacheFilePath, buffer);
  } catch (err) {
    console.error("Failed to write cache file:", err);
  }

  cache.set(cacheKey, { buffer, timestamp: Date.now() });
  return { buffer, cacheStatus: "LOCAL-GENERATED" };
}

async function preloadCache() {
  const sizes = [500, 2000];
  const total = 1000 * sizes.length;
  const concurrency = 4;
  let nextTask = 0;

  await Promise.all(
    Array.from({ length: concurrency }, async () => {
      while (nextTask < total) {
        const task = nextTask++;
        const id = Math.floor(task / sizes.length).toString();
        const size = sizes[task % sizes.length];
        await getImageBuffer(id, size, size);
      }
    }),
  );

  console.log(`[CACHE READY] ${cache.size} images preloaded`);
}

// Bật Cache-Control 30 ngày cho toàn bộ response
app.use((req, res, next) => {
  res.setHeader("Cache-Control", "public, max-age=2592000"); // 30 ngày
  next();
});

// Logging middleware cho các kết nối
app.use((req, res, next) => {
  const timestamp = new Date().toISOString();
  const clientIp = req.ip || req.connection.remoteAddress || "unknown";
  console.log(`[${timestamp}] ${clientIp} -> ${req.method} ${req.url}`);
  next();
});

// Endpoint khớp với cấu trúc URL trong test case Android: /id/{id}/{width}/{height}
app.get("/id/:id/:width/:height", async (req, res) => {
  try {
    const width = parseInt(req.params.width) || 200;
    const height = parseInt(req.params.height) || 200;
    const id = req.params.id || "0";

    // Giới hạn kích thước tránh quá tải RAM
    const safeWidth = Math.min(Math.max(width, 10), 4000);
    const safeHeight = Math.min(Math.max(height, 10), 4000);

    const { buffer, cacheStatus } = await getImageBuffer(
      id,
      safeWidth,
      safeHeight,
    );
    res.set("Content-Type", "image/jpeg");
    res.set("X-Cache-Status", cacheStatus);
    res.send(buffer);
  } catch (error) {
    console.error(error);
    res.status(500).send("Error generating image");
  }
});

preloadCache()
  .then(() => {
    app.listen(PORT, "0.0.0.0", () => {
      console.log(`Local Image Provider running on port ${PORT}`);
    });
  })
  .catch((error) => {
    console.error("Failed to preload image cache:", error);
    process.exit(1);
  });
