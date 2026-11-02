-- Städar läckta statusvärden på booking.
-- CONFIRMED och DRAFT finns inte i BookingStatus och mappas till WORK_ORDER_CREATED.
UPDATE booking SET status = 'WORK_ORDER_CREATED' WHERE status = 'CONFIRMED';

UPDATE booking SET status = 'WORK_ORDER_CREATED' WHERE status = 'DRAFT';