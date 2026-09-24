const express = require('express');
const { pool } = require('../config/db');

const router = express.Router();

/* GET /api/categories — public */
router.get('/', async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT id, name, description
         FROM categories
        WHERE is_active = 1
        ORDER BY name ASC`
    );
    return res.json(rows);
  } catch (err) {
    console.error('[categories/list]', err.message);
    return res.status(500).json({ error: err.message });
  }
});

module.exports = router;