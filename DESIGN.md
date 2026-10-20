# DESIGN

## 1. How do login and the two roles work?

A customer signs up with name, email and password. The server always gives a new account the role `customer`. The role is never taken from the request, so nobody can sign up as the studio. The studio account comes from the seed data and is created at startup.

Passwords are stored as BCrypt hashes. On login, the server checks the password and returns a signed JWT that contains the user id, email and role, valid for 24 hours. The frontend sends it as `Authorization: Bearer <token>` on every request.

A filter (`JwtAuthFilter`) reads the token on each request. Every `/api/**` URL except signup and login needs a valid token, otherwise the server answers 401. Studio-only and customer-only endpoints use `@PreAuthorize`, so the wrong role gets 403. Customers look up records with the order, booking or request id **and** their own user id together, so someone else's record simply "does not exist" and returns 404. The user id always comes from the token, never from the request body.

## 2. Which database did you choose, and why?

MySQL, running locally. The data is relational (users, orders, bookings, requests all point to each other), and I needed foreign keys, unique constraints and transactions to stop two people from taking the same painting or date. I also already knew MySQL well enough to explain how it behaves. I did not use a cloud-hosted database, which the brief said was preferred but not evaluated.

## 3. How do you stop two people from taking the same painting or date?

**Painting:** buying runs one database command, `UPDATE paintings SET status='sold' WHERE id=? AND status='available'`, and checks how many rows changed. Only the first buyer changes a row. A second buyer changes 0 rows and gets "Sorry, this painting was just sold". The update and the creation of the order run in one transaction, so if saving the order fails, the painting goes back to available. Cancelling an order uses the same conditional-update approach to free the painting.

**Wall date:** the bookings table has an `active_date` column with a UNIQUE constraint. It holds the booking date while the booking is `requested` or `confirmed`, and is set to NULL when the booking is cancelled or declined, which frees the date. If two people submit the same date at the same moment, the database rejects the second one and the server answers "Sorry, this date was just taken". The server also checks first, so the usual case gets the friendly message without an error.

The other status changes (confirm, decline, accept, cancel, and editing a pottery request) are also single conditional updates such as `WHERE status='requested'`, so two actions at once cannot both succeed.

## 4. What did you finish, what did you leave out, and what would you do next?

**Finished:** both roles with the 401, 403 and 404 rules enforced on the server; paintings (add, edit while available, buy once, cancel); wall bookings with the date window, size limits, price calculation, one booking per date, studio confirm or decline, and the 3-day cancel rule; pottery requests with seed-data options, limits, studio accept or decline, and edit or cancel only while requested; a simple frontend where every data screen has loading, empty and error states and shows clear messages when something is refused.

**Left out:** automated tests (I tested every rule manually with Postman and in the browser); a cloud database and deployment; pagination; showing already-taken dates in the booking form (the server refuses them with a clear message instead). The out-of-scope items from the brief (payments, image uploads, password reset, email verification, studio sign-up) were not built.

**Known limitations:** the login token is kept in the browser's localStorage, and tokens cannot be revoked before they expire. The booking form's price preview uses a rate of 150 written in the frontend, but the server always calculates the real price.

**Next:** write automated tests for the double-booking and status rules, move the token to an httpOnly cookie, use database migrations instead of `ddl-auto=update`, add pagination, mark taken dates in the booking form, and add login rate limiting.