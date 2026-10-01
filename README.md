# Pastimes

**A second-hand online clothing marketplace — Android app + Node.js API + MySQL.**

![CI](https://github.com/Nasstyfox/pastimes_xisd_android/actions/workflows/ci.yml/badge.svg)

---

## Quick links

| Resource | Link |
|---|---|
| **Live API** | https://pastimes-api.onrender.com |
| **Health check** | https://pastimes-api.onrender.com/health |
| **Android app** | `android/` (Kotlin + Jetpack Compose) |
| **API source** | `api/` (Node.js + Express) |
| **User documentation** | [docs/USER_GUIDE.md](docs/USER_GUIDE.md) |
| **Architecture** | [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) |
| **Database schema** | [docs/schema.sql](docs/schema.sql) |
| **XISD6329 Task 2** | IIE Module Manual (2026) — Task 2 Requirements |

---

## What it does

Pastimes is a marketplace where users can buy and sell second-hand clothing. Three roles:

### Buyers
- Browse items with search + category + price filters
- Add items to cart
- Save delivery addresses
- Check out (simulated payment) and view order history
- Update theme, notifications, and language preferences

### Sellers
- Dashboard with live stats: items listed / available / sold / removed and total earnings
- View own listings with filter tabs (All / Available / Sold / Removed)
- Add, edit, and soft-delete items (images uploaded from the phone)
- Settings page

### Admins
- View all users with role and status filters
- Open a user's detail card showing summary stats
- **Buyers:** order count, total spent, full transaction history
- **Sellers:** items listed, available, sold, total earnings, sales history
- **Reset password** — sends a Firebase reset email
- **Deactivate / reactivate** user account
- All admin actions logged in `audit_logs`

---

## Stack

| Layer | Technology |
|---|---|
| **Android app** | Kotlin, Jetpack Compose, Material 3, Retrofit, Coil, Navigation Compose |
| **Authentication** | Firebase Authentication (email/password + Google SSO) |
| **Image uploads** | Cloudinary (unsigned preset) |
| **API** | Node.js 20, Express 5, mysql2, helmet, morgan, JWT via Firebase Admin SDK |
| **Database** | MySQL 8 (hosted on Aiven) |
| **API hosting** | Render (free tier) |
| **CI** | GitHub Actions — Jest tests + Android APK build |
| **Version control** | GitHub + Azure DevOps backlog |

## Architecture

┌────────────────────┐ ┌────────────────────┐
│ Android app │ HTTPS │ Node.js + Express │
│ (Kotlin/Compose) │ ──────► │ API (Render) │
└────────────────────┘ └─────────┬──────────┘
│ │
│ Firebase SDK ├──► Firebase Auth (SSO + passwords)
│ Cloudinary SDK ├──► Cloudinary (item images)
▼ ▼
┌────────────────────┐ ┌────────────────────┐
│ Firebase Auth │ │ MySQL 8 (Aiven) │
│ (online auth) │ │ Pastimes DB │
└────────────────────┘ └────────────────────┘


See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for details.

---

## Running locally

### API

```bash
cd api
cp .env.example .env   # fill in Aiven + Firebase + Cloudinary values
npm install
npm run dev            # http://localhost:4000
npm test               # Jest test suite

Android
Open android/ in Android Studio.

Place google-services.json in android/app/ (Firebase console → Project Settings → Your apps).

In android/app/src/main/java/com/pastimes/app/util/Constants.kt, confirm the API base URL points to your Render deployment.

Run on a real Android phone (not just emulator — the Task 2 spec requires this).

The app calls the live API at https://pastimes-api.onrender.com.

pastimes_xisd_android/
├── api/                       Node.js + Express backend
│   ├── src/
│   │   ├── config/            db.js, firebase.js
│   │   ├── middleware/        auth.js, role.js
│   │   ├── routes/            auth, categories, items, cart, orders, addresses, settings, admin
│   │   └── server.js
│   ├── tests/                 Jest test suite (7 tests)
│   └── package.json
├── android/                   Kotlin Android app
│   └── app/src/main/java/com/pastimes/app/
│       ├── data/              api, model, repository
│       ├── ui/                auth, buyer, seller, admin, settings, theme
│       └── MainActivity.kt
├── docs/                      User guide, architecture, schema
├── .github/workflows/         GitHub Actions CI
└── README.md


Role	Email	            Password
Buyer	buyer1@test.com	    Test1234!
Seller	seller1@test.com	Test1234!
Admin	admin1@test.com	    Test1234!

CI badges
Every push to main runs:

API tests — 7 Jest tests against a mocked Firebase + MySQL layer

Android build — ./gradlew assembleDebug, APK available as a workflow artifact

License


Save it.

---

## 18.2 — Create `docs/` folder with 3 files

### `docs/ARCHITECTURE.md`

Path: `C:\Users\tshia\AndroidStudioProjects\pastimes_xisd\docs\ARCHITECTURE.md`

```markdown
# Pastimes — Architecture

## High-level flow

Android App (Kotlin + Compose)
│
├── Firebase SDK ───► Firebase Auth
│ (email/password + Google SSO)
│ ID tokens issued here
│
├── Cloudinary SDK ─► Cloudinary
│ (item image uploads, unsigned preset)
│
└── Retrofit + OkHttp ──► Node.js API (Render)
(Bearer token) │
├── Firebase Admin SDK
│ verifies ID tokens on every request
│
└── mysql2 pool ──► MySQL (Aiven)


Pastimes database


## Authentication flow

1. User submits email + password on the **Login screen**.
2. Android calls `FirebaseAuth.signInWithEmailAndPassword` via the Firebase SDK.
3. Firebase returns an **ID token** (JWT, valid for 1 hour).
4. Every API request from the app sends `Authorization: Bearer <id_token>`.
5. The API verifies the token with the **Firebase Admin SDK**.
6. The API looks up the user's profile in MySQL by `firebase_uid` and attaches it to `req.user`.
7. Routes check `req.user.role` to allow/deny access.

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
| POST | `/api/orders` | Bearer | buyer | Checkout (transaction) |
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

- `users` — profile + role + firebase_uid
- `user_settings` — theme, notifications, language
- `user_addresses` — buyer delivery addresses
- `categories` — item categories
- `items` — listings with image_url, status (available/reserved/sold/removed)
- `carts` — one per buyer
- `cart_items` — items in cart
- `orders` — order header with shipping snapshot
- `order_items` — line items with seller_id and price_at_purchase
- `audit_logs` — admin actions

Full DDL in [schema.sql](schema.sql).

## CI/CD

`.github/workflows/ci.yml` runs on every push:

1. **API tests job** — `npm ci && npm test` (Jest, 7 tests with mocked DB/Firebase)
2. **Android build job** — `./gradlew assembleDebug`, uploads `app-debug.apk` as an artifact

Render auto-deploys the API on every push to `main` (webhook).