const express = require('express');
const { pool } = require('../config/db');
const { authRequired } = require('../middleware/auth');
const { requireRole } = require('../middleware/role');

const router = express.Router();

/* ------------------------------------------------------------------ */
/* Helpers                                                             */
/* ------------------------------------------------------------------ */
const ALLOWED_CONDITIONS = ['new', 'like_new', 'good', 'fair'];

function isPositiveNumber(n) {
  return typeof n === 'number' && isFinite(n) && n > 0;
}

/* ------------------------------------------------------------------ */
/* GET /api/items                                                      */
/* Public. Buyers browse available items with filters.                 */
/* Query: ?category_id= &size= &min_price= &max_price= &q=             */
/* ------------------------------------------------------------------ */
router.get('/', async (req, res) => {
  try {
    const { category_id, size, min_price, max_price, q } = req.query;
    const where = [`i.status = 'available'`];
    const params = [];

    if (category_id) {
      where.push('i.category_id = ?');
      params.push(Number(category_id));
    }
    if (size) {
      where.push('i.size = ?');
      params.push(String(size));
    }
    if (min_price) {
      where.push('i.price >= ?');
      params.push(Number(min_price));
    }
    if (max_price) {
      where.push('i.price <= ?');
      params.push(Number(max_price));
    }
    if (q) {
      where.push('(i.title LIKE ? OR i.description LIKE ? OR i.brand LIKE ?)');
      const like = `%${q}%`;
      params.push(like, like, like);
    }

    const sql = `
      SELECT i.id, i.title, i.description, i.price, i.size, i.brand, i.colour,
             i.condition_tag, i.image_url, i.status, i.created_at,
             i.category_id, c.name AS category_name,
             i.seller_id, u.full_name AS seller_name
        FROM items i
        JOIN categories c ON c.id = i.category_id
        JOIN users u ON u.id = i.seller_id
       WHERE ${where.join(' AND ')}
       ORDER BY i.created_at DESC
       LIMIT 200`;

    const [rows] = await pool.query(sql, params);
    return res.json(rows);
  } catch (err) {
    console.error('[items/list]', err.message);
    return res.status(500).json({ error: err.message });
  }
});

/* ------------------------------------------------------------------ */
/* GET /api/items/mine                                                 */
/* Seller only. Query: ?status=all|available|sold|reserved|removed     */
/* MUST be declared BEFORE /:id or Express treats "mine" as an id.     */
/* ------------------------------------------------------------------ */
router.get('/mine', authRequired, requireRole('seller'), async (req, res) => {
  try {
    const status = req.query.status || 'all';
    const where = [`seller_id = ?`];
    const params = [req.user.id];

    if (status !== 'all') {
      where.push('status = ?');
      params.push(status);
    }

    const [rows] = await pool.query(
      `SELECT i.*, c.name AS category_name
         FROM items i
         JOIN categories c ON c.id = i.category_id
        WHERE ${where.join(' AND ')}
        ORDER BY i.created_at DESC`,
      params
    );
    return res.json(rows);
  } catch (err) {
    console.error('[items/mine]', err.message);
    return res.status(500).json({ error: err.message });
  }
});

/* ------------------------------------------------------------------ */
/* GET /api/items/:id  — public item detail                            */
/* ------------------------------------------------------------------ */
router.get('/:id', async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT i.*, c.name AS category_name, u.full_name AS seller_name
         FROM items i
         JOIN categories c ON c.id = i.category_id
         JOIN users u ON u.id = i.seller_id
        WHERE i.id = ? LIMIT 1`,
      [req.params.id]
    );
    if (rows.length === 0) {
      return res.status(404).json({ error: 'Item not found' });
    }
    return res.json(rows[0]);
  } catch (err) {
    console.error('[items/detail]', err.message);
    return res.status(500).json({ error: err.message });
  }
});

/* ------------------------------------------------------------------ */
/* POST /api/items — seller creates a listing                          */
/* Body: category_id, title, description, price, size, brand, colour,  */
/*       condition_tag, image_url                                      */
/* ------------------------------------------------------------------ */
router.post('/', authRequired, requireRole('seller'), async (req, res) => {
  const {
    category_id, title, description, price,
    size, brand, colour, condition_tag, image_url
  } = req.body || {};

  if (!category_id || !title || price == null) {
    return res.status(400).json({ error: 'category_id, title and price are required' });
  }
  if (!isPositiveNumber(Number(price))) {
    return res.status(400).json({ error: 'price must be a positive number' });
  }
  const cond = condition_tag || 'good';
  if (!ALLOWED_CONDITIONS.includes(cond)) {
    return res.status(400).json({ error: 'Invalid condition_tag' });
  }

  try {
    const [cat] = await pool.query(
      'SELECT id FROM categories WHERE id = ? LIMIT 1',
      [category_id]
    );
    if (cat.length === 0) {
      return res.status(400).json({ error: 'Invalid category_id' });
    }

    const [result] = await pool.query(
      `INSERT INTO items
        (seller_id, category_id, title, description, price,
         size, brand, colour, condition_tag, image_url)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        req.user.id,
        category_id,
        title,
        description || null,
        Number(price),
        size || null,
        brand || null,
        colour || null,
        cond,
        image_url || null
      ]
    );

    const [rows] = await pool.query(
      `SELECT i.*, c.name AS category_name
         FROM items i JOIN categories c ON c.id = i.category_id
        WHERE i.id = ?`,
      [result.insertId]
    );

    return res.status(201).json(rows[0]);
  } catch (err) {
    console.error('[items/create]', err.message);
    return res.status(500).json({ error: err.message });
  }
});

/* ------------------------------------------------------------------ */
/* PUT /api/items/:id — seller edits own item                          */
/* ------------------------------------------------------------------ */
router.put('/:id', authRequired, requireRole('seller'), async (req, res) => {
  try {
    const [existing] = await pool.query(
      'SELECT * FROM items WHERE id = ? LIMIT 1',
      [req.params.id]
    );
    if (existing.length === 0) {
      return res.status(404).json({ error: 'Item not found' });
    }
    if (existing[0].seller_id !== req.user.id) {
      return res.status(403).json({ error: 'Not your item' });
    }
    if (existing[0].status === 'sold') {
      return res.status(400).json({ error: 'Cannot edit a sold item' });
    }

    const {
      category_id, title, description, price,
      size, brand, colour, condition_tag, image_url, status
    } = req.body || {};

    if (condition_tag && !ALLOWED_CONDITIONS.includes(condition_tag)) {
      return res.status(400).json({ error: 'Invalid condition_tag' });
    }

    const updates = {};
    if (category_id != null) updates.category_id = category_id;
    if (title != null) updates.title = title;
    if (description !== undefined) updates.description = description;
    if (price != null) {
      if (!isPositiveNumber(Number(price))) {
        return res.status(400).json({ error: 'price must be positive' });
      }
      updates.price = Number(price);
    }
    if (size !== undefined) updates.size = size;
    if (brand !== undefined) updates.brand = brand;
    if (colour !== undefined) updates.colour = colour;
    if (condition_tag != null) updates.condition_tag = condition_tag;
    if (image_url !== undefined) updates.image_url = image_url;
    if (status && ['available', 'removed'].includes(status)) {
      updates.status = status;
    }

    const keys = Object.keys(updates);
    if (keys.length === 0) {
      return res.status(400).json({ error: 'Nothing to update' });
    }

    const setClause = keys.map(k => `${k} = ?`).join(', ');
    const values = keys.map(k => updates[k]);

    await pool.query(
      `UPDATE items SET ${setClause} WHERE id = ?`,
      [...values, req.params.id]
    );

    const [rows] = await pool.query(
      `SELECT i.*, c.name AS category_name
         FROM items i JOIN categories c ON c.id = i.category_id
        WHERE i.id = ?`,
      [req.params.id]
    );
    return res.json(rows[0]);
  } catch (err) {
    console.error('[items/update]', err.message);
    return res.status(500).json({ error: err.message });
  }
});

/* ------------------------------------------------------------------ */
/* DELETE /api/items/:id — seller soft-deletes own item                */
/* ------------------------------------------------------------------ */
router.delete('/:id', authRequired, requireRole('seller'), async (req, res) => {
  try {
    const [existing] = await pool.query(
      'SELECT id, seller_id, status FROM items WHERE id = ? LIMIT 1',
      [req.params.id]
    );
    if (existing.length === 0) {
      return res.status(404).json({ error: 'Item not found' });
    }
    if (existing[0].seller_id !== req.user.id) {
      return res.status(403).json({ error: 'Not your item' });
    }
    if (existing[0].status === 'sold') {
      return res.status(400).json({ error: 'Cannot remove a sold item' });
    }

    await pool.query(
      `UPDATE items SET status = 'removed' WHERE id = ?`,
      [req.params.id]
    );
    return res.json({ ok: true, id: Number(req.params.id), status: 'removed' });
  } catch (err) {
    console.error('[items/delete]', err.message);
    return res.status(500).json({ error: err.message });
  }
});

module.exports = router;