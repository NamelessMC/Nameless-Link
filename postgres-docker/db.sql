CREATE TABLE connections (
  id serial PRIMARY KEY,
  guild_id BIGINT UNIQUE NOT NULL,
  api_url TEXT NOT NULL,
  last_use BIGINT NOT NULL
);
