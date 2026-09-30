-- V4__work_order_item_snapshots.sql
-- Arbetsorderns rader får samma snapshot som bokningens rader: namn, avtalat pris och tid.
-- SQLite kan inte ändra kolumner i en befintlig tabell, så tabellen byggs om:
-- ny tabell -> kopiera data -> ta bort gamla -> byt namn.

CREATE TABLE work_order_service_item_new
(
    work_order_id    INTEGER NOT NULL,
    service_item_id  INTEGER NOT NULL,
    service_name     TEXT    NOT NULL,
    agreed_price     REAL    NOT NULL,
    duration_minutes INTEGER NOT NULL,
    PRIMARY KEY (work_order_id, service_item_id),
    FOREIGN KEY (work_order_id) REFERENCES work_order (id),
    FOREIGN KEY (service_item_id) REFERENCES service_item (id)
);

-- Befintliga rader fylls i från bokningens snapshot i första hand.
-- Saknas bokningsraden används radens gamla pris, och i sista hand tjänstekatalogen.
INSERT INTO work_order_service_item_new
(work_order_id, service_item_id, service_name, agreed_price, duration_minutes)
SELECT old_item.work_order_id,
       old_item.service_item_id,
       COALESCE(booking_item.service_name, service_item.name),
       COALESCE(booking_item.price_at_booking, old_item.price, service_item.price),
       COALESCE(booking_item.duration_minutes, service_item.estimated_minutes)
FROM work_order_service_item old_item
         JOIN work_order
              ON work_order.id = old_item.work_order_id
         JOIN service_item
              ON service_item.id = old_item.service_item_id
         LEFT JOIN booking_service_item booking_item
                   ON booking_item.booking_id = work_order.booking_id
                       AND booking_item.service_item_id = old_item.service_item_id;

DROP TABLE work_order_service_item;

ALTER TABLE work_order_service_item_new
    RENAME TO work_order_service_item;

-- En bokning kan bara ha en arbetsorder.
CREATE UNIQUE INDEX ux_work_order_booking_id ON work_order (booking_id);