-- Insert test data for development/testing

-- Insert test users
INSERT INTO dbo.USERS (username, email, password_hash, balance) VALUES
    ('Paul Atreides', 'paul@atreides.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye1hzjJHGRarQk6ZKMX8sFgYjKhKdKxQi', 1000),  -- password: muadib
    ('Duncan Idaho', 'duncan@idaho.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye1hzjJHGRarQk6ZKMX8sFgYjKhKdKxQi', 800),    -- password: swordmaster
    ('Lady Jessica', 'jessica@benegesserit.org', '$2a$10$N9qo8uLOickgx2ZMRZoMye1hzjJHGRarQk6ZKMX8sFgYjKhKdKxQi', 1200); -- password: test123

-- Insert test lobbies
INSERT INTO dbo.LOBBY (name, description, host_id, min_players, max_players) VALUES
    ('Dune Warriors', 'Battle for Arrakis', 1, 2, 4),
    ('Spice Traders', 'Economic warfare', 2, 2, 6);

-- Add players to lobbies
INSERT INTO dbo.LOBBY_PLAYER (lobby_id, user_id) VALUES
    (1, 1),  -- Paul in Dune Warriors
    (1, 2),  -- Duncan in Dune Warriors
    (2, 2),  -- Duncan in Spice Traders
    (2, 3);  -- Jessica in Spice Traders

SELECT 'Test data inserted successfully!' as message;
SELECT 'Users created: 3' as users;
SELECT 'Lobbies created: 2' as lobbies;
-- Clear all data from the database while preserving schema
-- Useful for resetting to a clean state during testing

-- Clear game-related data (in correct order due to foreign keys)
TRUNCATE TABLE dbo.PLAYER_HAND CASCADE;
TRUNCATE TABLE dbo.ROUND CASCADE;
TRUNCATE TABLE dbo.GAME_PLAYER CASCADE;
TRUNCATE TABLE dbo.GAME CASCADE;

-- Clear lobby data
TRUNCATE TABLE dbo.LOBBY_PLAYER CASCADE;
TRUNCATE TABLE dbo.LOBBY CASCADE;

-- Clear authentication data
TRUNCATE TABLE dbo.TOKEN CASCADE;

-- Clear users (will cascade to everything due to foreign keys)
TRUNCATE TABLE dbo.USERS RESTART IDENTITY CASCADE;

-- Reset sequences
ALTER SEQUENCE dbo.users_id_seq RESTART WITH 1;
ALTER SEQUENCE dbo.lobby_id_seq RESTART WITH 1;
ALTER SEQUENCE dbo.game_id_seq RESTART WITH 1;

SELECT 'Database cleared successfully!' as message;

