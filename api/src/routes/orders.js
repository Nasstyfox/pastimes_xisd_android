const express = require('express');
const { pool } = require('../config/db');
const { authRequired } = require('../middleware/auth');
const { requireRole } = require('../middleware/role');

const router = express.Router();

router.use(authRequired, requireRole('buyer'));

/* ------------------------------------------------------------------ */
/* POST /api/orders — checkout                                         */
/* Body: { address_id }  OR  { ship_recipient, ship_phone,             */
/*        ship_street, ship_suburb, ship_city, ship_province,          */
/*        ship_postal_code, ship_country? }                            */
/* ------------------------------------------------------------------ */
router.post('/', async (req, res) => {
  const { address_id } = req.body || {};

  const conn = await pool.getConnection();
  try {
    await conn.beginTransaction();

    /* 1. Resolve shipping snapshot */
    let snap;
    if (address_id) {
      const [addrRows] = await conn.query(
        'SELECT * FROM user_addresses WHERE id = ? AND user_id = ? LIMIT 1',
        [address_id, req.user.id]
      );
      if (addrRows.length === 0) {
        await conn.rollback();
        return res.status(400).json({ error: 'Address not found' });
      }
      const a = addrRows[0];
      snap = {
        recipient: a.recipient, phone: a.phone, street: a.street,
        suburb: a.suburb, city: a.city, province: a.province,
        postal_code: a.postal_code, country: a.country || 'South Africa'
      };
    } else {
      const b = req.body;
      if (!b.ship_recipient || !b.ship_phone || !b.ship_street || !b.ship_suburb ||
          !b.ship_city || !b.ship_province || !b.ship_postal_code) {
        await conn.rollback();
        return res.status(400).json({
          error: 'Either address_id or full ship_* fields are required'
        });
      }
      snap = {
        recipient: b.ship_recipient, phone: b.ship_phone, street: b.ship_street,
        suburb: b.ship_suburb, city: b.ship_city, province: b.ship_province,
        postal_code: b.ship_postal_code, country: b.ship_country || 'South Africa'
      };
    }

    /* 2. Load cart + items (lock rows to avoid race) */
    const [cartRows] = await conn.query(
      'SELECT id FROM carts WHERE buyer_id = ? LIMIT 1',
      [req.user.id]
    );
    if (cartRows.length === 0) {
      await conn.rollback();
      return res.status(400).json({ error: 'Cart is empty' });
    }
    const cartId = cartRows[0].id;

    const [items] = await conn.query(
      `SELECT ci.item_id, i.price, i.seller_id, i.status
         FROM cart_items ci
         JOIN items i ON i.id = ci.item_id
        WHERE ci.cart_id = ?
        FOR UPDATE`,
      [cartId]
    );

    if (items.length === 0) {
      await conn.rollback();
      return res.status(400).json({ error: 'Cart is empty' });
    }

    /* 3. Verify all still available */
    for (const it of items) {
      if (it.status !== 'available') {
        await conn.rollback();
        return res.status(409).json({
          error: `Item #${it.item_id} is no longer available (status: ${it.status})`
        });
      }
    }

    /* 4. Compute total */
    const total = items.reduce((sum, it) => sum + Number(it.price), 0);

    /* 5. Insert order */
    const [orderResult] = await conn.query(
      `INSERT INTO orders
        (buyer_id, total_amount, status,
         ship_recipient, ship_phone, ship_street, ship_suburb,
         ship_city, ship_province, ship_postal_code, ship_country)
       VALUES (?, ?, 'paid', ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        req.user.id, total.toFixed(2),
        snap.recipient, snap.phone, snap.street, snap.suburb,
        snap.city, snap.province, snap.postal_code, snap.country
      ]
    );
    const orderId = orderResult.insertId;

    /* 6. Insert order_items + mark items sold */
    for (const it of items) {
      await conn.query(
        `INSERT INTO order_items (order_id, item_id, seller_id, price_at_purchase)
         VALUES (?, ?, ?, ?)`,
        [orderId, it.item_id, it.seller_id, it.price]
      );
      await conn.query(
        `UPDATE items SET status = 'sold' WHERE id = ?`,
        [it.item_id]
      );
    }

    /* 7. Clear the cart */
    await conn.query('DELETE FROM cart_items WHERE cart_id = ?', [cartId]);

    await conn.commit();

    /* 8. Return the created order with items */
    const [order] = await pool.query(
      'SELECT * FROM orders WHERE id = ?',
      [orderId]
    );
    const [orderItems] = await pool.query(
      `SELECT oi.*, i.title, i.image_url
         FROM order_items oi
         JOIN items i ON i.id = oi.item_id
        WHERE oi.order_id = ?`,
      [orderId]
    );

    return res.status(201).json({ ...order[0], items: orderItems });
  } catch (err) {
    await conn.rollback();
    console.error('[orders/create]', err.message);
    return res.status(500).json({ error: err.message });
  } finally {
    conn.release();
  }
});

/* GET /api/orders — buyer's order history */
router.get('/', async (req, res) => {
  try {
    const [orders] = await pool.query(
      `SELECT * FROM orders WHERE buyer_id = ? ORDER BY created_at DESC`,
      [req.user.id]
    );

    if (orders.length === 0) return res.json([]);

    const orderIds = orders.map(o => o.id);
    const [items] = await pool.query(
      `SELECT oi.*, i.title, i.image_url
         FROM order_items oi
         JOIN items i ON i.id = oi.item_id
        WHERE oi.order_id IN (?)`,
      [orderIds]
    );

    const grouped = orders.map(o => ({
      ...o,
      items: items.filter(it => it.order_id === o.id)
    }));
    return res.json(grouped);
  } catch (err) {
    console.error('[orders/list]', err.message);
    return res.status(500).json({ error: err.message });
  }
});

/* GET /api/orders/:id */
router.get('/:id', async (req, res) => {
  try {
    const [orders] = await pool.query(
      'SELECT * FROM orders WHERE id = ? AND buyer_id = ? LIMIT 1',
      [req.params.id, req.user.id]
    );
    if (orders.length === 0) {
      return res.status(404).json({ error: 'Order not found' });
    }

    const [items] = await pool.query(
      `SELECT oi.*, i.title, i.image_url
         FROM order_items oi
         JOIN items i ON i.id = oi.item_id
        WHERE oi.order_id = ?`,
      [req.params.id]
    );

    return res.json({ ...orders[0], items });
  } catch (err) {
    console.error('[orders/detail]', err.message);
    return res.status(500).json({ error: err.message });
  }
});

module.exports = router;