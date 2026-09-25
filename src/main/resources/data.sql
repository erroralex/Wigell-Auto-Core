-- data.sql: grunddata för mekaniker och tjänster.
-- Körs av Spring vid varje start, efter schema.sql (spring.sql.init.mode=always).
-- INSERT OR IGNORE hoppar över rader vars id redan finns, så befintlig data skrivs aldrig över.
-- Samma id:n som i Database.java, eftersom CreateBookingDialog fortfarande väljer mekaniker därifrån.

-- customer
INSERT OR IGNORE INTO customer (id, name, phone, email, vip) VALUES
    (1, 'Anna Andersson', '070-1111111', 'anna.andersson@email.se', 0),
    (2, 'Erik Eriksson', '070-2222222', 'erik.eriksson@email.se', 1),
    (3, 'Maria Svensson', '070-3333333', 'maria.svensson@email.se', 0);

-- mechanic
INSERT OR IGNORE INTO mechanic (id, name, phone, specialization, available) VALUES
    (1, 'Johan Karlsson', '070-5551111', 'General service', 1),
    (2, 'Sara Nilsson', '070-5552222', 'Brakes', 1),
    (3, 'Mikael Berg', '070-5553333', 'Diagnostics', 1);

-- service_item
INSERT OR IGNORE INTO service_item (id, name, description, price, estimated_minutes) VALUES
    (1, 'Oil change', 'Engine oil and oil filter replacement', 1295.0, 45),
    (2, 'Brake service', 'Inspection and replacement of front brake pads', 2495.0, 90),
    (3, 'Diagnostics', 'Electronic fault code diagnostics', 995.0, 60),
    (4, 'Annual service', 'Standard annual vehicle service', 3495.0, 120);

-- vehicle
INSERT OR IGNORE INTO vehicle (id, registration_number, brand, model, year, customer_id) VALUES
    (1, 'ABC123', 'Volvo', 'V70', 2012, 1),
    (2, 'DEF456', 'Volkswagen', 'Passat', 2018, 2),
    (3, 'GHI789', 'Toyota', 'Corolla', 2020, 3);

-- booking
INSERT OR IGNORE INTO booking
    (id, vehicle_id, mechanic_id, date, start_time, end_time, description, status)
VALUES
    (1, 1, 1, '2026-09-21', '08:00', '10:45', 'Annual maintenance and oil change', 'COMPLETED'),
    (2, 2, 2, '2026-09-22', '10:00', '13:00', 'Brake inspection and electronic diagnostics', 'COMPLETED'),
    (3, 3, 3, '2026-09-28', '09:00', '10:00', 'Warning light diagnostics', 'WORK_ORDER_CREATED');

-- booking_service_item
INSERT OR IGNORE INTO booking_service_item (booking_id, service_item_id) VALUES
    (1, 1),
    (1, 4),
    (2, 2),
    (2, 3),
    (3, 3);

-- work_order
INSERT OR IGNORE INTO work_order (id, booking_id, mechanic_id, status) VALUES
    (1, 1, 1, 'COMPLETED'),
    (2, 2, 2, 'COMPLETED'),
    (3, 3, 3, 'CREATED');

-- work_order_service_item
INSERT OR IGNORE INTO work_order_service_item (work_order_id, service_item_id) VALUES
    (1, 1),
    (1, 4),
    (2, 2),
    (2, 3),
    (3, 3);

-- invoice
INSERT OR IGNORE INTO invoice
    (id, work_order_id, invoice_date, amount, discount, total_amount, paid)
VALUES
    (1, 1, '2026-09-21', 4790.0, 0.0, 4790.0, 1),
    (2, 2, '2026-09-22', 3490.0, 0.0, 3490.0, 0);

-- payment
INSERT OR IGNORE INTO payment
(id, invoice_id, amount, payment_type, payment_date, successful)
VALUES
    (1, 1, 4790.0, 'CARD', '2026-09-21 11:00', 1);
