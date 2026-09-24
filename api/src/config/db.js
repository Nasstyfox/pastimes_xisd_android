const fs = require('fs');
const path = require('path');
const mysql = require('mysql2/promise');

/**
 * SSL config resolution:
 *  1. If DB_SSL_CA_CONTENT is set (Render), use it directly.
 *  2. Else if DB_SSL_CA is a path (local), read the file.
 *  3. Else no SSL (local dev against non-SSL DB).
 */
let sslConfig;
if (process.env.DB_SSL_CA_CONTENT) {
  sslConfig = { ca: process.env.DB_SSL_CA_CONTENT.replace(/\\n/g, '\n') };
} else if (process.env.DB_SSL_CA) {
  const caPath = path.resolve(process.cwd(), process.env.DB_SSL_CA);
  sslConfig = { ca: fs.readFileSync(caPath, 'utf8') };
} else {
  sslConfig = undefined;
}

const pool = mysql.createPool({
  host: process.env.DB_HOST,
  port: Number(process.env.DB_PORT || 3306),
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  ssl: sslConfig,
  waitForConnections: true,
  connectionLimit: 10,
  queueLimit: 0,
  timezone: 'Z',
  charset: 'utf8mb4'
});

/**
 * Quick connectivity test — called once on server start.
 */
async function testConnection() {
  const conn = await pool.getConnection();
  try {
    await conn.query('SELECT 1');
    console.log('[db] Connected to MySQL');
  } finally {
    conn.release();
  }
}

module.exports = { pool, testConnection };