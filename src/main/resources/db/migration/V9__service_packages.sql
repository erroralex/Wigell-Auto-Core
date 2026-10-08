-- service_package:
CREATE TABLE service_package
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT NOT NULL UNIQUE,
    description TEXT
);

-- service_package_item:
CREATE TABLE service_package_item
(
    package_id      INTEGER NOT NULL,
    service_item_id INTEGER NOT NULL,
    PRIMARY KEY (package_id, service_item_id),
    FOREIGN KEY (package_id) REFERENCES service_package (id) ON DELETE CASCADE,
    FOREIGN KEY (service_item_id) REFERENCES service_item (id)
);

-- Seed data
INSERT INTO service_package (name, description)
VALUES ('Basic service', 'Annual service including oil and oil filter change'),
       ('Winter check', 'Brake inspection and fault code diagnostics before winter'),
       ('Summer/holiday check', 'Fresh oil and checked brakes before a long trip'),
       ('Purchase inspection', 'Thorough check of a used car before purchase');

INSERT INTO service_package_item (package_id, service_item_id)
SELECT p.id, s.id
FROM service_package p, service_item s
WHERE p.name = 'Basic service' AND s.name IN ('Annual service', 'Oil change');

INSERT INTO service_package_item (package_id, service_item_id)
SELECT p.id, s.id
FROM service_package p, service_item s
WHERE p.name = 'Winter check' AND s.name IN ('Brake service', 'Diagnostics');

INSERT INTO service_package_item (package_id, service_item_id)
SELECT p.id, s.id
FROM service_package p, service_item s
WHERE p.name = 'Summer/holiday check' AND s.name IN ('Oil change', 'Brake service');

INSERT INTO service_package_item (package_id, service_item_id)
SELECT p.id, s.id
FROM service_package p, service_item s
WHERE p.name = 'Purchase inspection' AND s.name IN ('Diagnostics', 'Brake service', 'Annual service');
