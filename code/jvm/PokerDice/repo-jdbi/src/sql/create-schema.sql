create schema if not exists dbo;

-- Users and Authentication
create table dbo.USERS (
    id serial primary key,
    username varchar(255) unique not null,
    email varchar(255) unique not null,
    password_hash varchar(255) not null,
    balance integer not null default 100 -- Starting balance for players
);

create table dbo.TOKEN (
    token varchar(255) primary key,
    user_id integer not null references dbo.USERS(id),
    created_at bigint not null,
    last_used_at bigint not null
);

-- Lobbies
create table dbo.LOBBY (
    id serial primary key,
    name varchar(255) unique not null,
    description varchar(255),
    host_id integer not null references dbo.USERS(id),
    min_players integer not null check (min_players >= 2),
    max_players integer not null check (max_players >= min_players)
);

create table dbo.LOBBY_PLAYER (
    lobby_id integer not null references dbo.LOBBY(id) on delete cascade,
    user_id integer not null references dbo.USERS(id),
    primary key (lobby_id, user_id)
);

-- Game State
create type dbo.GAME_STATE as enum ('WAITING', 'RUNNING', 'TERMINATED', 'FINISHED');

create table dbo.GAME (
    id serial primary key,
    lobby_id integer unique not null references dbo.LOBBY(id),
    state dbo.GAME_STATE not null,
    current_round integer not null default 1,
    total_rounds integer not null,
    created_at bigint not null,
    ended_at bigint
);

create table dbo.GAME_PLAYER (
    game_id integer not null references dbo.GAME(id) on delete cascade,
    user_id integer not null references dbo.USERS(id),
    primary key (game_id, user_id)
);

create table dbo.ROUND (
    game_id integer not null references dbo.GAME(id) on delete cascade,
    round_number integer not null,
    turn_of_player integer references dbo.USERS(id),
    pot integer not null,
    primary key (game_id, round_number)
);

create table dbo.PLAYER_HAND (
    game_id integer not null,
    round_number integer not null,
    user_id integer not null,
    dice_values char(1)[] not null, -- Array of characters: A, K, Q, J, T, 9
    rolls_left integer not null default 2,
    primary key (game_id, round_number, user_id),
    foreign key (game_id, round_number) references dbo.ROUND(game_id, round_number),
    foreign key (user_id) references dbo.USERS(id)
);

-- Performance indexes
create index idx_lobby_player_user_id on dbo.LOBBY_PLAYER(user_id);
create index idx_game_player_user_id on dbo.GAME_PLAYER(user_id);
create index idx_game_lobby_id on dbo.GAME(lobby_id);
