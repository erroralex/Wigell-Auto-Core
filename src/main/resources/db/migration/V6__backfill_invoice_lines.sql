-- V5: skapar tabellen för fakturarader och lämnas oförändrad för att befintliga databaser ska kunna uppdateras säkert.
-- V6: fyller på saknade historiska rader i den befintliga tabellen.
-- Gäller fakturor utan rabatt där radpriserna summerar till fakturans belopp.
-- För äldre fakturor med rabatt vet vi inte säkert hur rabatten fördelades mellan tjänsterna.
-- Befintliga fakturarader och fakturans totalsummor lämnas oförändrade.
INSERT INTO invoice_line (
    invoice_id,
    service_item_name,
    amount,
    discount,
    total
)
SELECT
    i.id,
    wsi.service_name,
    wsi.agreed_price,
    0,
    wsi.agreed_price
FROM invoice i
         JOIN work_order_service_item wsi
              ON wsi.work_order_id = i.work_order_id
WHERE NOT EXISTS (
    SELECT 1
    FROM invoice_line il
    WHERE il.invoice_id = i.id
)
  AND i.discount = 0
  AND ABS(i.total_amount - i.amount) < 0.005
  AND NOT EXISTS (
    SELECT 1
    FROM work_order_service_item invalid_item
    WHERE invalid_item.work_order_id = i.work_order_id
      AND (
        invalid_item.service_name IS NULL
            OR TRIM(invalid_item.service_name) = ''
            OR invalid_item.agreed_price IS NULL
            OR invalid_item.agreed_price < 0
        )
)
  AND ABS(
              i.amount - (
                  SELECT SUM(item.agreed_price)
                  FROM work_order_service_item item
                  WHERE item.work_order_id = i.work_order_id
              )
      ) < 0.005;