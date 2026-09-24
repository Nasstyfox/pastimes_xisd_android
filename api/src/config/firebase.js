const { initializeApp, cert, getApps } = require('firebase-admin/app');
const { getAuth } = require('firebase-admin/auth');
const { getStorage } = require('firebase-admin/storage');

if (!getApps().length) {
  const raw = process.env.FIREBASE_SERVICE_ACCOUNT;
  if (!raw) {
    throw new Error('FIREBASE_SERVICE_ACCOUNT env var is missing');
  }
  const serviceAccount = JSON.parse(raw);

  initializeApp({
    credential: cert(serviceAccount),
    storageBucket: process.env.FIREBASE_STORAGE_BUCKET
  });

  console.log('[firebase] Initialized');
}

module.exports = {
  auth: getAuth,       // usage: admin.auth().verifyIdToken(token)
  storage: getStorage  // usage: admin.storage().bucket()
};