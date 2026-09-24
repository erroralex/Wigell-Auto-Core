-- SQLite schema for Wigell AutoCore.
-- All statements use CREATE TABLE IF NOT EXISTS so startup can run repeatedly without destroying existing data.

-- customer:
CREATE TABLE IF NOT EXISTS customer (
    id     INTEGER PRIMARY KEY AUTOINCREMENT,
    name   TEXT NOT NULL,
    phone  TEXT NOT NULL,
    email  TEXT NOT NULL,
    vip    INTEGER NOT NULL DEFAULT 0 -- boolean: 0 = false, 1 = true
);

-- mechanic:
CREATE TABLE IF NOT EXISTS mechanic (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    name            TEXT NOT NULL,
    phone           TEXT NOT NULL,
    specialization  TEXT NOT NULL,
    available       INTEGER NOT NULL DEFAULT 1 -- boolean: 0 = false, 1 = true
);

-- service_item:
CREATE TABLE IF NOT EXISTS service_item (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    name               TEXT NOT NULL,
    description        TEXT NOT NULL,
    price              REAL NOT NULL,
    estimated_minutes  INTEGER NOT NULL
);

-- vehicle:
-- registration_number is unique because it identifies a physical vehicle.
CREATE TABLE IF NOT EXISTS vehicle (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    registration_number   TEXT NOT NULL UNIQUE,
    brand                 TEXT NOT NULL,
    model                 TEXT NOT NULL,
    year                  INTEGER NOT NULL,
    customer_id           INTEGER NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customer(id)
    );

-- booking:
CREATE TABLE IF NOT EXISTS booking (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    vehicle_id   INTEGER NOT NULL,
    mechanic_id  INTEGER NOT NULL,
    date         TEXT NOT NULL,
    start_time   TEXT NOT NULL,
    description  TEXT NOT NULL,
    status       TEXT NOT NULL DEFAULT 'BOOKED',
    FOREIGN KEY (vehicle_id) REFERENCES vehicle(id),
    FOREIGN KEY (mechanic_id) REFERENCES mechanic(id)
    );

-- work_order:
CREATE TABLE IF NOT EXISTS work_order (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    booking_id    INTEGER NOT NULL,
    mechanic_id   INTEGER NOT NULL,
    status        TEXT NOT NULL DEFAULT 'CREATED',
    FOREIGN KEY (booking_id) REFERENCES booking(id),
    FOREIGN KEY (mechanic_id) REFERENCES mechanic(id)
    );

-- invoice:
CREATE TABLE IF NOT EXISTS invoice (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    work_order_id  INTEGER NOT NULL,
    invoice_date   TEXT NOT NULL,
    amount         REAL NOT NULL,
    discount       REAL NOT NULL DEFAULT 0,
    total_amount   REAL NOT NULL,
    paid           INTEGER NOT NULL DEFAULT 0, -- boolean: 0 = false, 1 = true
    FOREIGN KEY (work_order_id) REFERENCES work_order(id)
    );

-- payment:
CREATE TABLE IF NOT EXISTS payment (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    invoice_id    INTEGER NOT NULL,
    amount        REAL NOT NULL,
    payment_type  TEXT NOT NULL,
    payment_date  TEXT NOT NULL,
    successful    INTEGER NOT NULL DEFAULT 0, -- boolean: 0 = false, 1 = true
    FOREIGN KEY (invoice_id) REFERENCES invoice(id)
    );

-- booking_service_item: join table between booking and service_item.
-- This table is created with a composite primary key to avoid duplicate relations, per the explicit Jira requirement.
CREATE TABLE IF NOT EXISTS booking_service_item (
    booking_id       INTEGER NOT NULL,
    service_item_id  INTEGER NOT NULL,
    PRIMARY KEY (booking_id, service_item_id),
    FOREIGN KEY (booking_id) REFERENCES booking(id),
    FOREIGN KEY (service_item_id) REFERENCES service_item(id)
    );

-- work_order_service_item: join table backing WorkOrder.
-- This table is created with a composite primary key.
CREATE TABLE IF NOT EXISTS work_order_service_item (
    work_order_id    INTEGER NOT NULL,
    service_item_id  INTEGER NOT NULL,
    PRIMARY KEY (work_order_id, service_item_id),
    FOREIGN KEY (work_order_id) REFERENCES work_order(id),
    FOREIGN KEY (service_item_id) REFERENCES service_item(id)
    );