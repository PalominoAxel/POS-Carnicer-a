-- Catalogo inicial de productos carnicos para Don Toribio S.A.C.
-- Ejecutar contra la base "dontoribio_pos" despues de crear el esquema (schema.sql).
-- Precios de referencia en Soles (S/) por kilogramo; precio mayorista con descuento sobre minorista.

-- === RES (12) ===
INSERT INTO productos (codigo, nombre, precio_kg_minorista, precio_kg_mayorista, unidad_medida, stock_disponible, activo) VALUES
('RES-001', 'Bistec de Res',            34.90, 31.50, 'kg', 45.000, true),
('RES-002', 'Lomo Fino de Res',         45.90, 41.90, 'kg', 20.000, true),
('RES-003', 'Asado de Tira',            28.90, 25.90, 'kg', 35.000, true),
('RES-004', 'Churrasco de Res',         36.90, 33.50, 'kg', 30.000, true),
('RES-005', 'Punta de Asado',           27.90, 24.90, 'kg', 25.000, true),
('RES-006', 'Osobuco de Res',           24.90, 22.00, 'kg', 28.000, true),
('RES-007', 'Costillar de Res',         26.90, 23.90, 'kg', 32.000, true),
('RES-008', 'Pecho de Res',             22.90, 20.50, 'kg', 26.000, true),
('RES-009', 'Falda de Res',             21.90, 19.50, 'kg', 24.000, true),
('RES-010', 'Malaya de Res',            30.90, 27.90, 'kg', 18.000, true),
('RES-011', 'Carne Molida de Res',      29.90, 26.90, 'kg', 40.000, true),
('RES-012', 'Cadera de Res',            33.90, 30.50, 'kg', 22.000, true);

-- === CERDO (10) ===
INSERT INTO productos (codigo, nombre, precio_kg_minorista, precio_kg_mayorista, unidad_medida, stock_disponible, activo) VALUES
('CER-001', 'Panceta de Cerdo',         22.90, 20.50, 'kg', 38.000, true),
('CER-002', 'Chuleta de Cerdo',         24.90, 22.50, 'kg', 34.000, true),
('CER-003', 'Pierna de Cerdo',          21.90, 19.90, 'kg', 42.000, true),
('CER-004', 'Brazo de Cerdo (Paleta)',  20.90, 18.90, 'kg', 36.000, true),
('CER-005', 'Costillar de Cerdo',       23.90, 21.50, 'kg', 30.000, true),
('CER-006', 'Cerdo Entero',             19.90, 17.50, 'kg', 15.000, true),
('CER-007', 'Pellejo de Cerdo',         12.90, 11.00, 'kg', 20.000, true),
('CER-008', 'Manteca de Cerdo',         14.90, 13.00, 'kg', 18.000, true),
('CER-009', 'Cabeza de Cerdo',          11.90,  9.90, 'kg', 12.000, true),
('CER-010', 'Pata de Cerdo',            13.90, 11.90, 'kg', 20.000, true);

-- === POLLO (10) ===
INSERT INTO productos (codigo, nombre, precio_kg_minorista, precio_kg_mayorista, unidad_medida, stock_disponible, activo) VALUES
('POL-001', 'Pechuga de Pollo',         16.90, 14.90, 'kg', 50.000, true),
('POL-002', 'Pierna de Pollo',          13.90, 12.20, 'kg', 48.000, true),
('POL-003', 'Ala de Pollo',             11.90, 10.20, 'kg', 30.000, true),
('POL-004', 'Muslo de Pollo',           12.90, 11.20, 'kg', 40.000, true),
('POL-005', 'Pollo Entero Beneficiado', 10.90,  9.50, 'kg', 60.000, true),
('POL-006', 'Higado de Pollo',           8.90,  7.50, 'kg', 15.000, true),
('POL-007', 'Molleja de Pollo',          9.90,  8.50, 'kg', 14.000, true),
('POL-008', 'Espinazo de Pollo',         6.90,  5.90, 'kg', 20.000, true),
('POL-009', 'Patas de Pollo',            7.90,  6.50, 'kg', 16.000, true),
('POL-010', 'Pollo Trozado (Presas Mixtas)', 12.50, 11.00, 'kg', 35.000, true);

-- === MENUDENCIA (8) ===
INSERT INTO productos (codigo, nombre, precio_kg_minorista, precio_kg_mayorista, unidad_medida, stock_disponible, activo) VALUES
('MEN-001', 'Higado de Res',            15.90, 13.90, 'kg', 20.000, true),
('MEN-002', 'Rinon de Res',             12.90, 11.00, 'kg', 14.000, true),
('MEN-003', 'Mollejas de Res',          18.90, 16.50, 'kg', 12.000, true),
('MEN-004', 'Panza de Res (Mondongo)',  14.90, 12.90, 'kg', 18.000, true),
('MEN-005', 'Sesos de Res',             13.90, 11.90, 'kg', 10.000, true),
('MEN-006', 'Ubre de Res',              11.90,  9.90, 'kg',  8.000, true),
('MEN-007', 'Corazon de Res',           13.90, 11.90, 'kg', 15.000, true),
('MEN-008', 'Lengua de Res',            22.90, 19.90, 'kg', 10.000, true);

-- === EMBUTIDOS (8) ===
INSERT INTO productos (codigo, nombre, precio_kg_minorista, precio_kg_mayorista, unidad_medida, stock_disponible, activo) VALUES
('EMB-001', 'Chorizo Parrillero',       18.90, 16.90, 'kg', 25.000, true),
('EMB-002', 'Salchicha Huachana',       16.90, 14.90, 'kg', 22.000, true),
('EMB-003', 'Longaniza Ahumada',        17.90, 15.90, 'kg', 20.000, true),
('EMB-004', 'Jamonada Clasica',         14.90, 13.00, 'kg', 30.000, true),
('EMB-005', 'Hot Dog Premium',          15.90, 13.90, 'kg', 24.000, true),
('EMB-006', 'Chorizo Criollo',          19.90, 17.50, 'kg', 18.000, true),
('EMB-007', 'Morcilla (Relleno Norteño)', 13.90, 12.00, 'kg', 12.000, true),
('EMB-008', 'Tocino Ahumado',           21.90, 19.50, 'kg', 16.000, true);
