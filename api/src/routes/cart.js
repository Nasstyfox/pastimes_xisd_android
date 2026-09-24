const express = require('express');
const { pool } = require('../config/db');
const { authRequired } = require('../middleware/auth');
const { requireRole } = require('../middleware/role');

const router = express.Router();

router.use(authRequired, requireRole('buyer'));

/* Helper: get the buyer's cart id (create if missing) */
async function getOrCreateCart(buyerId) {
  const [rows] = await pool.query(
    'SELECT id FROM carts WHERE buyer_id = ? LIMIT 1',
    [buyerId]
  );
  if (rows.length > 0) return rows[0].id;

  const [result] = await pool.query(
    'INSERT INTO carts (buyer_id) VALUES (?)',
    [buyerId]
  );
  return result.insertId;
}

/* GET /api/cart — return cart with items and totals */
router.get('/', async (req, res) => {
  try {
    const cartId = await getOrCreateCart(req.user.id);

    const [rows] = await pool.query(
      `SELECT ci.id AS cart_item_id, ci.added_at,
              i.id AS item_id, i.title, i.price, i.image_url,
              i.size, i.brand, i.colour, i.status,
              i.category_id, c.name AS category_name,
              i.seller_id, u.full_name AS seller_name
         FROM cart_items ci
         JOIN items i      ON i.id = ci.item_id
         JOIN categories c ON c.id = i.category_id
         JOIN users u      ON u.id = i.seller_id
        WHERE ci.cart_id = ?
        ORDER BY ci.added_at DESC`,
      [cartId]
    );

    const total = rows.reduce((sum, r) => sum + Number(r.price), 0);
    return res.json({
      cart_id: cartId,
      item_count: rows.length,
      total: Number(total.toFixed(2)),
      items: rows
    });
  } catch (err) {
    console.error('[cart/get]', err.message);
    return res.status(500).json({ error: err.message });
  }
});

/* POST /api/cart/items — body: { item_id } */
router.post('/items', async (req, res) => {
  const { item_id } = req.body || {};
  if (!item_id) {
    return res.status(400).json({ error: 'item_id is required' });
  }

  try {
    const [itemRows] = await pool.query(
      'SELECT id, status FROM items WHERE id = ? LIMIT 1',
      [item_id]
    );
    if (itemRows.length === 0) {
      return res.status(404).json({ error: 'Item not found' });
    }
    if (itemRows[0].status !== 'available') {
      return res.status(400).json({ error: `Item is not available (status: ${itemRows[0].status})` });
    }

    const cartId = await getOrCreateCart(req.user.id);

    try {
      await pool.query(
        'INSERT INTO cart_items (cart_id, item_id) VALUES (?, ?)',
        [cartId, item_id]
      );
    } catch (e) {
      if (e.code === 'ER_DUP_ENTRY') {
        return res.status(409).json({ error: 'Item is already in your cart' });
      }
      throw e;
    }

    return res.status(201).json({ ok: true, item_id: Number(item_id) });
  } catch (err) {
    console.error('[cart/add]', err.message);
    return res.status(500).json({ error: err.message });
  }
});

/* DELETE /api/cart/items/:itemId */
router.delete('/items/:itemId', async (req, res) => {
  try {
    const cartId = await getOrCreateCart(req.user.id);
    const [result] = await pool.query(
      'DELETE FROM cart_items WHERE cart_id = ? AND item_id = ?',
      [cartId, req.params.itemId]
    );
    if (result.affectedRows === 0) {
      return res.status(404).json({ error: 'Item not in cart' });
    }
    return res.json({ ok: true });
  } catch (err) {
    console.error('[cart/remove]', err.message);
    return res.status(500).json({ error: err.message });
  }
});

/* DELETE /api/cart — clear all */
router.delete('/', async (req, res) => {
  try {
    const cartId = await getOrCreateCart(req.user.id);
    await pool.query('DELETE FROM cart_items WHERE cart_id = ?', [cartId]);
    return res.json({ ok: true });
  } catch (err) {
    console.error('[cart/clear]', err.message);
    return res.status(500).json({ error: err.message });
  }
});

module.exports = router;