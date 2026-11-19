-- =============================================================================
-- ORDENHA MONITORADA · Parte 1 (Banco de dados)
-- Script 02 de 03: carga inicial. Rode DEPOIS do 01_criar_tabelas.sql.
-- ARQUIVO GERADO por banco/gerar_dados.py; não edite à mão.
--
-- As datas são relativas ao dia da execução: a carga cobre os 10 dias
-- anteriores a hoje (CURDATE() - 10 dias até CURDATE() - 1 dia).
-- Usuários de teste (senha senha123, gravada só como hash BCrypt):
--   joao@exemplo.com
--   maria@exemplo.com
-- Os alertas foram calculados com as mesmas regras da API (RegrasAlertaService).
--
-- Roteiro do rebanho (para consultas, testes e vídeo):
--   vaca 1 BR-001 Bonita    Normal
--   vaca 2 BR-002 Malhada   Normal
--   vaca 3 BR-003 Mimosa    Condutividade subindo nos últimos dias: alertas de SUSPEITA_MASTITE abertos
--   vaca 4 BR-004 Estrela   Produção caindo nas 4 últimas ordenhas: QUEDA_PRODUCAO aberta
--   vaca 5 BR-005 Princesa  Normal
--   vaca 6 SL-010 Pintada   Mastite nos dias -8/-7, tratada desde o dia -6: LEITE_EM_CARENCIA aberto; 2 tratamentos em 12 meses
--   vaca 7 SL-011 Faceira   Pico isolado no dia -4: alerta resolvido (CMT negativo, falso positivo)
--   vaca 8 SL-012 Serena    Seca: não é ordenhada
-- =============================================================================

SET NAMES utf8mb4;
USE ordenha;

START TRANSACTION;

-- USUARIO
INSERT INTO usuario (id, nome, email, senha_hash) VALUES (1, 'João Ferreira', 'joao@exemplo.com', '$2a$10$dJLHVPXnJCFg2aOsSeMXkuGM0cHLJ1x5w1ZfXJGY.gg2q.GtbdWta');
INSERT INTO usuario (id, nome, email, senha_hash) VALUES (2, 'Maria Souza', 'maria@exemplo.com', '$2a$10$yv/JYJu1n4mCM0SbBcfUqO/NxVYjDnDuLnaoEBPkxFrkpapUekcZK');

-- PROPRIEDADE
INSERT INTO propriedade (id, usuario_id, nome, municipio, uf) VALUES (1, 1, 'Sítio Boa Vista', 'Itamonte', 'MG');
INSERT INTO propriedade (id, usuario_id, nome, municipio, uf) VALUES (2, 2, 'Fazendinha Santa Luzia', 'Pouso Alegre', 'MG');

-- VACA
INSERT INTO vaca (id, propriedade_id, brinco, nome, data_nascimento, status) VALUES (1, 1, 'BR-001', 'Bonita', DATE '2020-03-12', 'ATIVA');
INSERT INTO vaca (id, propriedade_id, brinco, nome, data_nascimento, status) VALUES (2, 1, 'BR-002', 'Malhada', DATE '2019-08-02', 'ATIVA');
INSERT INTO vaca (id, propriedade_id, brinco, nome, data_nascimento, status) VALUES (3, 1, 'BR-003', 'Mimosa', DATE '2021-01-20', 'ATIVA');
INSERT INTO vaca (id, propriedade_id, brinco, nome, data_nascimento, status) VALUES (4, 1, 'BR-004', 'Estrela', DATE '2018-11-05', 'ATIVA');
INSERT INTO vaca (id, propriedade_id, brinco, nome, data_nascimento, status) VALUES (5, 1, 'BR-005', 'Princesa', DATE '2022-02-14', 'ATIVA');
INSERT INTO vaca (id, propriedade_id, brinco, nome, data_nascimento, status) VALUES (6, 2, 'SL-010', 'Pintada', DATE '2020-06-30', 'EM_TRATAMENTO');
INSERT INTO vaca (id, propriedade_id, brinco, nome, data_nascimento, status) VALUES (7, 2, 'SL-011', 'Faceira', DATE '2021-09-09', 'ATIVA');
INSERT INTO vaca (id, propriedade_id, brinco, nome, data_nascimento, status) VALUES (8, 2, 'SL-012', 'Serena', DATE '2017-04-18', 'SECA');

-- TRATAMENTO
INSERT INTO tratamento (id, vaca_id, medicamento, data_inicio, data_fim, dias_carencia, observacao) VALUES (1, 6, 'Anti-inflamatório', CURDATE() - INTERVAL 48 DAY, CURDATE() - INTERVAL 46 DAY, 2, 'Tratamento anterior, já liberado. Dados fictícios.');
INSERT INTO tratamento (id, vaca_id, medicamento, data_inicio, data_fim, dias_carencia, observacao) VALUES (2, 6, 'Cefalexina intramamária', CURDATE() - INTERVAL 6 DAY, CURDATE() - INTERVAL 2 DAY, 4, 'Quarto posterior esquerdo, após alerta do sistema e teste da caneca. Dados fictícios.');

-- ORDENHA (140 leituras: 7 vacas em lactação x 10 dias x 2 turnos)
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (1, 1, TIMESTAMP(CURDATE() - INTERVAL 10 DAY, '06:00:00'), 'MANHA', 8.2, 0.429);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (2, 2, TIMESTAMP(CURDATE() - INTERVAL 10 DAY, '06:04:00'), 'MANHA', 9.0, 0.399);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (3, 3, TIMESTAMP(CURDATE() - INTERVAL 10 DAY, '06:08:00'), 'MANHA', 7.0, 0.435);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (4, 4, TIMESTAMP(CURDATE() - INTERVAL 10 DAY, '06:12:00'), 'MANHA', 8.2, 0.382);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (5, 5, TIMESTAMP(CURDATE() - INTERVAL 10 DAY, '06:16:00'), 'MANHA', 6.4, 0.398);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (6, 6, TIMESTAMP(CURDATE() - INTERVAL 10 DAY, '06:20:00'), 'MANHA', 10.0, 0.421);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (7, 7, TIMESTAMP(CURDATE() - INTERVAL 10 DAY, '06:24:00'), 'MANHA', 7.4, 0.368);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (8, 1, TIMESTAMP(CURDATE() - INTERVAL 10 DAY, '16:30:00'), 'TARDE', 7.1, 0.435);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (9, 2, TIMESTAMP(CURDATE() - INTERVAL 10 DAY, '16:34:00'), 'TARDE', 8.8, 0.411);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (10, 3, TIMESTAMP(CURDATE() - INTERVAL 10 DAY, '16:38:00'), 'TARDE', 5.8, 0.437);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (11, 4, TIMESTAMP(CURDATE() - INTERVAL 10 DAY, '16:42:00'), 'TARDE', 7.4, 0.377);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (12, 5, TIMESTAMP(CURDATE() - INTERVAL 10 DAY, '16:46:00'), 'TARDE', 5.7, 0.417);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (13, 6, TIMESTAMP(CURDATE() - INTERVAL 10 DAY, '16:50:00'), 'TARDE', 9.5, 0.437);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (14, 7, TIMESTAMP(CURDATE() - INTERVAL 10 DAY, '16:54:00'), 'TARDE', 6.8, 0.344);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (15, 1, TIMESTAMP(CURDATE() - INTERVAL 9 DAY, '06:00:00'), 'MANHA', 7.9, 0.419);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (16, 2, TIMESTAMP(CURDATE() - INTERVAL 9 DAY, '06:04:00'), 'MANHA', 9.3, 0.411);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (17, 3, TIMESTAMP(CURDATE() - INTERVAL 9 DAY, '06:08:00'), 'MANHA', 6.8, 0.441);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (18, 4, TIMESTAMP(CURDATE() - INTERVAL 9 DAY, '06:12:00'), 'MANHA', 8.8, 0.379);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (19, 5, TIMESTAMP(CURDATE() - INTERVAL 9 DAY, '06:16:00'), 'MANHA', 6.3, 0.409);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (20, 6, TIMESTAMP(CURDATE() - INTERVAL 9 DAY, '06:20:00'), 'MANHA', 10.0, 0.431);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (21, 7, TIMESTAMP(CURDATE() - INTERVAL 9 DAY, '06:24:00'), 'MANHA', 7.2, 0.384);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (22, 1, TIMESTAMP(CURDATE() - INTERVAL 9 DAY, '16:30:00'), 'TARDE', 6.8, 0.413);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (23, 2, TIMESTAMP(CURDATE() - INTERVAL 9 DAY, '16:34:00'), 'TARDE', 8.7, 0.407);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (24, 3, TIMESTAMP(CURDATE() - INTERVAL 9 DAY, '16:38:00'), 'TARDE', 6.1, 0.415);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (25, 4, TIMESTAMP(CURDATE() - INTERVAL 9 DAY, '16:42:00'), 'TARDE', 7.0, 0.392);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (26, 5, TIMESTAMP(CURDATE() - INTERVAL 9 DAY, '16:46:00'), 'TARDE', 5.6, 0.421);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (27, 6, TIMESTAMP(CURDATE() - INTERVAL 9 DAY, '16:50:00'), 'TARDE', 9.2, 0.435);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (28, 7, TIMESTAMP(CURDATE() - INTERVAL 9 DAY, '16:54:00'), 'TARDE', 6.5, 0.381);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (29, 1, TIMESTAMP(CURDATE() - INTERVAL 8 DAY, '06:00:00'), 'MANHA', 8.0, 0.425);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (30, 2, TIMESTAMP(CURDATE() - INTERVAL 8 DAY, '06:04:00'), 'MANHA', 10.0, 0.405);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (31, 3, TIMESTAMP(CURDATE() - INTERVAL 8 DAY, '06:08:00'), 'MANHA', 7.0, 0.427);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (32, 4, TIMESTAMP(CURDATE() - INTERVAL 8 DAY, '06:12:00'), 'MANHA', 8.3, 0.382);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (33, 5, TIMESTAMP(CURDATE() - INTERVAL 8 DAY, '06:16:00'), 'MANHA', 6.2, 0.411);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (34, 6, TIMESTAMP(CURDATE() - INTERVAL 8 DAY, '06:20:00'), 'MANHA', 10.4, 0.431);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (35, 7, TIMESTAMP(CURDATE() - INTERVAL 8 DAY, '06:24:00'), 'MANHA', 7.2, 0.382);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (36, 1, TIMESTAMP(CURDATE() - INTERVAL 8 DAY, '16:30:00'), 'TARDE', 7.6, 0.407);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (37, 2, TIMESTAMP(CURDATE() - INTERVAL 8 DAY, '16:34:00'), 'TARDE', 8.1, 0.396);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (38, 3, TIMESTAMP(CURDATE() - INTERVAL 8 DAY, '16:38:00'), 'TARDE', 6.0, 0.423);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (39, 4, TIMESTAMP(CURDATE() - INTERVAL 8 DAY, '16:42:00'), 'TARDE', 7.6, 0.379);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (40, 5, TIMESTAMP(CURDATE() - INTERVAL 8 DAY, '16:46:00'), 'TARDE', 5.3, 0.413);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (41, 6, TIMESTAMP(CURDATE() - INTERVAL 8 DAY, '16:50:00'), 'TARDE', 8.0, 0.614);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (42, 7, TIMESTAMP(CURDATE() - INTERVAL 8 DAY, '16:54:00'), 'TARDE', 6.6, 0.382);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (43, 1, TIMESTAMP(CURDATE() - INTERVAL 7 DAY, '06:00:00'), 'MANHA', 7.8, 0.409);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (44, 2, TIMESTAMP(CURDATE() - INTERVAL 7 DAY, '06:04:00'), 'MANHA', 9.2, 0.403);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (45, 3, TIMESTAMP(CURDATE() - INTERVAL 7 DAY, '06:08:00'), 'MANHA', 6.4, 0.431);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (46, 4, TIMESTAMP(CURDATE() - INTERVAL 7 DAY, '06:12:00'), 'MANHA', 8.5, 0.401);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (47, 5, TIMESTAMP(CURDATE() - INTERVAL 7 DAY, '06:16:00'), 'MANHA', 6.0, 0.409);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (48, 6, TIMESTAMP(CURDATE() - INTERVAL 7 DAY, '06:20:00'), 'MANHA', 9.0, 0.606);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (49, 7, TIMESTAMP(CURDATE() - INTERVAL 7 DAY, '06:24:00'), 'MANHA', 7.8, 0.369);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (50, 1, TIMESTAMP(CURDATE() - INTERVAL 7 DAY, '16:30:00'), 'TARDE', 6.9, 0.441);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (51, 2, TIMESTAMP(CURDATE() - INTERVAL 7 DAY, '16:34:00'), 'TARDE', 8.8, 0.409);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (52, 3, TIMESTAMP(CURDATE() - INTERVAL 7 DAY, '16:38:00'), 'TARDE', 6.2, 0.417);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (53, 4, TIMESTAMP(CURDATE() - INTERVAL 7 DAY, '16:42:00'), 'TARDE', 7.0, 0.398);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (54, 5, TIMESTAMP(CURDATE() - INTERVAL 7 DAY, '16:46:00'), 'TARDE', 6.0, 0.405);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (55, 6, TIMESTAMP(CURDATE() - INTERVAL 7 DAY, '16:50:00'), 'TARDE', 7.6, 0.639);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (56, 7, TIMESTAMP(CURDATE() - INTERVAL 7 DAY, '16:54:00'), 'TARDE', 6.6, 0.392);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (57, 1, TIMESTAMP(CURDATE() - INTERVAL 6 DAY, '06:00:00'), 'MANHA', 8.3, 0.431);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (58, 2, TIMESTAMP(CURDATE() - INTERVAL 6 DAY, '06:04:00'), 'MANHA', 9.3, 0.405);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (59, 3, TIMESTAMP(CURDATE() - INTERVAL 6 DAY, '06:08:00'), 'MANHA', 6.9, 0.431);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (60, 4, TIMESTAMP(CURDATE() - INTERVAL 6 DAY, '06:12:00'), 'MANHA', 8.0, 0.379);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (61, 5, TIMESTAMP(CURDATE() - INTERVAL 6 DAY, '06:16:00'), 'MANHA', 6.7, 0.419);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (62, 6, TIMESTAMP(CURDATE() - INTERVAL 6 DAY, '06:20:00'), 'MANHA', 8.9, 0.538);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (63, 7, TIMESTAMP(CURDATE() - INTERVAL 6 DAY, '06:24:00'), 'MANHA', 7.4, 0.357);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (64, 1, TIMESTAMP(CURDATE() - INTERVAL 6 DAY, '16:30:00'), 'TARDE', 6.9, 0.407);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (65, 2, TIMESTAMP(CURDATE() - INTERVAL 6 DAY, '16:34:00'), 'TARDE', 8.6, 0.407);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (66, 3, TIMESTAMP(CURDATE() - INTERVAL 6 DAY, '16:38:00'), 'TARDE', 5.8, 0.421);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (67, 4, TIMESTAMP(CURDATE() - INTERVAL 6 DAY, '16:42:00'), 'TARDE', 7.8, 0.386);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (68, 5, TIMESTAMP(CURDATE() - INTERVAL 6 DAY, '16:46:00'), 'TARDE', 5.6, 0.405);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (69, 6, TIMESTAMP(CURDATE() - INTERVAL 6 DAY, '16:50:00'), 'TARDE', 8.3, 0.500);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (70, 7, TIMESTAMP(CURDATE() - INTERVAL 6 DAY, '16:54:00'), 'TARDE', 7.0, 0.375);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (71, 1, TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '06:00:00'), 'MANHA', 7.8, 0.421);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (72, 2, TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '06:04:00'), 'MANHA', 9.2, 0.379);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (73, 3, TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '06:08:00'), 'MANHA', 7.0, 0.407);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (74, 4, TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '06:12:00'), 'MANHA', 8.4, 0.399);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (75, 5, TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '06:16:00'), 'MANHA', 6.4, 0.398);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (76, 6, TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '06:20:00'), 'MANHA', 9.6, 0.464);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (77, 7, TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '06:24:00'), 'MANHA', 7.8, 0.392);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (78, 1, TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '16:30:00'), 'TARDE', 7.2, 0.423);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (79, 2, TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '16:34:00'), 'TARDE', 8.2, 0.413);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (80, 3, TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '16:38:00'), 'TARDE', 6.3, 0.433);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (81, 4, TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '16:42:00'), 'TARDE', 7.4, 0.398);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (82, 5, TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '16:46:00'), 'TARDE', 5.6, 0.415);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (83, 6, TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '16:50:00'), 'TARDE', 9.0, 0.445);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (84, 7, TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '16:54:00'), 'TARDE', 6.6, 0.375);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (85, 1, TIMESTAMP(CURDATE() - INTERVAL 4 DAY, '06:00:00'), 'MANHA', 7.7, 0.419);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (86, 2, TIMESTAMP(CURDATE() - INTERVAL 4 DAY, '06:04:00'), 'MANHA', 9.3, 0.411);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (87, 3, TIMESTAMP(CURDATE() - INTERVAL 4 DAY, '06:08:00'), 'MANHA', 6.9, 0.427);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (88, 4, TIMESTAMP(CURDATE() - INTERVAL 4 DAY, '06:12:00'), 'MANHA', 8.3, 0.394);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (89, 5, TIMESTAMP(CURDATE() - INTERVAL 4 DAY, '06:16:00'), 'MANHA', 6.4, 0.409);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (90, 6, TIMESTAMP(CURDATE() - INTERVAL 4 DAY, '06:20:00'), 'MANHA', 9.5, 0.435);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (91, 7, TIMESTAMP(CURDATE() - INTERVAL 4 DAY, '06:24:00'), 'MANHA', 7.1, 0.470);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (92, 1, TIMESTAMP(CURDATE() - INTERVAL 4 DAY, '16:30:00'), 'TARDE', 7.2, 0.435);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (93, 2, TIMESTAMP(CURDATE() - INTERVAL 4 DAY, '16:34:00'), 'TARDE', 8.6, 0.396);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (94, 3, TIMESTAMP(CURDATE() - INTERVAL 4 DAY, '16:38:00'), 'TARDE', 5.8, 0.427);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (95, 4, TIMESTAMP(CURDATE() - INTERVAL 4 DAY, '16:42:00'), 'TARDE', 7.0, 0.388);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (96, 5, TIMESTAMP(CURDATE() - INTERVAL 4 DAY, '16:46:00'), 'TARDE', 5.8, 0.427);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (97, 6, TIMESTAMP(CURDATE() - INTERVAL 4 DAY, '16:50:00'), 'TARDE', 8.7, 0.447);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (98, 7, TIMESTAMP(CURDATE() - INTERVAL 4 DAY, '16:54:00'), 'TARDE', 6.5, 0.382);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (99, 1, TIMESTAMP(CURDATE() - INTERVAL 3 DAY, '06:00:00'), 'MANHA', 7.9, 0.429);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (100, 2, TIMESTAMP(CURDATE() - INTERVAL 3 DAY, '06:04:00'), 'MANHA', 9.5, 0.411);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (101, 3, TIMESTAMP(CURDATE() - INTERVAL 3 DAY, '06:08:00'), 'MANHA', 6.7, 0.441);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (102, 4, TIMESTAMP(CURDATE() - INTERVAL 3 DAY, '06:12:00'), 'MANHA', 8.3, 0.392);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (103, 5, TIMESTAMP(CURDATE() - INTERVAL 3 DAY, '06:16:00'), 'MANHA', 6.3, 0.411);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (104, 6, TIMESTAMP(CURDATE() - INTERVAL 3 DAY, '06:20:00'), 'MANHA', 10.2, 0.421);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (105, 7, TIMESTAMP(CURDATE() - INTERVAL 3 DAY, '06:24:00'), 'MANHA', 7.6, 0.364);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (106, 1, TIMESTAMP(CURDATE() - INTERVAL 3 DAY, '16:30:00'), 'TARDE', 7.1, 0.445);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (107, 2, TIMESTAMP(CURDATE() - INTERVAL 3 DAY, '16:34:00'), 'TARDE', 8.7, 0.390);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (108, 3, TIMESTAMP(CURDATE() - INTERVAL 3 DAY, '16:38:00'), 'TARDE', 5.5, 0.485);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (109, 4, TIMESTAMP(CURDATE() - INTERVAL 3 DAY, '16:42:00'), 'TARDE', 7.7, 0.379);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (110, 5, TIMESTAMP(CURDATE() - INTERVAL 3 DAY, '16:46:00'), 'TARDE', 5.8, 0.407);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (111, 6, TIMESTAMP(CURDATE() - INTERVAL 3 DAY, '16:50:00'), 'TARDE', 9.1, 0.451);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (112, 7, TIMESTAMP(CURDATE() - INTERVAL 3 DAY, '16:54:00'), 'TARDE', 6.4, 0.375);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (113, 1, TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '06:00:00'), 'MANHA', 8.2, 0.431);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (114, 2, TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '06:04:00'), 'MANHA', 9.2, 0.396);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (115, 3, TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '06:08:00'), 'MANHA', 6.4, 0.536);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (116, 4, TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '06:12:00'), 'MANHA', 6.2, 0.403);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (117, 5, TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '06:16:00'), 'MANHA', 6.5, 0.415);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (118, 6, TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '06:20:00'), 'MANHA', 10.2, 0.439);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (119, 7, TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '06:24:00'), 'MANHA', 7.3, 0.366);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (120, 1, TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '16:30:00'), 'TARDE', 7.0, 0.413);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (121, 2, TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '16:34:00'), 'TARDE', 8.3, 0.392);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (122, 3, TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '16:38:00'), 'TARDE', 5.4, 0.576);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (123, 4, TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '16:42:00'), 'TARDE', 5.4, 0.398);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (124, 5, TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '16:46:00'), 'TARDE', 5.6, 0.413);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (125, 6, TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '16:50:00'), 'TARDE', 8.4, 0.453);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (126, 7, TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '16:54:00'), 'TARDE', 6.6, 0.369);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (127, 1, TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '06:00:00'), 'MANHA', 8.1, 0.421);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (128, 2, TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '06:04:00'), 'MANHA', 9.7, 0.401);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (129, 3, TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '06:08:00'), 'MANHA', 6.0, 0.555);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (130, 4, TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '06:12:00'), 'MANHA', 6.2, 0.386);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (131, 5, TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '06:16:00'), 'MANHA', 6.4, 0.396);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (132, 6, TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '06:20:00'), 'MANHA', 9.8, 0.427);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (133, 7, TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '06:24:00'), 'MANHA', 7.6, 0.377);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (134, 1, TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '16:30:00'), 'TARDE', 7.1, 0.431);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (135, 2, TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '16:34:00'), 'TARDE', 9.0, 0.398);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (136, 3, TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '16:38:00'), 'TARDE', 5.2, 0.567);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (137, 4, TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '16:42:00'), 'TARDE', 5.1, 0.369);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (138, 5, TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '16:46:00'), 'TARDE', 5.3, 0.427);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (139, 6, TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '16:50:00'), 'TARDE', 8.7, 0.453);
INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES (140, 7, TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '16:54:00'), 'TARDE', 6.5, 0.381);

-- ALERTA (resolvido: 0 = aberto, 1 = resolvido pelo produtor)
INSERT INTO alerta (id, vaca_id, ordenha_id, tipo, mensagem, data_hora, resolvido) VALUES (1, 6, 41, 'SUSPEITA_MASTITE', 'Suspeita de mastite: faça o teste da caneca ou o CMT', TIMESTAMP(CURDATE() - INTERVAL 8 DAY, '16:50:00'), 1);
INSERT INTO alerta (id, vaca_id, ordenha_id, tipo, mensagem, data_hora, resolvido) VALUES (2, 6, 48, 'SUSPEITA_MASTITE', 'Suspeita de mastite: faça o teste da caneca ou o CMT', TIMESTAMP(CURDATE() - INTERVAL 7 DAY, '06:20:00'), 1);
INSERT INTO alerta (id, vaca_id, ordenha_id, tipo, mensagem, data_hora, resolvido) VALUES (3, 6, 55, 'SUSPEITA_MASTITE', 'Suspeita de mastite: faça o teste da caneca ou o CMT', TIMESTAMP(CURDATE() - INTERVAL 7 DAY, '16:50:00'), 1);
INSERT INTO alerta (id, vaca_id, ordenha_id, tipo, mensagem, data_hora, resolvido) VALUES (4, 6, 62, 'LEITE_EM_CARENCIA', 'Não enviar este leite ao tanque', TIMESTAMP(CURDATE() - INTERVAL 6 DAY, '06:20:00'), 1);
INSERT INTO alerta (id, vaca_id, ordenha_id, tipo, mensagem, data_hora, resolvido) VALUES (5, 6, 69, 'LEITE_EM_CARENCIA', 'Não enviar este leite ao tanque', TIMESTAMP(CURDATE() - INTERVAL 6 DAY, '16:50:00'), 1);
INSERT INTO alerta (id, vaca_id, ordenha_id, tipo, mensagem, data_hora, resolvido) VALUES (6, 6, 76, 'LEITE_EM_CARENCIA', 'Não enviar este leite ao tanque', TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '06:20:00'), 1);
INSERT INTO alerta (id, vaca_id, ordenha_id, tipo, mensagem, data_hora, resolvido) VALUES (7, 6, 83, 'LEITE_EM_CARENCIA', 'Não enviar este leite ao tanque', TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '16:50:00'), 1);
INSERT INTO alerta (id, vaca_id, ordenha_id, tipo, mensagem, data_hora, resolvido) VALUES (8, 6, 90, 'LEITE_EM_CARENCIA', 'Não enviar este leite ao tanque', TIMESTAMP(CURDATE() - INTERVAL 4 DAY, '06:20:00'), 1);
INSERT INTO alerta (id, vaca_id, ordenha_id, tipo, mensagem, data_hora, resolvido) VALUES (9, 7, 91, 'SUSPEITA_MASTITE', 'Suspeita de mastite: faça o teste da caneca ou o CMT', TIMESTAMP(CURDATE() - INTERVAL 4 DAY, '06:24:00'), 1);
INSERT INTO alerta (id, vaca_id, ordenha_id, tipo, mensagem, data_hora, resolvido) VALUES (10, 6, 97, 'LEITE_EM_CARENCIA', 'Não enviar este leite ao tanque', TIMESTAMP(CURDATE() - INTERVAL 4 DAY, '16:50:00'), 1);
INSERT INTO alerta (id, vaca_id, ordenha_id, tipo, mensagem, data_hora, resolvido) VALUES (11, 6, 104, 'LEITE_EM_CARENCIA', 'Não enviar este leite ao tanque', TIMESTAMP(CURDATE() - INTERVAL 3 DAY, '06:20:00'), 1);
INSERT INTO alerta (id, vaca_id, ordenha_id, tipo, mensagem, data_hora, resolvido) VALUES (12, 6, 111, 'LEITE_EM_CARENCIA', 'Não enviar este leite ao tanque', TIMESTAMP(CURDATE() - INTERVAL 3 DAY, '16:50:00'), 1);
INSERT INTO alerta (id, vaca_id, ordenha_id, tipo, mensagem, data_hora, resolvido) VALUES (13, 3, 115, 'SUSPEITA_MASTITE', 'Suspeita de mastite: faça o teste da caneca ou o CMT', TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '06:08:00'), 0);
INSERT INTO alerta (id, vaca_id, ordenha_id, tipo, mensagem, data_hora, resolvido) VALUES (14, 6, 118, 'LEITE_EM_CARENCIA', 'Não enviar este leite ao tanque', TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '06:20:00'), 1);
INSERT INTO alerta (id, vaca_id, ordenha_id, tipo, mensagem, data_hora, resolvido) VALUES (15, 3, 122, 'SUSPEITA_MASTITE', 'Suspeita de mastite: faça o teste da caneca ou o CMT', TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '16:38:00'), 0);
INSERT INTO alerta (id, vaca_id, ordenha_id, tipo, mensagem, data_hora, resolvido) VALUES (16, 6, 125, 'LEITE_EM_CARENCIA', 'Não enviar este leite ao tanque', TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '16:50:00'), 1);
INSERT INTO alerta (id, vaca_id, ordenha_id, tipo, mensagem, data_hora, resolvido) VALUES (17, 3, 129, 'SUSPEITA_MASTITE', 'Suspeita de mastite: faça o teste da caneca ou o CMT', TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '06:08:00'), 0);
INSERT INTO alerta (id, vaca_id, ordenha_id, tipo, mensagem, data_hora, resolvido) VALUES (18, 4, 130, 'QUEDA_PRODUCAO', 'Produção em queda', TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '06:12:00'), 0);
INSERT INTO alerta (id, vaca_id, ordenha_id, tipo, mensagem, data_hora, resolvido) VALUES (19, 6, 132, 'LEITE_EM_CARENCIA', 'Não enviar este leite ao tanque', TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '06:20:00'), 0);
INSERT INTO alerta (id, vaca_id, ordenha_id, tipo, mensagem, data_hora, resolvido) VALUES (20, 3, 136, 'SUSPEITA_MASTITE', 'Suspeita de mastite: faça o teste da caneca ou o CMT', TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '16:38:00'), 0);
INSERT INTO alerta (id, vaca_id, ordenha_id, tipo, mensagem, data_hora, resolvido) VALUES (21, 6, 139, 'LEITE_EM_CARENCIA', 'Não enviar este leite ao tanque', TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '16:50:00'), 0);

COMMIT;

-- Os IDs acima foram informados à mão. No MySQL, o AUTO_INCREMENT de cada
-- tabela passa sozinho para o maior ID + 1, então o próximo INSERT feito
-- pela API (sem id) não colide com a carga. Conferência:
SELECT 'usuario' AS tabela, COUNT(*) AS linhas FROM usuario
UNION ALL SELECT 'propriedade', COUNT(*) FROM propriedade
UNION ALL SELECT 'vaca', COUNT(*) FROM vaca
UNION ALL SELECT 'tratamento', COUNT(*) FROM tratamento
UNION ALL SELECT 'ordenha', COUNT(*) FROM ordenha
UNION ALL SELECT 'alerta', COUNT(*) FROM alerta;
