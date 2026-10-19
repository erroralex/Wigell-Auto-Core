-- Ordern får en egen starttid, så att krockar för mekaniker kan kontrolleras för alla ordertyper (Kortförslag M).
-- Nullbar: utkast, drop-in och reklamation kan sakna starttid.
ALTER TABLE work_order ADD COLUMN planned_start_time TEXT;

-- Befintliga planerade ordrar får datum och starttid från sin bokning.
-- Ett datum som redan har satts på ordern behålls.
UPDATE work_order
SET planned_date       = COALESCE(planned_date, (SELECT b.date FROM booking b WHERE b.id = work_order.booking_id)),
    planned_start_time = (SELECT b.start_time FROM booking b WHERE b.id = work_order.booking_id)
WHERE type = 'PLANNED'
  AND booking_id IS NOT NULL;
