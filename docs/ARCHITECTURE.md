# Pastimes — Architecture

## High-level flow


┌──────────────────────────┐
│ Android App │
│ Kotlin + Jetpack Compose│
└────────────┬─────────────┘
│
┌────────┼────────────────┬────────────────────┐
│ │ │ │
▼ ▼ ▼ ▼
┌────────┐ ┌──────────┐ ┌──────────┐ ┌────────────────┐
│Firebase│ │Cloudinary│ │Retrofit +│ │Coil (image │
│Auth SDK│ │SDK │ │OkHttp │ │loading) │
└───┬────┘ └────┬─────┘ └─────┬────┘ └────────────────┘
│ │ │
│ │ ▼
│ │ ┌──────────────────────┐
│ │ │ Node.js + Express │
│ │ │ API (Render) │
│ │ └────┬─────────────┬───┘
│ │ │ │
▼ ▼ ▼ ▼
┌──────────┐ ┌────────┐ ┌──────────┐ ┌──────────┐
│ Firebase │ │Cloudin-│ │ Firebase │ │ MySQL 8 │
│ Auth │ │ary │ │ Admin SDK│ │ (Aiven) │
│ (online) │ │(images)│ │(verifies │ │ Pastimes │
│ │ │ │ │ tokens) │ │ DB │
└──────────┘ └────────┘ └──────────┘ └──────────┘


## Authentication flow

1. User submits email + password on the **Login screen**.
2. Android calls `FirebaseAuth.signInWithEmailAndPassword` via the Firebase SDK.
3. Firebase returns an **ID token** (JWT, valid for 1 hour).
4. Every subsequent API request sends `Authorization: Bearer <id_token>`.
5. The API verifies the token with the **Firebase Admin SDK** (using the service account from `FIREBASE_SERVICE_ACCOUNT` env var).
6. The API looks up the user's row in MySQL by `firebase_uid` and attaches it to `req.user`.
7. Route handlers check `req.user.role` (`buyer` / `seller` / `admin`) to allow or deny access.

## Image upload flow (seller)

1. Seller taps the image area on the **Add Item** screen.
2. Android opens the phone's photo picker (`ActivityResultContracts.GetContent()`).
3. Chosen image URI is passed to `MediaManager.get().upload(uri).unsigned("pastimes_unsigned").dispatch()`.
4. Cloudinary uploads the file and returns a **`secure_url`**.
5. The URL is stored in the app's `imageUrl` state.
6. When the seller taps **Create Listing**, the app POSTs the URL as part of the item payload.
7. The URL is saved in `items.image_url` in MySQL.
8. Buyers' apps load the image directly from Cloudinary via Coil.

## API endpoints

| Method | Path | Auth | Role | Purpose |
|---|---|---|---|---|
| GET | `/health` | – | – | Health check |
| POST | `/api/auth/register` | Bearer | any | Create Firebase user + MySQL profile |
| POST | `/api/auth/login` | Bearer | any | Verify token, return profile |
| POST | `/api/auth/google` | Bearer | any | Google SSO — check / suggest registration |
| GET | `/api/auth/me` | Bearer | any | Current user + settings |
| GET | `/api/categories` | – | – | List categories |
| GET | `/api/items` | – | – | Browse items with filters |
| GET | `/api/items/:id` | – | – | Item detail |
| GET | `/api/items/mine` | Bearer | seller | Seller's own items with status filter |
| POST | `/api/items` | Bearer | seller | Create listing |
| PUT | `/api/items/:id` | Bearer | seller | Update listing |
| DELETE | `/api/items/:id` | Bearer | seller | Soft-delete listing |
| GET | `/api/cart` | Bearer | buyer | Cart with items + totals |
| POST | `/api/cart/items` | Bearer | buyer | Add item to cart |
| DELETE | `/api/cart/items/:itemId` | Bearer | buyer | Remove item from cart |
| DELETE | `/api/cart` | Bearer | buyer | Clear cart |
| GET | `/api/addresses` | Bearer | buyer | List saved addresses |
| POST | `/api/addresses` | Bearer | buyer | Create address |
| DELETE | `/api/addresses/:id` | Bearer | buyer | Delete address |
| POST | `/api/orders` | Bearer | buyer | Checkout (transactional) |
| GET | `/api/orders` | Bearer | buyer | Order history |
| GET | `/api/orders/:id` | Bearer | buyer | Order detail |
| GET | `/api/settings` | Bearer | any | Read preferences |
| PUT | `/api/settings` | Bearer | any | Update preferences |
| GET | `/api/admin/users` | Bearer | admin | List all users |
| GET | `/api/admin/users/:id` | Bearer | admin | User detail card |
| GET | `/api/admin/users/:id/transactions` | Bearer | admin | Buyer transactions |
| GET | `/api/admin/users/:id/earnings` | Bearer | admin | Seller earnings |
| POST | `/api/admin/users/:id/reset-password` | Bearer | admin | Send reset email |
| POST | `/api/admin/users/:id/toggle-active` | Bearer | admin | Deactivate / reactivate |
| GET | `/api/admin/audit-logs` | Bearer | admin | Admin action log |

## Database schema (summary)

10 tables, InnoDB, utf8mb4:

- **`users`** — profile + role + `firebase_uid` (the link to Firebase Auth)
- **`user_settings`** — theme, notifications, language (1:1 with users)
- **`user_addresses`** — buyer delivery addresses
- **`categories`** — item categories
- **`items`** — listings with `image_url` (Cloudinary) and status enum
- **`carts`** — one per buyer
- **`cart_items`** — items currently in the cart
- **`orders`** — order header with shipping address snapshot
- **`order_items`** — line items with `seller_id` and `price_at_purchase`
- **`audit_logs`** — admin actions (password resets, deactivations)

Full DDL in [schema.sql](schema.sql).

## Checkout transaction

When a buyer taps **Place Order**, the API runs a single MySQL transaction:

1. Resolve shipping snapshot from `address_id` (or take the request body directly).
2. Lock the buyer's `cart_items` rows `FOR UPDATE`.
3. Verify every item is still `available` — if any was sold meanwhile, abort and return `409`.
4. Insert into `orders` with the shipping snapshot.
5. For each item: insert into `order_items`, then `UPDATE items SET status='sold'`.
6. Delete all rows from `cart_items`.
7. Commit.

If any step fails, the whole transaction rolls back — no half-finished orders.

## Environment variables

### Local (`api/.env`)
- `PORT`, `NODE_ENV`
- `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`, `DB_NAME`, `DB_SSL_CA` (path to cert file)
- `FIREBASE_SERVICE_ACCOUNT` (one-line JSON)
- `FIREBASE_STORAGE_BUCKET`, `FIREBASE_WEB_API_KEY`

### Render
- Same as local, except:
  - `DB_SSL_CA` → replaced by `DB_SSL_CA_CONTENT` (raw PEM text)
  - No `PORT` (Render injects it)

## CI/CD

`.github/workflows/ci.yml` runs on every push to `main`:

1. **API tests job** — `npm ci && npm test` (Jest with mocked DB + Firebase)
2. **Android build job** — decodes `google-services.json` from a GitHub secret, then `./gradlew assembleDebug` and uploads `app-debug.apk` as a workflow artifact.

Render auto-deploys the API on every push to `main`.

## Repository layout

pastimes_xisd_android/
├── api/ Node.js + Express backend
│ ├── src/
│ │ ├── config/ db.js, firebase.js
│ │ ├── middleware/ auth.js (verify token), role.js (role guard)
│ │ ├── routes/ auth, categories, items, cart, orders,
│ │ │ addresses, settings, admin
│ │ └── server.js
│ ├── tests/ Jest test suite (7 tests)
│ └── package.json
├── android/ Kotlin Android app
│ └── app/src/main/java/com/pastimes/app/
│ ├── data/
│ │ ├── api/ ApiClient, ApiService, AuthInterceptor
│ │ ├── model/ Gson DTOs
│ │ └── repository/ AuthRepository
│ ├── ui/
│ │ ├── auth/ Login, Register, AuthViewModel
│ │ ├── buyer/ Home, ItemDetail, Cart, Checkout, Orders
│ │ ├── seller/ Dashboard, Listings, AddEditItem
│ │ ├── admin/ UsersList, UserDetail
│ │ ├── settings/ SettingsScreen
│ │ └── theme/ Material 3 theme
│ ├── util/ Constants (BASE_URL)
│ └── MainActivity.kt Root navigation + session check
├── docs/ Architecture, user guide, schema.sql
├── .github/workflows/ ci.yml
└── README.md

