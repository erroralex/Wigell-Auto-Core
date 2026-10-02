CREATE TABLE invoice_line (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      invoice_id INTEGER NOT NULL,
      service_item_name TEXT,
      amount REAL,
      discount REAL,
      FOREIGN KEY (invoice_id) REFERENCES invoice(id)
);