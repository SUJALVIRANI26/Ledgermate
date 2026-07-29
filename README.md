## LedgerMate backend

Spring Boot backend for a Splitwise-like expense sharing app for students.

### Prerequisites

- **Java 17 JDK** installed (not just a JRE).
  - On Windows you can install e.g. Temurin or Oracle JDK 17 and ensure `java -version` shows version 17 and mentions a JDK.
- **Maven wrapper** is already included, so you do **not** need Maven installed separately.

### How to run

From the project root (`Ledgermate` folder):

```bash
.\mvnw.cmd spring-boot:run
```

The application will start on **`http://localhost:8080`** and use an **in‑memory H2 database**, so no external database setup is required.

You can access the H2 console at:

- `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:mem:ledgermate`
  - Username: `sa`
  - Password: *(leave empty)*

### API overview

- **Register user (public)**
  - `POST /api/auth/register`
  - Body:
    ```json
    {
      "name": "Alice",
      "email": "alice@example.com",
      "password": "password123"
    }
    ```

- **Authenticated endpoints (require HTTP Basic with registered email/password)**
  - `POST /api/groups` – create group with member emails.
  - `POST /api/groups/{groupId}/members` – add member to existing group.
  - `POST /api/expenses` – create expense for a group.
  - `POST /api/expenses/settle` – record a settlement between two users.
  - `GET /api/expenses/group/{groupId}/balances` – get net balance per member.

### Notes

- Data is stored only in memory (H2) and resets when the app restarts. For a persistent DB we can later switch to MySQL/PostgreSQL.
- If the build fails with **"No compiler is provided in this environment. Perhaps you are running on a JRE rather than a JDK?"**, install a **Java 17 JDK** and try again.

