const express = require('express');
const { pool } = require('../config/db');
const { authRequired } = require('../middleware/auth');
const { requireRole } = require('../middleware/role');

const router = express.Router();

/* All routes here are buyer-only */
router.use(authRequired, requireRole('buyer'));

/* GET /api/addresses */
router.get('/', async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT * FROM user_addresses
        WHERE user_id = ?
        ORDER BY is_default DESC, created_at DESC`,
      [req.user.id]
    );
    return res.json(rows);
  } catch (err) {
    console.error('[addresses/list]', err.message);
    return res.status(500).json({ error: err.message });
  }
});

/* POST /api/addresses */
router.post('/', async (req, res) => {
  const {
    label, recipient, phone, street, suburb,
    city, province, postal_code, country, is_default
  } = req.body || {};

  if (!recipient || !phone || !street || !suburb || !city || !province || !postal_code) {
    return res.status(400).json({
      error: 'recipient, phone, street, suburb, city, province, postal_code are required'
    });
  }

  const conn = await pool.getConnection();
  try {
    await conn.beginTransaction();

    if (is_default) {
      await conn.query(
        `UPDATE user_addresses SET is_default = 0 WHERE user_id = ?`,
        [req.user.id]
      );
    }

    const [result] = await conn.query(
      `INSERT INTO user_addresses
        (user_id, label, recipient, phone, street, suburb,
         city, province, postal_code, country, is_default)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        req.user.id, label || null, recipient, phone, street, suburb,
        city, province, postal_code, country || 'South Africa',
        is_default ? 1 : 0
      ]
    );

    await conn.commit();

    const [rows] = await conn.query(
      'SELECT * FROM user_addresses WHERE id = ?',
      [result.insertId]
    );
    return res.status(201).json(rows[0]);
  } catch (err) {
    await conn.rollback();
    console.error('[addresses/create]', err.message);
    return res.status(500).json({ error: err.message });
  } finally {
    conn.release();
  }
});

/* PUT /api/addresses/:id */
router.put('/:id', async (req, res) => {
  try {
    const [existing] = await pool.query(
      'SELECT * FROM user_addresses WHERE id = ? AND user_id = ?',
      [req.params.id, req.user.id]
    );
    if (existing.length === 0) {
      return res.status(404).json({ error: 'Address not found' });
    }

    const fields = ['label', 'recipient', 'phone', 'street', 'suburb',
                    'city', 'province', 'postal_code', 'country', 'is_default'];
    const updates = {};
    for (const f of fields) {
      if (req.body[f] !== undefined) updates[f] = req.body[f];
    }

    if (Object.keys(updates).length === 0) {
      return res.status(400).json({ error: 'Nothing to update' });
    }

    const conn = await pool.getConnection();
    try {
      await conn.beginTransaction();

      if (updates.is_default) {
        await conn.query(
          `UPDATE user_addresses SET is_default = 0 WHERE user_id = ?`,
          [req.user.id]
        );
      }

      const setClause = Object.keys(updates).map(k => `${k} = ?`).join(', ');
      const values = Object.values(updates).map(v => (v === true ? 1 : v === false ? 0 : v));

      await conn.query(
        `UPDATE user_addresses SET ${setClause} WHERE id = ?`,
        [...values, req.params.id]
      );

      await conn.commit();
    } catch (e) {
      await conn.rollback();
      throw e;
    } finally {
      conn.release();
    }

    const [rows] = await pool.query('SELECT * FROM user_addresses WHERE id = ?', [req.params.id]);
    return res.json(rows[0]);
  } catch (err) {
    console.error('[addresses/update]', err.message);
    return res.status(500).json({ error: err.message });
  }
});

/* DELETE /api/addresses/:id */
router.delete('/:id', async (req, res) => {
  try {
    const [result] = await pool.query(
      'DELETE FROM user_addresses WHERE id = ? AND user_id = ?',
      [req.params.id, req.user.id]
    );
    if (result.affectedRows === 0) {
      return res.status(404).json({ error: 'Address not found' });
    }
    return res.json({ ok: true });
  } catch (err) {
    console.error('[addresses/delete]', err.message);
    return res.status(500).json({ error: err.message });
  }
});

module.exports = router;