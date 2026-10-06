-- V7__work_order_types.sql
-- Arbetsordern kan nu finnas utan bokning och får typ, fordon och nya fritextfält.
-- booking_id och mechanic_id blir valfria. Befintliga arbetsordrar blir PLANNED,
-- får fordonet från sin bokning och status CREATED byts mot CONFIRMED.
--
-- SQLite kan inte ändra kolumner i en befintlig tabell, så tabellen byggs om:
-- ny tabell -> kopiera data -> ta bort gamla -> byt namn.
--
-- work_order är en föräldratabell (invoice och work_order_service_item pekar hit),
-- så FK-kontrollen måste vara avslagen under ombyggnaden. PRAGMA foreign_keys
-- ignoreras inuti en transaktion, därför körs skriptet utanför Flyways transaktion
-- (se V7__work_order_types.sql.conf) och styr sin egen transaktion.

PRAGMA
foreign_keys = OFF;

SAVEPOINT v7_work_order_types;

CREATE TABLE work_order_new
(
    id                     INTEGER PRIMARY KEY AUTOINCREMENT,
    booking_id             INTEGER,
    mechanic_id            INTEGER,
    vehicle_id             INTEGER,
    original_work_order_id INTEGER,
    type                   TEXT NOT NULL,
    status                 TEXT NOT NULL DEFAULT 'CONFIRMED',
    problem_description    TEXT,
    planned_date           TEXT,
    customer_instructions  TEXT,
    comments               TEXT,
    estimated_minutes      INTEGER,
    FOREIGN KEY (booking_id) REFERENCES booking (id),
    FOREIGN KEY (mechanic_id) REFERENCES mechanic (id),
    FOREIGN KEY (vehicle_id) REFERENCES vehicle (id),
    FOREIGN KEY (original_work_order_id) REFERENCES work_order (id)
);

-- id kopieras explicit så att invoice.work_order_id fortsätter peka rätt.
-- LEFT JOIN: en arbetsorder vars bokning saknas försvinner inte, den får vehicle_id = NULL.
INSERT INTO work_order_new (id, booking_id, mechanic_id, vehicle_id, type, status)
SELECT o.id,
       o.booking_id,
       o.mechanic_id,
       b.vehicle_id,
       'PLANNED',
       CASE o.status
           WHEN 'CREATED' THEN 'CONFIRMED'
           ELSE o.status
           END
FROM work_order o
         LEFT JOIN booking b ON b.id = o.booking_id;

DROP TABLE work_order;

ALTER TABLE work_order_new
    RENAME TO work_order;

-- En bokning kan bara ha en arbetsorder. Flera NULL tillåts, så fristående arbetsordrar fungerar.
CREATE UNIQUE INDEX ux_work_order_booking_id ON work_order (booking_id);

RELEASE SAVEPOINT v7_work_order_types;

PRAGMA
foreign_keys = ON;