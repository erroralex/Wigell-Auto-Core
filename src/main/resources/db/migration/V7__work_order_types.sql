PRAGMA
foreign_keys=OFF;
BEGIN
TRANSACTION;

CREATE TABLE work_order_new
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    booking_id  INTEGER,
    mechanic_id INTEGER,
    type TEXT NOT NULL,
    status      TEXT    NOT NULL DEFAULT 'CONFIRMED',
    original_work_order_id ... REFERENCES work_order(id)
    FOREIGN KEY (booking_id) REFERENCES booking (id),
    FOREIGN KEY (mechanic_id) REFERENCES mechanic (id),
    FOREIGN KEY (vehicle) REFERENCES vehicle (id)
);



COMMIT;
PRAGMA
foreign_keys=ON;