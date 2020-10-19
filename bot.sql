SET AUTOCOMMIT=0;
START TRANSACTION;
CREATE DATABASE IF NOT EXISTS `bot` DEFAULT CHARACTER SET latin1 COLLATE latin1_swedish_ci;
USE `bot`;

CREATE TABLE `guilds` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `guild_id` bigint(18) DEFAULT NULL,
  `api_url` varchar(128) DEFAULT NULL,
  `owner_id` bigint(18) DEFAULT NULL,
  PRIMARY KEY (`id`), UNIQUE (ID),
  UNIQUE (`guild_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `pending_verifications` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `discord_id` bigint(18) NOT NULL,
  `username` varchar(256) NOT NULL,
  `guild_id` bigint(18) NOT NULL,
  `role` bigint(18) DEFAULT NULL,
  `site` varchar(256) NOT NULL,
  PRIMARY KEY (`id`), UNIQUE (`id`),
  UNIQUE(`discord_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `user_languages` (
  `id` UNSIGNED int(11) NOT NULL AUTO_INCREMENT,
  `discord_id` bigint(18) NOT NULL,
  `language` varchar(256) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE(`discord_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

COMMIT;
