# Pastimes — User Guide

This guide shows how to use the Pastimes app for each role.

---

## 1. Getting the app

The app is built for Android (minimum Android 7.0).

1. Download the APK from the latest successful CI build:
   **GitHub → Actions → CI → latest run → Artifacts → pastimes-debug-apk**
2. On your phone: **Settings → Apps → Install unknown apps** → allow for your browser / file manager.
3. Open the downloaded `app-debug.apk` → **Install**.
4. Launch **Pastimes3** from your home screen.

---

## 2. First launch

You'll see the **Login** screen with the Pastimes heading and two fields (Email, Password).

Below the fields are two buttons:
- **Log In** — for existing users
- **Don't have an account? Register** — opens the registration screen

---

## 3. Registering

Tap **Register**. Fill in:

| Field | Notes |
|---|---|
| Full Name | Required |
| Email | Required — becomes your login |
| Phone | Optional |
| Password | At least 6 characters |
| **I am a** | Choose **Buyer** or **Seller** |

Tap **Register**. The app creates your account and takes you to your role's home screen.

> **Note:** Admin accounts are created by seeding the database directly (see README). You cannot self-register as an admin.

---

## 4. Buyer features

### Home (browse)
- Search bar at the top — type a keyword, tap **Go**.
- Category chips below — tap to filter by category.
- Two-column grid of item cards showing photo, title, price, size.
- Tap any card → **Item Detail**.

### Item detail
- Full-size photo, title, price, condition, size, brand, colour, seller name, description.
- **Add to Cart** button — tap once to add. A snackbar appears: *"Added to cart"*.

### Cart
Bottom nav → **Cart**.
- List of items with photo, title, size, price.
- Trash icon on each row → removes that item.
- Bottom bar shows **Total**.
- **Clear** empties the cart.
- **Checkout** proceeds to the next screen.

### Checkout
- Pick a saved address (radio button).
- Or tap **Add New Address** and fill the form.
- Tap **Place Order**.
- If any item in the cart was sold by someone else while you were browsing, checkout stops with a clear error.

### Orders
Bottom nav → **Orders**. Shows all past orders with:
- Order number and status
- Date and time
- Line items and prices
- Ship-to summary

### Settings
Bottom nav → **Settings**.
- Account card (read-only name, email, phone, role).
- **Theme** — Light / Dark / System.
- **Notifications** — on/off switch.
- **Language** — English, isiZulu, Afrikaans, isiXhosa.
- **Log Out** button at the bottom.

---

## 5. Seller features

### Dashboard
The first screen after login shows:
- Cards: **Listed**, **Available**, **Sold**, **Removed** counts.
- **Total Earnings** card showing the sum of your sold items.
- **Manage My Listings** button.

### Listings
Bottom nav → **Listings**.
- Filter tabs: **All**, **Available**, **Sold**, **Removed**.
- Each item row shows photo, title, price, status badge, edit and delete icons.
- Floating **+** button → **Add Item**.

### Adding / editing an item
1. Tap the image area → pick a photo from your phone.
2. Wait for the upload spinner (uploads to Cloudinary).
3. Select a **Category** from the dropdown.
4. Fill in **Title**, **Description**, **Price**, **Size**, **Brand**, **Colour**.
5. Pick a **Condition**: New / Like New / Good / Fair.
6. Tap **Create Listing** (or **Save Changes** when editing).

### Deleting
Tap the trash icon on any row → the item's status becomes **Removed**. It disappears from buyers' browse view but stays in your Removed filter.

> Sold items cannot be deleted (they're part of an order).

---

## 6. Admin features

Bottom nav shows **Users** and **Settings**.

### Users
- Filter chips: **All**, **Buyer**, **Seller**, **Admin**.
- Each row shows name, email, role badge, Active/Inactive badge.
- Tap a user → **User Detail card**.

### User detail card
- **Profile:** name, email, phone, role, provider, status, join date.
- **Buyers:** Purchase Summary (order count, total spent) + full Transaction History.
- **Sellers:** Sales Summary (items listed, available, sold, total earnings) + Sales History with buyer names.
- **Actions:**
  - **Reset Password (send email)** — sends a Firebase password-reset email to that user.
  - **Deactivate / Activate User** — toggles the account's active flag. Deactivated users can't use the API.

Every action is written to the `audit_logs` table.

### Settings
Same as other roles — theme, notifications, language, and logout.

---

## 7. Logout

From any role's **Settings** tab, tap **Log Out**. The app returns to the Login screen.

---

## 8. Troubleshooting

| Problem | Fix |
|---|---|
| App shows "Network error" | The API is waking up (Render free tier). Wait 30s and retry. |
| Images don't load | Check phone internet. Cloudinary URLs load from the CDN. |
| Password reset email never arrives | Check spam. Firebase sends from `noreply@<project>.firebaseapp.com`. |
| Login fails with "Incorrect email or password" | Password is case-sensitive. Re-type. |
| "No account found" | Register first — Firebase accounts and MySQL profiles must both exist. |