const admin = require('../config/firebase');
const { pool } = require('../config/db');

async function authRequired(req, res, next) {
  try {
    const header = req.headers.authorization || '';
    const token = header.startsWith('Bearer ') ? header.slice(7).trim() : null;
    if (!token) {
      return res.status(401).json({ error: 'Missing or malformed Authorization header' });
    }

    const decoded = await admin.auth().verifyIdToken(token);

    const [rows] = await pool.query(
      `SELECT id, firebase_uid, full_name, email, role, is_active
         FROM users
        WHERE firebase_uid = ?
        LIMIT 1`,
      [decoded.uid]
    );

    if (rows.length === 0) {
      return res.status(401).json({ error: 'User not registered in system' });
    }
    if (!rows[0].is_active) {
      return res.status(403).json({ error: 'Account is deactivated' });
    }

    req.user = rows[0];
    next();
  } catch (err) {
    console.error('[auth] verify failed:', err.message);
    return res.status(401).json({ error: 'Invalid or expired token' });
  }
}

module.exports = { authRequired };