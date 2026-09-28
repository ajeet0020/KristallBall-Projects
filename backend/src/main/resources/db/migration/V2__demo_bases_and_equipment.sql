INSERT INTO bases (name, location) VALUES
    ('North Base', 'Demo Region North'),
    ('South Base', 'Demo Region South');

INSERT INTO equipment (type, name, base_id, quantity, status)
SELECT 'VEHICLE', 'Utility Vehicle', id, 12, 'AVAILABLE' FROM bases WHERE name = 'North Base';
INSERT INTO equipment (type, name, base_id, quantity, status)
SELECT 'AMMUNITION', 'Training Ammunition', id, 500, 'LIMITED' FROM bases WHERE name = 'South Base';
