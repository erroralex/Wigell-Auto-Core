-- Partiellt unikt index. Indexet gäller då bara rader där status inte är CANCELLED
DROP INDEX ux_work_order_booking_id;

CREATE UNIQUE INDEX ux_work_order_active_booking
    ON work_order (booking_id)
    WHERE status <> 'CANCELLED';