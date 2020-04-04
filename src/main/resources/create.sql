create table ConfigurationApplication (
    id SERIAL PRIMARY KEY,
    name varchar(100) UNIQUE NOT NULL,
    version bigint NOT NULL,
    createdms bigint NOT NULL,
    modifiedms bigint NOT NULL
);

create table ConfigurationHost (
    id SERIAL PRIMARY KEY,
    name varchar(100) UNIQUE NOT NULL,
    createdms bigint NOT NULL,
    modifiedms bigint NOT NULL
);

create table ConfigurationProperty (
    id SERIAL PRIMARY KEY,
    appId bigint REFERENCES ConfigurationApplication (id) NOT NULL,
    hostId bigint REFERENCES ConfigurationHost (id) NOT NULL,
    name varchar(150) NOT NULL,
    value text NOT NULL,
    version bigint NOT NULL,
    deleted boolean NOT NULL,
    createdms bigint NOT NULL,
    modifiedms bigint NOT NULL
);
