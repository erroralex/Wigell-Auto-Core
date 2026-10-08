-- service_package:
CREATE TABLE service_package
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT NOT NULL UNIQUE,
    description TEXT
);

-- service_package_item:
CREATE TABLE service_package_item
(
    package_id      INTEGER NOT NULL,
    service_item_id INTEGER NOT NULL,
    PRIMARY KEY (package_id, service_item_id),
    FOREIGN KEY (package_id) REFERENCES service_package (id) ON DELETE CASCADE,
    FOREIGN KEY (service_item_id) REFERENCES service_item (id)
);