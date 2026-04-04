-- Moneris Go Portal credentials per cab/merchant terminal
CREATE TABLE `moneris_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cab_number` varchar(20) NOT NULL,
  `shift` varchar(10) NOT NULL DEFAULT 'BOTH',
  `merchant_number` varchar(100) NOT NULL,
  `moneris_store_id` varchar(50) NOT NULL,
  `moneris_api_token` varchar(100) NOT NULL,
  `moneris_environment` varchar(10) NOT NULL DEFAULT 'PROD',
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `created_by` varchar(100) DEFAULT NULL,
  `updated_by` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_moneris_cab_shift_merchant` (`cab_number`, `shift`, `merchant_number`),
  INDEX `idx_moneris_cab` (`cab_number`),
  INDEX `idx_moneris_store` (`moneris_store_id`),
  INDEX `idx_moneris_merchant` (`merchant_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
