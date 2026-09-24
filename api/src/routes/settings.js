const express = require('express');
const { pool } = require('../config/db');
const { authRequired } = require('../middleware/auth');

const router = express.Router();

router.use(authRequired);

const ALLOWED_THEMES = ['light', 'dark', 'system'];

/* GET /api/settings — current user's settings */
router.get('/', async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT id, theme, notifications_enabled, language, updated_at
         FROM user_settings WHERE user_id = ? LIMIT 1`,
      [req.user.id]
    );

    if (rows.length === 0) {
      // Safety net: create default row if somehow missing
      await pool.query(
        'INSERT INTO user_settings (user_id) VALUES (?)',
        [req.user.id]
      );
      const [created] = await pool.query(
        `SELECT id, theme, notifications_enabled, language, updated_at
           FROM user_settings WHERE user_id = ? LIMIT 1`,
        [req.user.id]
      );
      return res.json(created[0]);
    }

    return res.json(rows[0]);
  } catch (err) {
    console.error('[settings/get]', err.message);
    return res.status(500).json({ error: err.message });
  }
});

/* PUT /api/settings — update one or more fields */
/* Body: { theme?, notifications_enabled?, language? } */
router.put('/', async (req, res) => {
  const { theme, notifications_enabled, language } = req.body || {};

  if (theme !== undefined && !ALLOWED_THEMES.includes(theme)) {
    return res.status(400).json({
      error: `theme must be one of: ${ALLOWED_THEMES.join(', ')}`
    });
  }
  if (language !== undefined && (typeof language !== 'string' || language.length < 2)) {
    return res.status(400).json({ error: 'language must be a valid code like "en", "zu"' });
  }

  const updates = {};
  if (theme !== undefined) updates.theme = theme;
  if (notifications_enabled !== undefined) updates.notifications_enabled = notifications_enabled ? 1 : 0;
  if (language !== undefined) updates.language = language;

  if (Object.keys(updates).length === 0) {
    return res.status(400).json({ error: 'Nothing to update' });
  }

  try {
    // Ensure row exists
    const [existing] = await pool.query(
      'SELECT id FROM user_settings WHERE user_id = ? LIMIT 1',
      [req.user.id]
    );
    if (existing.length === 0) {
      await pool.query(
        'INSERT INTO user_settings (user_id) VALUES (?)',
        [req.user.id]
      );
    }

    const setClause = Object.keys(updates).map(k => `${k} = ?`).join(', ');
    const values = Object.values(updates);

    await pool.query(
      `UPDATE user_settings SET ${setClause} WHERE user_id = ?`,
      [...values, req.user.id]
    );

    const [rows] = await pool.query(
      `SELECT id, theme, notifications_enabled, language, updated_at
         FROM user_settings WHERE user_id = ? LIMIT 1`,
      [req.user.id]
    );
    return res.json(rows[0]);
  } catch (err) {
    console.error('[settings/update]', err.message);
    return res.status(500).json({ error: err.message });
  }
});

module.exports = router;