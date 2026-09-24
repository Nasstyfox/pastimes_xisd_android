const express = require('express');
const { pool } = require('../config/db');
const { authRequired } = require('../middleware/auth');
const { requireRole } = require('../middleware/role');

const router = express.Router();

/* Every route here requires an authenticated admin */
router.use(authRequired, requireRole('admin'));

/* Helper: write an audit log entry */
async function audit(adminId, action, targetUserId, details) {
  try {
    await pool.query(
      `INSERT INTO audit_logs (admin_id, action, target_user_id, details)
       VALUES (?, ?, ?, ?)`,
      [adminId, action, targetUserId || null, details || null]
    );
  } catch (e) {
    console.error('[audit] failed to log:', e.message);
  }
}

/* ------------------------------------------------------------------ */
/* GET /api/admin/users                                                */
/* Optional filters: ?role=buyer|seller|admin  ?active=1|0             */
/* ------------------------------------------------------------------ */
router.get('/users', async (req, res) => {
  try {
    const where = [];
    const params = [];

    if (req.query.role) {
      where.push('role = ?');
      params.push(req.query.role);
    }
    if (req.query.active === '1' || req.query.active === '0') {
      where.push('is_active = ?');
      params.push(Number(req.query.active));
    }

    const sql = `
      SELECT id, full_name, email, phone, role, auth_provider,
             is_active, created_at
        FROM users
       ${where.length ? 'WHERE ' + where.join(' AND ') : ''}
       ORDER BY created_at DESC`;

    const [rows] = await pool.query(sql, params);
    return res.json(rows);
  } catch (err) {
    console.error('[admin/users]', err.message);
    return res.status(500).json({ error: err.message });
  }
});

/* ------------------------------------------------------------------ */
/* GET /api/admin/users/:id                                            */
/* User detail "card" — includes a small summary per role              */
/* ------------------------------------------------------------------ */
router.get('/users/:id', async (req, res) => {
  try {
    const [users] = await pool.query(
      `SELECT id, firebase_uid, full_name, email, phone, role,
              auth_provider, is_active, created_at, updated_at
         FROM users WHERE id = ? LIMIT 1`,
      [req.params.id]
    );
    if (users.length === 0) {
      return res.status(404).json({ error: 'User not found' });
    }
    const user = users[0];

    let summary = null;

    if (user.role === 'buyer') {
      const [[row]] = await pool.query(
        `SELECT COUNT(*) AS order_count,
                COALESCE(SUM(total_amount), 0) AS total_spent
           FROM orders
          WHERE buyer_id = ?`,
        [user.id]
      );
      summary = {
        order_count: Number(row.order_count),
        total_spent: Number(row.total_spent)
      };
    } else if (user.role === 'seller') {
      const [[row]] = await pool.query(
        `SELECT
           (SELECT COUNT(*) FROM items WHERE seller_id = ?) AS total_items,
           (SELECT COUNT(*) FROM items WHERE seller_id = ? AND status = 'available') AS available_items,
           (SELECT COUNT(*) FROM items WHERE seller_id = ? AND status = 'sold') AS sold_items,
           (SELECT COALESCE(SUM(price_at_purchase), 0)
              FROM order_items WHERE seller_id = ?) AS total_earnings`,
        [user.id, user.id, user.id, user.id]
      );
      summary = {
        total_items: Number(row.total_items),
        available_items: Number(row.available_items),
        sold_items: Number(row.sold_items),
        total_earnings: Number(row.total_earnings)
      };
    }

    const [settingsRows] = await pool.query(
      `SELECT theme, notifications_enabled, language
         FROM user_settings WHERE user_id = ? LIMIT 1`,
      [user.id]
    );

    return res.json({
      ...user,
      settings: settingsRows[0] || null,
      summary
    });
  } catch (err) {
    console.error('[admin/user-detail]', err.message);
    return res.status(500).json({ error: err.message });
  }
});

/* ------------------------------------------------------------------ */
/* GET /api/admin/users/:id/transactions  — buyer's order history     */
/* ------------------------------------------------------------------ */
router.get('/users/:id/transactions', async (req, res) => {
  try {
    const [users] = await pool.query(
      'SELECT id, role FROM users WHERE id = ? LIMIT 1',
      [req.params.id]
    );
    if (users.length === 0) {
      return res.status(404).json({ error: 'User not found' });
    }
    if (users[0].role !== 'buyer') {
      return res.status(400).json({ error: 'Transactions are only for buyers' });
    }

    const [orders] = await pool.query(
      `SELECT * FROM orders WHERE buyer_id = ? ORDER BY created_at DESC`,
      [req.params.id]
    );

    if (orders.length === 0) return res.json([]);

    const orderIds = orders.map(o => o.id);
    const [items] = await pool.query(
      `SELECT oi.*, i.title, i.image_url, u.full_name AS seller_name
         FROM order_items oi
         JOIN items i ON i.id = oi.item_id
         JOIN users u ON u.id = oi.seller_id
        WHERE oi.order_id IN (?)`,
      [orderIds]
    );

    const grouped = orders.map(o => ({
      ...o,
      items: items.filter(it => it.order_id === o.id)
    }));

    return res.json(grouped);
  } catch (err) {
    console.error('[admin/transactions]', err.message);
    return res.status(500).json({ error: err.message });
  }
});

/* ------------------------------------------------------------------ */
/* GET /api/admin/users/:id/earnings — seller's sold items + total    */
/* ------------------------------------------------------------------ */
router.get('/users/:id/earnings', async (req, res) => {
  try {
    const [users] = await pool.query(
      'SELECT id, role FROM users WHERE id = ? LIMIT 1',
      [req.params.id]
    );
    if (users.length === 0) {
      return res.status(404).json({ error: 'User not found' });
    }
    if (users[0].role !== 'seller') {
      return res.status(400).json({ error: 'Earnings are only for sellers' });
    }

    const [rows] = await pool.query(
      `SELECT oi.id, oi.order_id, oi.item_id, oi.price_at_purchase,
              oi.created_at, i.title, i.image_url,
              o.id AS order_ref, o.buyer_id,
              b.full_name AS buyer_name
         FROM order_items oi
         JOIN items i ON i.id = oi.item_id
         JOIN orders o ON o.id = oi.order_id
         JOIN users b ON b.id = o.buyer_id
        WHERE oi.seller_id = ?
        ORDER BY oi.created_at DESC`,
      [req.params.id]
    );

    const total = rows.reduce((s, r) => s + Number(r.price_at_purchase), 0);
    return res.json({
      total_earnings: Number(total.toFixed(2)),
      sold_count: rows.length,
      sales: rows
    });
  } catch (err) {
    console.error('[admin/earnings]', err.message);
    return res.status(500).json({ error: err.message });
  }
});

/* ------------------------------------------------------------------ */
/* POST /api/admin/users/:id/reset-password                            */
/* Sends a Firebase password reset email to the user                    */
/* ------------------------------------------------------------------ */
router.post('/users/:id/reset-password', async (req, res) => {
  try {
    const [users] = await pool.query(
      'SELECT id, email, full_name FROM users WHERE id = ? LIMIT 1',
      [req.params.id]
    );
    if (users.length === 0) {
      return res.status(404).json({ error: 'User not found' });
    }
    const target = users[0];

    const apiKey = process.env.FIREBASE_WEB_API_KEY;
    if (!apiKey) {
      return res.status(500).json({ error: 'FIREBASE_WEB_API_KEY not configured' });
    }

    const url = `https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=${apiKey}`;
    const resp = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ requestType: 'PASSWORD_RESET', email: target.email })
    });

    const data = await resp.json();
    if (!resp.ok) {
      console.error('[admin/reset] Firebase error:', data);
      return res.status(500).json({
        error: data.error?.message || 'Failed to send reset email'
      });
    }

    await audit(req.user.id, 'RESET_PASSWORD', target.id,
      `Sent password reset email to ${target.email}`);

    return res.json({
      ok: true,
      message: `Password reset email sent to ${target.email}`,
      email: target.email
    });
  } catch (err) {
    console.error('[admin/reset]', err.message);
    return res.status(500).json({ error: err.message });
  }
});

/* ------------------------------------------------------------------ */
/* POST /api/admin/users/:id/toggle-active  — deactivate/reactivate    */
/* ------------------------------------------------------------------ */
router.post('/users/:id/toggle-active', async (req, res) => {
  try {
    const [users] = await pool.query(
      'SELECT id, is_active, role FROM users WHERE id = ? LIMIT 1',
      [req.params.id]
    );
    if (users.length === 0) {
      return res.status(404).json({ error: 'User not found' });
    }
    if (users[0].id === req.user.id) {
      return res.status(400).json({ error: 'Cannot deactivate your own account' });
    }

    const newState = users[0].is_active ? 0 : 1;
    await pool.query(
      'UPDATE users SET is_active = ? WHERE id = ?',
      [newState, req.params.id]
    );

    await audit(
      req.user.id,
      newState ? 'ACTIVATE_USER' : 'DEACTIVATE_USER',
      users[0].id,
      `Role: ${users[0].role}`
    );

    return res.json({ ok: true, is_active: newState });
  } catch (err) {
    console.error('[admin/toggle]', err.message);
    return res.status(500).json({ error: err.message });
  }
});

/* ------------------------------------------------------------------ */
/* GET /api/admin/audit-logs                                           */
/* ------------------------------------------------------------------ */
router.get('/audit-logs', async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT al.id, al.action, al.details, al.created_at,
              al.admin_id, a.full_name AS admin_name,
              al.target_user_id, t.full_name AS target_name
         FROM audit_logs al
         LEFT JOIN users a ON a.id = al.admin_id
         LEFT JOIN users t ON t.id = al.target_user_id
        ORDER BY al.created_at DESC
        LIMIT 200`
    );
    return res.json(rows);
  } catch (err) {
    console.error('[admin/audit]', err.message);
    return res.status(500).json({ error: err.message });
  }
});

module.exports = router;