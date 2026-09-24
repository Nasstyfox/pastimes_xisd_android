require('dotenv').config();

const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');

const { testConnection } = require('./config/db');
require('./config/firebase'); // initialize on boot

const app = express();

app.use(helmet());
app.use(cors());
app.use(express.json({ limit: '5mb' }));
app.use(morgan(process.env.NODE_ENV === 'production' ? 'combined' : 'dev'));

// Health check
app.get('/health', (req, res) => {
  res.json({ status: 'ok', ts: new Date().toISOString() });
});

// Routes will be mounted here in Step 4+
app.use('/api/auth',     require('./routes/auth'));
app.use('/api/categories', require('./routes/categories'));
app.use('/api/items', require('./routes/items'));
// app.use('/api/items',    require('./routes/items'));
// app.use('/api/cart',     require('./routes/cart'));
// app.use('/api/orders',   require('./routes/orders'));
// app.use('/api/seller',   require('./routes/seller'));
// app.use('/api/admin',    require('./routes/admin'));
// app.use('/api/settings', require('./routes/settings'));

// 404
app.use((req, res) => res.status(404).json({ error: 'Not found' }));

// Error handler
app.use((err, req, res, next) => {
  console.error('[error]', err);
  res.status(err.status || 500).json({ error: err.message || 'Server error' });
});

const PORT = process.env.PORT || 4000;

(async () => {
  try {
    await testConnection();
  } catch (e) {
    console.error('[boot] DB connection failed:', e.message);
    process.exit(1);
  }
  app.listen(PORT, () => console.log(`[server] Listening on :${PORT}`));
})();

module.exports = app;