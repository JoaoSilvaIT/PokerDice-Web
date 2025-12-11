-- sql
-- Test data for manual API testing (users, tokens, lobbies, lobby membership, one running game + round + hands)
-- Truncate existing data (order respects FKs) and insert deterministic IDs so tests can reference them easily.

-- Enable pgcrypto extension for generating BCrypt hashes
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Clear existing data
TRUNCATE TABLE dbo.ROUND_WINNER CASCADE;
TRUNCATE TABLE dbo.TURN CASCADE;
TRUNCATE TABLE dbo.ROUND CASCADE;
TRUNCATE TABLE dbo.GAME CASCADE;
TRUNCATE TABLE dbo.LOBBY_USER CASCADE;
TRUNCATE TABLE dbo.LOBBY CASCADE;
TRUNCATE TABLE dbo.TOKEN CASCADE;
TRUNCATE TABLE dbo.INVITE CASCADE;
TRUNCATE TABLE dbo.USERS RESTART IDENTITY CASCADE;

INSERT INTO dbo.USERS (username, email, password_hash, balance)
VALUES ('Admin', 'Admin@gmail.com', crypt('password123', gen_salt('bf', 10)), 1000);
INSERT INTO dbo.USERS (username, email, password_hash, balance)
VALUES ('User2', 'user2@example.com', crypt('1234', gen_salt('bf', 10)), 1000);
INSERT INTO dbo.USERS (username, email, password_hash, balance)
VALUES ('User1', 'user1@example.com', crypt('1234', gen_salt('bf', 10)), 1000)
