const express = require('express');
const admin = require('../config/firebase');
const { pool } = require('../config/db');
const { authRequired } = require('../middleware/auth');

const router = express.Router();

async function verifyToken(req) {
  const header = req.headers.authorization || '';
  const token = header.startsWith('Bearer ') ? header.slice(7).trim() : null;
  if (!token) {
    const e = new Error('Missing Authorization header');
    e.status = 401;
    throw e;
  }
  return admin.auth().verifyIdToken(token);
}

/* POST /api/auth/register */
router.post('/register', async (req, res) => {
  const { full_name, phone, role, auth_provider } = req.body || {};

  if (!full_name || !role) {
    return res.status(400).json({ error: 'full_name and role are required' });
  }
  if (!['buyer', 'seller', 'admin'].includes(role)) {
    return res.status(400).json({ error: 'Invalid role' });
  }
  const provider = auth_provider === 'google' ? 'google' : 'password';

  try {
    const decoded = await verifyToken(req);
    const uid = decoded.uid;
    const email = decoded.email;
    if (!email) {
      return res.status(400).json({ error: 'Firebase user has no email' });
    }

    const [existing] = await pool.query(
      'SELECT id FROM users WHERE firebase_uid = ? LIMIT 1',
      [uid]
    );
    if (existing.length > 0) {
      return res.status(409).json({ error: 'User already registered' });
    }

    const conn = await pool.getConnection();
    try {
      await conn.beginTransaction();

      const [result] = await conn.query(
        `INSERT INTO users (firebase_uid, full_name, email, phone, role, auth_provider)
         VALUES (?, ?, ?, ?, ?, ?)`,
        [uid, full_name, email, phone || null, role, provider]
      );
      const userId = result.insertId;

      await conn.query(
        `INSERT INTO user_settings (user_id) VALUES (?)`,
        [userId]
      );

      if (role === 'buyer') {
        await conn.query(
          `INSERT INTO carts (buyer_id) VALUES (?)`,
          [userId]
        );
      }

      await conn.commit();
      await admin.auth().setCustomUserClaims(uid, { role });

      return res.status(201).json({
        id: userId,
        firebase_uid: uid,
        full_name,
        email,
        phone: phone || null,
        role,
        auth_provider: provider
      });
    } catch (err) {
      await conn.rollback();
      throw err;
    } finally {
      conn.release();
    }
  } catch (err) {
    console.error('[auth/register]', err.message);
    return res.status(err.status || 500).json({ error: err.message });
  }
});

/* POST /api/auth/login */
router.post('/login', async (req, res) => {
  try {
    const decoded = await verifyToken(req);

    const [rows] = await pool.query(
      `SELECT id, firebase_uid, full_name, email, phone, role, is_active, created_at
         FROM users WHERE firebase_uid = ? LIMIT 1`,
      [decoded.uid]
    );

    if (rows.length === 0) {
      return res.status(404).json({
        error: 'User not registered in Pastimes. Please register first.'
      });
    }
    if (!rows[0].is_active) {
      return res.status(403).json({ error: 'Account is deactivated' });
    }
    return res.json(rows[0]);
  } catch (err) {
    console.error('[auth/login]', err.message);
    return res.status(err.status || 401).json({ error: err.message });
  }
});

/* POST /api/auth/google */
router.post('/google', async (req, res) => {
  try {
    const decoded = await verifyToken(req);

    const [rows] = await pool.query(
      `SELECT id, firebase_uid, full_name, email, phone, role, is_active, created_at
         FROM users WHERE firebase_uid = ? LIMIT 1`,
      [decoded.uid]
    );

    if (rows.length === 0) {
      return res.json({
        needsRegistration: true,
        email: decoded.email,
        suggestedName: decoded.name || ''
      });
    }
    if (!rows[0].is_active) {
      return res.status(403).json({ error: 'Account is deactivated' });
    }
    return res.json(rows[0]);
  } catch (err) {
    console.error('[auth/google]', err.message);
    return res.status(err.status || 401).json({ error: err.message });
  }
});

/* GET /api/auth/me */
router.get('/me', authRequired, async (req, res) => {
  try {
    const [settingsRows] = await pool.query(
      `SELECT theme, notifications_enabled, language
         FROM user_settings WHERE user_id = ? LIMIT 1`,
      [req.user.id]
    );
    return res.json({
      ...req.user,
      settings: settingsRows[0] || null
    });
  } catch (err) {
    console.error('[auth/me]', err.message);
    return res.status(500).json({ error: err.message });
  }
});

module.exports = router;