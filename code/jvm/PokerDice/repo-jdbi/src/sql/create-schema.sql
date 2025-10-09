create schema dbo;

create table dbo.USER (
    id serial primary key,
    username varchar(255) unique not null,
    password varchar(255) not null
);

create table dbo.TOKEN (
    tokenValidation varchar(255) primary key ,
    createdAt bigint not null,
    lastUsedAt bigint not null,
    userId integer,
    foreign key (userId) references dbo.USER(id)
);

create table dbo.LOBBY (
    id serial primary key,
    name varchar(255) unique not null,
    description varchar(255) not null,
    hostId integer,
    foreign key (hostId) references dbo.USER(id)
)

create table dbo.GAME (
    id serial primary key,
    startedAt bigint not null,
    endedAt bigint,
    numberOfRounds integer not null,
    state varchar(255) not null,
    lobbyId integer,
    foreign key (lobbyId) references dbo.LOBBY(id)
)