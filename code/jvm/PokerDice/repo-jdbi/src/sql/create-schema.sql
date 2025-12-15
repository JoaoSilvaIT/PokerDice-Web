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
    max_players integer not null check (max_players >= min_players),
    number_of_rounds integer not null default 3 check (number_of_rounds >= 1)
);

create table dbo.LOBBY_USER (
   lobby_id INT NOT NULL,
   user_id INT NOT NULL,
   PRIMARY KEY (lobby_id, user_id),
   FOREIGN KEY (lobby_id) REFERENCES dbo.LOBBY(id) ON DELETE CASCADE,
   FOREIGN KEY (user_id) REFERENCES dbo.USERS(id) ON DELETE CASCADE
);

-- Game State
create type dbo.GAME_STATE as enum ('WAITING', 'RUNNING', 'TERMINATED', 'FINISHED');

create table dbo.GAME (
    id serial primary key,
    lobby_id integer references dbo.LOBBY(id) ON DELETE SET NULL,
    state dbo.GAME_STATE not null,
    current_round_number integer,
    total_rounds integer not null,
    started_at bigint not null,
    ended_at bigint
);

create table dbo.ROUND (
    round_number integer not null,
    game_id integer not null references dbo.GAME(id) on delete cascade,
    first_player_idx integer not null,
    turn_of_player integer references dbo.USERS(id),
    ante integer not null,
    pot integer not null,
    primary key (game_id, round_number)
);

create table dbo.TURN (
    game_id integer not null,
    round_number integer not null,
    user_id integer not null references dbo.USERS(id) on delete cascade,
    dice_values text[] not null,
    rolls_left integer not null default 3,
    primary key (game_id, round_number, user_id),
    foreign key (game_id, round_number) references dbo.ROUND(game_id, round_number) on delete cascade
);

create table dbo.ROUND_WINNER (
    game_id integer not null,
    round_number integer not null,
    user_id integer not null references dbo.USERS(id) on delete cascade,
    winnings_amount integer not null default 0,
    primary key (game_id, round_number, user_id),
    foreign key (game_id, round_number) references dbo.ROUND(game_id, round_number) on delete cascade
);

create table dbo.INVITE(
    id serial primary key,
    inviterId integer references dbo.USERS(id),
    inviteValidationInfo varchar(255) unique not null,
    state varchar(20) not null CHECK (state IN ('pending', 'used', 'expired')),
    createdAt bigint not null
);