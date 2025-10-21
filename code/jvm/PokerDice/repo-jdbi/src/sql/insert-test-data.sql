-- sql
-- Test data for manual API testing (users, tokens, lobbies, lobby membership, one running game + round + hands)
-- Truncate existing data (order respects FKs) and insert deterministic IDs so tests can reference them easily.

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

-- Insert users (explicit ids for predictable tests)
INSERT INTO dbo.USERS (id, username, email, password_hash, balance)
VALUES (1, 'Paul Atreides', 'paul@atreides.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye1hzjJHGRarQk6ZKMX8sFgYjKhKdKxQi', 1000),
       (2, 'Duncan Idaho', 'duncan@idaho.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye1hzjJHGRarQk6ZKMX8sFgYjKhKdKxQi', 800),
       (3, 'Lady Jessica', 'jessica@benegesserit.org', '$2a$10$N9qo8uLOickgx2ZMRZoMye1hzjJHGRarQk6ZKMX8sFgYjKhKdKxQi',
        1200);

-- Insert tokens for authentication tests (replace token values as needed)
INSERT INTO dbo.TOKEN (token, user_id, created_at, last_used_at)
VALUES ('token-user-1-valid', 1, (extract(epoch from now())::bigint - 3600), (extract(epoch from now())::bigint - 60)),
       ('token-user-2-valid', 2, (extract(epoch from now())::bigint - 3600), (extract(epoch from now())::bigint - 120));

-- Insert lobbies
INSERT INTO dbo.LOBBY (id, name, description, host_id, min_players, max_players)
VALUES (1, 'Dune Warriors', 'Battle for Arrakis', 1, 2, 4),
       (2, 'Spice Traders', 'Economic warfare', 2, 2, 6);

-- Lobby memberships
INSERT INTO dbo.LOBBY_USER (lobby_id, user_id)
VALUES (1, 1),
       (1, 2),
       (2, 2),
       (2, 3);

-- Create a running game for lobby 1 (game id fixed for tests)
INSERT INTO dbo.GAME (id, lobby_id, state, current_round_number, total_rounds, started_at, ended_at)
VALUES (1, 1, 'RUNNING', 1, 5, extract(epoch from now())::bigint - 300, NULL);

-- Create the current round for game 1
INSERT INTO dbo.ROUND (round_number, game_id, first_player_idx, turn_of_player, ante, pot)
VALUES (1, 1, 0, 1, 10, 20);

-- Insert player turns for round 1 (dice values use textual faces for easy inspection)
INSERT INTO dbo.TURN (game_id, round_number, user_id, dice_values, rolls_left)
VALUES (1, 1, 1, ARRAY ['ONE','TWO','THREE','FOUR','FIVE'], 2),
       (1, 1, 2, ARRAY ['SIX','SIX','TWO','TWO','THREE'], 2);

-- Optional: create another (waiting) game for lobby 2 to test creation/start flows
INSERT INTO dbo.GAME (id, lobby_id, state, current_round_number, total_rounds, started_at, ended_at)
VALUES (2, 2, 'WAITING', NULL, 3, extract(epoch from now())::bigint, NULL);

-- Reset sequences to avoid conflicts when inserting new rows without ids
-- NOTE: sequence names depend on DB; adjust if necessary.
ALTER SEQUENCE dbo.users_id_seq RESTART WITH 4;
ALTER SEQUENCE dbo.lobby_id_seq RESTART WITH 3;
ALTER SEQUENCE dbo.game_id_seq RESTART WITH 3;

-- Helpful messages (for manual runs in psql)
SELECT 'Test data inserted successfully!' AS message;
SELECT 'Users created: 3' AS users;
SELECT 'Lobbies created: 2' AS lobbies;
SELECT 'Games created: 2' AS games;
