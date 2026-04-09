-- V054: Seed 2024 Canadian tax brackets, credits, and CPP/EI rates

-- CPP/EI Rates for 2024
INSERT INTO cpp_ei_rate (tax_year, cpp_employee_rate, cpp_max_pensionable, cpp_basic_exemption, ei_employee_rate, ei_max_insurable, created_at)
VALUES (2024, 0.0595, 68500.00, 3500.00, 0.0166, 63200.00, NOW());

-- Federal Tax Brackets 2024
INSERT INTO income_tax_bracket (tax_year, jurisdiction, bracket_order, min_income, max_income, rate, created_at) VALUES
(2024, 'FEDERAL', 1, 0.00, 55867.00, 0.1500, NOW()),
(2024, 'FEDERAL', 2, 55867.00, 111733.00, 0.2050, NOW()),
(2024, 'FEDERAL', 3, 111733.00, 154906.00, 0.2600, NOW()),
(2024, 'FEDERAL', 4, 154906.00, 220000.00, 0.2900, NOW()),
(2024, 'FEDERAL', 5, 220000.00, NULL, 0.3300, NOW());

-- Ontario Tax Brackets 2024
INSERT INTO income_tax_bracket (tax_year, jurisdiction, bracket_order, min_income, max_income, rate, created_at) VALUES
(2024, 'ON', 1, 0.00, 51446.00, 0.0505, NOW()),
(2024, 'ON', 2, 51446.00, 102894.00, 0.0915, NOW()),
(2024, 'ON', 3, 102894.00, 150000.00, 0.1116, NOW()),
(2024, 'ON', 4, 150000.00, 220000.00, 0.1216, NOW()),
(2024, 'ON', 5, 220000.00, NULL, 0.1316, NOW());

-- Quebec Tax Brackets 2024
INSERT INTO income_tax_bracket (tax_year, jurisdiction, bracket_order, min_income, max_income, rate, created_at) VALUES
(2024, 'QC', 1, 0.00, 49275.00, 0.1500, NOW()),
(2024, 'QC', 2, 49275.00, 98540.00, 0.2000, NOW()),
(2024, 'QC', 3, 98540.00, 119910.00, 0.2300, NOW()),
(2024, 'QC', 4, 119910.00, 255625.00, 0.2575, NOW()),
(2024, 'QC', 5, 255625.00, NULL, 0.2975, NOW());

-- British Columbia Tax Brackets 2024
INSERT INTO income_tax_bracket (tax_year, jurisdiction, bracket_order, min_income, max_income, rate, created_at) VALUES
(2024, 'BC', 1, 0.00, 45654.00, 0.0506, NOW()),
(2024, 'BC', 2, 45654.00, 91310.00, 0.0770, NOW()),
(2024, 'BC', 3, 91310.00, 105685.00, 0.1050, NOW()),
(2024, 'BC', 4, 105685.00, 181232.00, 0.1229, NOW()),
(2024, 'BC', 5, 181232.00, NULL, 0.1679, NOW());

-- Alberta Tax Brackets 2024
INSERT INTO income_tax_bracket (tax_year, jurisdiction, bracket_order, min_income, max_income, rate, created_at) VALUES
(2024, 'AB', 1, 0.00, 148269.00, 0.1000, NOW()),
(2024, 'AB', 2, 148269.00, 177922.00, 0.1200, NOW()),
(2024, 'AB', 3, 177922.00, 237230.00, 0.1300, NOW()),
(2024, 'AB', 4, 237230.00, 355845.00, 0.1400, NOW()),
(2024, 'AB', 5, 355845.00, NULL, 0.1500, NOW());

-- Manitoba Tax Brackets 2024
INSERT INTO income_tax_bracket (tax_year, jurisdiction, bracket_order, min_income, max_income, rate, created_at) VALUES
(2024, 'MB', 1, 0.00, 36842.00, 0.1080, NOW()),
(2024, 'MB', 2, 36842.00, 73684.00, 0.1275, NOW()),
(2024, 'MB', 3, 73684.00, 210348.00, 0.1740, NOW()),
(2024, 'MB', 4, 210348.00, NULL, 0.2040, NOW());

-- Saskatchewan Tax Brackets 2024
INSERT INTO income_tax_bracket (tax_year, jurisdiction, bracket_order, min_income, max_income, rate, created_at) VALUES
(2024, 'SK', 1, 0.00, 49720.00, 0.1050, NOW()),
(2024, 'SK', 2, 49720.00, 99440.00, 0.1250, NOW()),
(2024, 'SK', 3, 99440.00, 142058.00, 0.1450, NOW()),
(2024, 'SK', 4, 142058.00, 270870.00, 0.1625, NOW()),
(2024, 'SK', 5, 270870.00, NULL, 0.1950, NOW());

-- Nova Scotia Tax Brackets 2024
INSERT INTO income_tax_bracket (tax_year, jurisdiction, bracket_order, min_income, max_income, rate, created_at) VALUES
(2024, 'NS', 1, 0.00, 29590.00, 0.0879, NOW()),
(2024, 'NS', 2, 29590.00, 59180.00, 0.1495, NOW()),
(2024, 'NS', 3, 59180.00, 93000.00, 0.1667, NOW()),
(2024, 'NS', 4, 93000.00, 150000.00, 0.1792, NOW()),
(2024, 'NS', 5, 150000.00, NULL, 0.2037, NOW());

-- New Brunswick Tax Brackets 2024
INSERT INTO income_tax_bracket (tax_year, jurisdiction, bracket_order, min_income, max_income, rate, created_at) VALUES
(2024, 'NB', 1, 0.00, 47715.00, 0.0940, NOW()),
(2024, 'NB', 2, 47715.00, 95431.00, 0.1400, NOW()),
(2024, 'NB', 3, 95431.00, 138586.00, 0.1600, NOW()),
(2024, 'NB', 4, 138586.00, 200000.00, 0.1733, NOW()),
(2024, 'NB', 5, 200000.00, NULL, 0.2030, NOW());

-- Prince Edward Island Tax Brackets 2024
INSERT INTO income_tax_bracket (tax_year, jurisdiction, bracket_order, min_income, max_income, rate, created_at) VALUES
(2024, 'PE', 1, 0.00, 31984.00, 0.0980, NOW()),
(2024, 'PE', 2, 31984.00, 63969.00, 0.1480, NOW()),
(2024, 'PE', 3, 63969.00, 111733.00, 0.1800, NOW()),
(2024, 'PE', 4, 111733.00, NULL, 0.1950, NOW());

-- Newfoundland and Labrador Tax Brackets 2024
INSERT INTO income_tax_bracket (tax_year, jurisdiction, bracket_order, min_income, max_income, rate, created_at) VALUES
(2024, 'NL', 1, 0.00, 41457.00, 0.0870, NOW()),
(2024, 'NL', 2, 41457.00, 82913.00, 0.1480, NOW()),
(2024, 'NL', 3, 82913.00, 148027.00, 0.1640, NOW()),
(2024, 'NL', 4, 148027.00, 207239.00, 0.1735, NOW()),
(2024, 'NL', 5, 207239.00, NULL, 0.2000, NOW());

-- Northwest Territories Tax Brackets 2024
INSERT INTO income_tax_bracket (tax_year, jurisdiction, bracket_order, min_income, max_income, rate, created_at) VALUES
(2024, 'NT', 1, 0.00, 55867.00, 0.0550, NOW()),
(2024, 'NT', 2, 55867.00, 111733.00, 0.0820, NOW()),
(2024, 'NT', 3, 111733.00, 160362.00, 0.1220, NOW()),
(2024, 'NT', 4, 160362.00, 240716.00, 0.1405, NOW()),
(2024, 'NT', 5, 240716.00, NULL, 0.1505, NOW());

-- Nunavut Tax Brackets 2024
INSERT INTO income_tax_bracket (tax_year, jurisdiction, bracket_order, min_income, max_income, rate, created_at) VALUES
(2024, 'NU', 1, 0.00, 55867.00, 0.0400, NOW()),
(2024, 'NU', 2, 55867.00, 111733.00, 0.0700, NOW()),
(2024, 'NU', 3, 111733.00, 160362.00, 0.0900, NOW()),
(2024, 'NU', 4, 160362.00, 240716.00, 0.1150, NOW()),
(2024, 'NU', 5, 240716.00, NULL, 0.1505, NOW());

-- Yukon Tax Brackets 2024
INSERT INTO income_tax_bracket (tax_year, jurisdiction, bracket_order, min_income, max_income, rate, created_at) VALUES
(2024, 'YT', 1, 0.00, 55867.00, 0.0600, NOW()),
(2024, 'YT', 2, 55867.00, 111733.00, 0.0900, NOW()),
(2024, 'YT', 3, 111733.00, 173205.00, 0.1100, NOW()),
(2024, 'YT', 4, 173205.00, 246752.00, 0.1280, NOW()),
(2024, 'YT', 5, 246752.00, NULL, 0.1500, NOW());

-- Federal Tax Credits 2024
INSERT INTO tax_credit (tax_year, jurisdiction, credit_code, credit_name, amount, rate, description, is_active, created_at) VALUES
(2024, 'FEDERAL', 'BPA', 'Basic Personal Amount', 15705.00, 0.15, 'Federal basic personal amount credit', TRUE, NOW()),
(2024, 'FEDERAL', 'AGE_AMOUNT', 'Age Amount (65+)', 8790.00, 0.15, 'Federal age amount credit for seniors', TRUE, NOW()),
(2024, 'FEDERAL', 'DISABILITY', 'Disability Amount', 9428.00, 0.15, 'Federal disability tax credit', TRUE, NOW()),
(2024, 'FEDERAL', 'CAREGIVER', 'Caregiver Amount', 3867.00, 0.15, 'Federal caregiver amount credit', TRUE, NOW()),
(2024, 'FEDERAL', 'DONATION_RATE_LOW', 'Donation Credit Low Rate', 200.00, 0.15, 'First $200 of donations at 15%', TRUE, NOW()),
(2024, 'FEDERAL', 'DONATION_RATE_HIGH', 'Donation Credit High Rate', 0.00, 0.2932, 'Donations over $200 at 29.32%', TRUE, NOW());

-- Ontario Tax Credits 2024
INSERT INTO tax_credit (tax_year, jurisdiction, credit_code, credit_name, amount, rate, description, is_active, created_at) VALUES
(2024, 'ON', 'BPA', 'Basic Personal Amount', 12150.00, 0.0505, 'Ontario basic personal amount credit', TRUE, NOW()),
(2024, 'ON', 'AGE_AMOUNT', 'Age Amount (65+)', 5717.00, 0.0505, 'Ontario age amount credit for seniors', TRUE, NOW()),
(2024, 'ON', 'DISABILITY', 'Disability Amount', 7876.00, 0.0505, 'Ontario disability tax credit', TRUE, NOW());

-- Quebec Tax Credits 2024
INSERT INTO tax_credit (tax_year, jurisdiction, credit_code, credit_name, amount, rate, description, is_active, created_at) VALUES
(2024, 'QC', 'BPA', 'Basic Personal Amount', 15705.00, 0.15, 'Quebec basic personal amount credit', TRUE, NOW()),
(2024, 'QC', 'AGE_AMOUNT', 'Age Amount (65+)', 3044.00, 0.15, 'Quebec age amount credit for seniors', TRUE, NOW()),
(2024, 'QC', 'DISABILITY', 'Disability Amount', 1793.00, 0.15, 'Quebec disability tax credit', TRUE, NOW());

-- British Columbia Tax Credits 2024
INSERT INTO tax_credit (tax_year, jurisdiction, credit_code, credit_name, amount, rate, description, is_active, created_at) VALUES
(2024, 'BC', 'BPA', 'Basic Personal Amount', 11739.00, 0.0506, 'BC basic personal amount credit', TRUE, NOW()),
(2024, 'BC', 'AGE_AMOUNT', 'Age Amount (65+)', 5217.00, 0.0506, 'BC age amount credit for seniors', TRUE, NOW()),
(2024, 'BC', 'DISABILITY', 'Disability Amount', 8667.00, 0.0506, 'BC disability tax credit', TRUE, NOW());

-- Alberta Tax Credits 2024
INSERT INTO tax_credit (tax_year, jurisdiction, credit_code, credit_name, amount, rate, description, is_active, created_at) VALUES
(2024, 'AB', 'BPA', 'Basic Personal Amount', 21885.00, 0.10, 'Alberta basic personal amount credit', TRUE, NOW()),
(2024, 'AB', 'AGE_AMOUNT', 'Age Amount (65+)', 6894.00, 0.10, 'Alberta age amount credit for seniors', TRUE, NOW()),
(2024, 'AB', 'DISABILITY', 'Disability Amount', 9428.00, 0.10, 'Alberta disability tax credit', TRUE, NOW());
