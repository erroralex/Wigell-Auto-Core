CREATE TABLE booking_service_item_new (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    booking_id         INTEGER NOT NULL,
    service_item_id    INTEGER NOT NULL,
    service_name       TEXT NOT NULL,
    price_at_booking   REAL NOT NULL,
    duration_minutes   INTEGER NOT NULL,
    CONSTRAINT uk_booking_service_item_booking_service
    UNIQUE (booking_id, service_item_id),
    FOREIGN KEY (booking_id) REFERENCES booking(id),
    FOREIGN KEY (service_item_id) REFERENCES service_item(id)
);

INSERT INTO booking_service_item_new
    (booking_id, service_item_id, service_name, price_at_booking, duration_minutes)
SELECT
    old_item.booking_id,
    old_item.service_item_id,
    service_item.name,
    service_item.price,
    service_item.estimated_minutes
FROM booking_service_item old_item
         JOIN service_item
              ON service_item.id = old_item.service_item_id;

DROP TABLE booking_service_item;

ALTER TABLE booking_service_item_new
    RENAME TO booking_service_item;