-- clear-data.sql
-- Truncate all application tables in schema dbo and restart serial sequences.
-- This keeps the schema (tables, types) intact but removes all rows and resets IDs.
-- Run with psql: psql -h HOST -p PORT -U USER -d DBNAME -f "path\to\clear-data.sql"

BEGIN;
TRUNCATE TABLE
    dbo.player_hand,
    dbo.ROUND,
    dbo.GAME,
    dbo.LOBBY_USER,
    dbo.LOBBY,
    dbo.TOKEN,
    dbo.USERS
RESTART IDENTITY CASCADE;
COMMIT;

-- Notes:
-- 1) RESTART IDENTITY resets all sequences owned by the truncated tables.
-- 2) CASCADE ensures dependent tables are also truncated if necessary.
-- 3) If you want to remove and recreate the whole schema (types included), use DROP SCHEMA ... CASCADE
--    instead, but that will delete custom types like dbo.GAME_STATE.

