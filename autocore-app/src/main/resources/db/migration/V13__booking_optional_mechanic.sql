-- V13__booking_optional_mechanic.sql
-- Bokningens mekaniker blir ett valfritt förslag: mechanic_id får vara NULL.
-- Förslaget reserverar ingen tid; mekanikern reserveras först på arbetsordern.
-- Inga data ändras.
--
-- SQLite kan inte ta bort NOT NULL från en kolumn, så tabellen byggs om enligt
-- samma mönster som V7: ny tabell -> kopiera data -> ta bort gamla -> byt namn.
-- booking är en föräldratabell (work_order och booking_service_item pekar hit),
-- så FK-kontrollen stängs av och skriptet körs utanför Flyways transaktion
-- (se V13__booking_optional_mechanic.sql.conf).

PRAGMA
foreign_keys = OFF;

SAVEPOINT v13_booking_optional_mechanic;

CREATE TABLE booking_new
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    vehicle_id  INTEGER NOT NULL,
    mechanic_id INTEGER,
    date        TEXT    NOT NULL,
    start_time  TEXT    NOT NULL,
    end_time    TEXT    NOT NULL,
    description TEXT    NOT NULL,
    status      TEXT    NOT NULL DEFAULT 'BOOKED',
    FOREIGN KEY (vehicle_id) REFERENCES vehicle (id),
    FOREIGN KEY (mechanic_id) REFERENCES mechanic (id)
);

-- id kopieras explicit så att work_order.booking_id och booking_service_item.booking_id fortsätter peka rätt.
INSERT INTO booking_new (id, vehicle_id, mechanic_id, date, start_time, end_time, description, status)
SELECT id, vehicle_id, mechanic_id, date, start_time, end_time, description, status
FROM booking;

DROP TABLE booking;

ALTER TABLE booking_new
    RENAME TO booking;

RELEASE SAVEPOINT v13_booking_optional_mechanic;

PRAGMA
foreign_keys = ON;