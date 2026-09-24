require('dotenv').config();

console.log('BUCKET:', process.env.FIREBASE_STORAGE_BUCKET);
console.log('DB_NAME:', process.env.DB_NAME);

try {
  const sa = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
  console.log('OK. project_id:', sa.project_id);
  console.log('client_email:', sa.client_email);
  console.log('private_key length:', sa.private_key.length);
} catch (e) {
  console.error('PARSE ERROR:', e.message);
}