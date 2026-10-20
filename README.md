# Art Studio

A small full-stack website for an art studio. Customers can buy paintings, book a wall painting, and request custom pottery. The studio reviews and manages everything.

## Features

- **Paintings:** every painting is one of a kind, so it can be bought only once. Cancelling an order makes the painting available again. The studio adds and edits paintings (only while available).
- **Wall painting bookings:** pick a date 7 to 90 days away, a wall size of 10 to 500 sq ft, and an address. Each date can be booked only once. The price is wall size x Rs 150. The studio confirms or declines. The customer can cancel until 3 days before the date.
- **Custom pottery:** choose an item and glaze, a quantity of 1 to 12, and a needed-by date at least 14 days away. The studio accepts or declines. The customer can edit or cancel only while the request is still `requested`.
- **Two roles:** customer and studio. The server checks the role on every request (401 if not logged in, 403 for the wrong role, 404 for someone else's record).
- Every screen that loads data shows loading, empty and error states.

## Tech stack

- Backend: Java 21, Spring Boot 4.1.1, Spring Security, Spring Data JPA
- Database: MySQL
- Authentication: JWT (JSON Web Token) with BCrypt password hashing
- Frontend: plain HTML, CSS and JavaScript, served by Spring Boot

## Prerequisites

- JDK 21 or newer
- MySQL 8 or newer, running locally
- Git

Maven does not need to be installed. The project includes the Maven wrapper (`mvnw`).

## Setup

Run all commands from the project root folder (the folder that contains `pom.xml`).

1. **Clone the repository**

```
git clone https://github.com/harshitha-jally/artstudio.git
cd artstudio
```

2. **Create the database** (in MySQL Workbench or the `mysql` command line)

```sql
CREATE DATABASE artstudio;
```

3. **Create your `.env` file** by copying the example, then edit the values

   Windows:
```
copy .env.example .env
```
macOS / Linux:
```
cp .env.example .env
```

Open `.env` and set your MySQL username and password. Set `JWT_SECRET` to any random text of **at least 32 characters**.

4. **Start the app**

   Windows:
```
mvnw.cmd spring-boot:run
```
macOS / Linux:
```
./mvnw spring-boot:run
```

5. **Open the site** at http://localhost:8080

On the first start, the tables are created automatically and the seed data (12 paintings, 5 pottery items, 6 glazes and the studio account) is loaded from `src/main/resources/seed.json`. The seeding only runs when the tables are empty, so restarting never creates duplicates.

To reset everything, drop and re-create the database, then start the app again.

## Environment variables

| Variable | Meaning | Example |
|---|---|---|
| `DB_URL` | MySQL connection URL | `jdbc:mysql://localhost:3306/artstudio` |
| `DB_USERNAME` | MySQL username | `root` |
| `DB_PASSWORD` | MySQL password | `your_mysql_password` |
| `JWT_SECRET` | Secret used to sign login tokens (32+ characters) | `replace_with_a_long_random_string` |

## Accounts

- **Studio:** `studio@artstudio.in` / `Studio@123` (from the seed data)
- **Customers:** use "Create an account" on the login page. Studio sign-up is not available.

## Project structure

```
src/main/java/com/artstudio/artstudio/
  config/       security configuration and the seed loader
  controller/   REST endpoints
  service/      business rules
  repository/   database access
  entity/       database tables
  dto/          request and response objects
  security/     JWT creation and checking
  exception/    error handling
src/main/resources/
  seed.json     seed data
  static/       the frontend (index.html, app.js, api.js, style.css, images/)
```

## API overview

| Area | Endpoints |
|---|---|
| Auth | `POST /api/auth/signup`, `POST /api/auth/login` |
| Paintings | `GET /api/paintings`, `POST /api/paintings` (studio), `PUT /api/paintings/{id}` (studio) |
| Orders | `POST /api/orders`, `GET /api/orders`, `POST /api/orders/{id}/cancel` |
| Wall bookings | `POST /api/bookings`, `GET /api/bookings`, `POST /api/bookings/{id}/cancel`, `.../confirm` and `.../decline` (studio) |
| Pottery | `GET /api/pottery/options`, `POST /api/pottery`, `GET /api/pottery`, `PUT /api/pottery/{id}`, `POST /api/pottery/{id}/cancel`, `.../accept` and `.../decline` (studio) |

All endpoints except signup and login need the header `Authorization: Bearer <token>`.

## Design notes

See [DESIGN.md](DESIGN.md) for the design decisions, what was finished, and what was left out.