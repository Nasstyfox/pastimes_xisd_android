# Pastimes

A second-hand online clothing marketplace — Android app + Node.js API + MySQL.

## Stack

- **Android app:** Kotlin, Jetpack Compose (buyer / seller / admin)
- **API:** Node.js + Express — `api/`
- **Database:** MySQL (Aiven) — schema in `docs/schema.sql`
- **Auth:** Firebase Authentication (email/password + Google SSO)
- **Images:** Firebase Storage
- **CI:** GitHub Actions

## Repository layout

api/ Node.js + Express backend
android/ Android app (Kotlin)
docs/ Diagrams, user documentation, ERD
.github/ GitHub Actions workflows


## Running locally

### API

```bash
cd api
cp .env.example .env    
npm install
npm run dev             # http://localhost:4000


Android
Open android/ in Android Studio, place google-services.json in android/app/, and run.

Live API
Deployed at: <paste Render URL here>

Health: <Render URL>/health

Roles
Buyer — browse items, cart, checkout, orders, settings

Seller — listing dashboard, add/edit/remove items, settings

Admin — user management, reset password, transactions, earnings, settings