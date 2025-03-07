CREATE TABLE IF NOT EXISTS `buylists` (
	`buylist_id` INT UNSIGNED,
	`item_id` INT UNSIGNED,
	`count` BIGINT UNSIGNED NOT NULL DEFAULT 0,
	`next_restock_time` BIGINT UNSIGNED NOT NULL DEFAULT 0,
	PRIMARY KEY (`buylist_id`, `item_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
