-- data.sql: grunddata för mekaniker och tjänster.
-- Körs av Spring vid varje start, efter schema.sql (spring.sql.init.mode=always).
-- INSERT OR IGNORE hoppar över rader vars id redan finns, så befintlig data skrivs aldrig över.
-- Samma id:n som i Database.java, eftersom CreateBookingDialog fortfarande väljer mekaniker därifrån.

-- mechanic:
INSERT OR IGNORE INTO mechanic (id, name, phone, specialization, available) VALUES
    (1, 'Johan Karlsson', '070-5551111', 'General service', 1),
    (2, 'Sara Nilsson',   '070-5552222', 'Brakes',          1),
    (3, 'Mikael Berg',    '070-5553333', 'Diagnostics',     1);

-- service_item:
INSERT OR IGNORE INTO service_item (id, name, description, price, estimated_minutes) VALUES
    (1, 'Oil change',     'Engine oil and oil filter replacement',          1295.0,  45),
    (2, 'Brake service',  'Inspection and replacement of front brake pads', 2495.0,  90),
    (3, 'Diagnostics',    'Electronic fault code diagnostics',               995.0,  60),
    (4, 'Annual service', 'Standard annual vehicle service',                3495.0, 120);