# PokerDice

DAW Project 2024/2025 - LEIC51D - Group 06

- Bernardo Jaco \- 51690
- João Silva \- 51682
- Pedro Monteiro \- 51457

## How to Run

This project uses **Docker Compose** to orchestrate the Database (PostgreSQL), Backend (JVM/Spring Boot), and Frontend (React/Nginx).

### Prerequisites
- Docker Engine & Docker Compose
- Java 21 (to run Gradle wrappers)

### Quick Start (Production Mode)

1. **Navigate to the Gradle project root:**
   ```bash
   cd code/jvm/PokerDice
   ```

2. **Build Docker Images:**
   This command compiles the Backend (Kotlin) and Frontend (TypeScript/Vite), and builds the local Docker images.
   ```bash
   ./gradlew buildImageAll
   ```

3. **Start the Environment:**
   This command launches all containers (Postgres, API, Nginx).
   ```bash
   ./gradlew allUp
   ```

4. **Access the Application:**
   Open your browser at:
   [http://localhost:5173](http://localhost:5173)
   
   > **Note:** Nginx acts as a reverse proxy. The React app is served at `/`, and API requests are proxied to `/api`.

### Development Mode (Local Frontend)
If you wish to run the Frontend locally (npm run dev) while keeping the Backend in Docker:

1. **Build Backend & DB Images:**
   ```bash
   cd code/jvm/PokerDice
   ./gradlew buildImagePostgres
   ./gradlew buildImageJvm
   ```

2. **Start Backend Services:**
   Start only the database and backend services (ignoring Nginx).
   ```bash
   docker compose up pokerdice-postgres pokerdice-jvm
   ```

3. **Run the Frontend:**
   Open a new terminal:
   ```bash
   cd code/ts
   npm install
   npm run dev
   ```

### Stopping the System
To stop the containers:
```bash
cd code/jvm/PokerDice
./gradlew allDown
```