## LedgerMate backend

Spring Boot backend for a Splitwise-like expense sharing app for students.

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



