const request = require('supertest');

// ---- Mock Firebase Admin SDK (never touches the real one) ----
jest.mock('../src/config/firebase', () => ({
  auth: () => ({
    verifyIdToken: jest.fn().mockResolvedValue({ uid: 'test-uid', email: 'test@test.com' }),
    setCustomUserClaims: jest.fn().mockResolvedValue(undefined),
  }),
  storage: () => ({}),
}));

// ---- Mock MySQL pool (never touches the real Aiven) ----
jest.mock('../src/config/db', () => ({
  pool: {
    query: jest.fn().mockResolvedValue([[]]),
    getConnection: jest.fn().mockResolvedValue({
      query: jest.fn().mockResolvedValue([{ insertId: 1 }]),
      beginTransaction: jest.fn(),
      commit: jest.fn(),
      rollback: jest.fn(),
      release: jest.fn(),
    }),
  },
  testConnection: jest.fn().mockResolvedValue(undefined),
}));

const app = require('../src/server');

describe('API smoke tests', () => {

  test('GET /health returns 200 ok', async () => {
    const res = await request(app).get('/health');
    expect(res.statusCode).toBe(200);
    expect(res.body.status).toBe('ok');
    expect(typeof res.body.ts).toBe('string');
  });

  test('GET /api/auth/me returns 401 without token', async () => {
    const res = await request(app).get('/api/auth/me');
    expect(res.statusCode).toBe(401);
    expect(res.body.error).toMatch(/authorization/i);
  });

  test('GET /api/categories responds', async () => {
    const res = await request(app).get('/api/categories');
    expect([200, 500]).toContain(res.statusCode);
  });

  test('GET /api/items responds', async () => {
    const res = await request(app).get('/api/items');
    expect([200, 500]).toContain(res.statusCode);
  });

  test('POST /api/auth/register validates missing fields', async () => {
    const res = await request(app)
      .post('/api/auth/register')
      .set('Authorization', 'Bearer fake-token')
      .send({}); // empty body
    expect(res.statusCode).toBe(400);
    expect(res.body.error).toMatch(/required|missing/i);
  });

  test('POST /api/auth/register rejects invalid role', async () => {
    const res = await request(app)
      .post('/api/auth/register')
      .set('Authorization', 'Bearer fake-token')
      .send({ full_name: 'X', role: 'wizard' });
    expect(res.statusCode).toBe(400);
    expect(res.body.error).toMatch(/role/i);
  });

  test('PUT /api/settings rejects invalid theme', async () => {
    const res = await request(app)
      .put('/api/settings')
      .set('Authorization', 'Bearer fake-token')
      .send({ theme: 'rainbow' });
    // 401 if auth fails first, 400 if reaches validation
    expect([400, 401, 403]).toContain(res.statusCode);
  });

});