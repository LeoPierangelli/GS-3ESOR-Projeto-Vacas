-- =============================================================================
-- ORDENHA MONITORADA · Parte 1 (Banco de dados)
-- Script 03 de 03: consultas de uso real (MySQL 8.0). Rode depois do 01 e do 02.
-- Todas usam JOIN e agregação. As datas são relativas a hoje (CURDATE()),
-- como a carga inicial, então sempre retornam dados.
-- No Workbench: Ctrl+Shift+Enter roda tudo (uma aba de resultado por consulta);
-- Ctrl+Enter roda só a consulta onde está o cursor.
-- =============================================================================

USE ordenha;


-- -----------------------------------------------------------------------------
-- CONSULTA 1 · Ranking de produção por vaca nos últimos 7 dias
-- Uso: o produtor vê quais vacas mais produzem em cada propriedade.
-- Recursos: JOIN de 3 tabelas, COUNT, SUM, AVG, GROUP BY, RANK() por propriedade.
-- Esperado na carga: no Sítio Boa Vista, a BR-003 (Mimosa, mastite) e a BR-004
-- (Estrela, queda de produção) ficam por último.
-- -----------------------------------------------------------------------------
SELECT p.nome                                   AS propriedade,
       RANK() OVER (PARTITION BY p.id
                    ORDER BY SUM(o.litros) DESC) AS posicao,
       v.brinco,
       v.nome                                   AS vaca,
       COUNT(o.id)                              AS ordenhas,
       SUM(o.litros)                            AS litros_7_dias,
       ROUND(AVG(o.litros), 2)                  AS media_por_ordenha
  FROM propriedade p
  JOIN vaca    v ON v.propriedade_id = p.id
  JOIN ordenha o ON o.vaca_id = v.id
 WHERE o.data_hora >= CURDATE() - INTERVAL 7 DAY
 GROUP BY p.id, p.nome, v.id, v.brinco, v.nome
 ORDER BY p.nome, posicao;


-- -----------------------------------------------------------------------------
-- CONSULTA 2 · Última condutividade de cada vaca ao lado da média dela
-- Uso: é a mesma conta da regra de mastite (última leitura contra a média das
-- 7 ordenhas anteriores + 15%) e mostra também o alarme do aparelho
-- (limite fixo de 0,537 mS do MastitAlert).
-- Recursos: CTE, ROW_NUMBER(), JOIN, AVG e MAX condicionais, GROUP BY, CASE.
-- Esperado na carga: a BR-003 (Mimosa) aparece no topo, com SUSPEITA e ALARME.
-- -----------------------------------------------------------------------------
WITH ordenadas AS (
    SELECT o.vaca_id,
           o.condutividade,
           ROW_NUMBER() OVER (PARTITION BY o.vaca_id ORDER BY o.data_hora DESC) AS n
      FROM ordenha o
),
resumo AS (
    SELECT vaca_id,
           MAX(CASE WHEN n = 1 THEN condutividade END)            AS ultima_ms,
           AVG(CASE WHEN n BETWEEN 2 AND 8 THEN condutividade END) AS media_ms
      FROM ordenadas
     GROUP BY vaca_id
)
SELECT v.brinco,
       v.nome                                   AS vaca,
       r.ultima_ms,
       ROUND(r.media_ms, 3)                     AS media_7_anteriores_ms,
       ROUND((r.ultima_ms / r.media_ms - 1) * 100, 1) AS variacao_pct,
       CASE WHEN r.ultima_ms > r.media_ms * 1.15
            THEN 'SUSPEITA' ELSE 'NORMAL' END    AS regra_do_sistema,
       CASE WHEN r.ultima_ms >= 0.537
            THEN 'ALARME' ELSE '-' END           AS alarme_do_aparelho
  FROM vaca v
  JOIN resumo r ON r.vaca_id = v.id
 ORDER BY variacao_pct DESC;


-- -----------------------------------------------------------------------------
-- CONSULTA 3 · Alertas abertos por propriedade e por tipo
-- Uso: o contador de alertas do app e a fila de vacas para examinar.
-- Recursos: JOIN de 4 tabelas (alerta -> ordenha -> vaca -> propriedade),
-- COUNT, COUNT(DISTINCT), MIN, MAX, GROUP_CONCAT, GROUP BY.
-- Esperado na carga: Sítio Boa Vista com alertas de mastite (BR-003) e de queda
-- (BR-004); Fazendinha Santa Luzia com leite em carência (SL-010).
-- -----------------------------------------------------------------------------
SELECT p.nome                                   AS propriedade,
       a.tipo,
       COUNT(*)                                 AS alertas_abertos,
       COUNT(DISTINCT a.vaca_id)                AS vacas_afetadas,
       MIN(a.data_hora)                         AS mais_antigo,
       MAX(o.condutividade)                     AS maior_condutividade_ms,
       GROUP_CONCAT(DISTINCT v.brinco ORDER BY v.brinco SEPARATOR ', ') AS vacas
  FROM alerta a
  JOIN ordenha     o ON o.id = a.ordenha_id AND o.vaca_id = a.vaca_id
  JOIN vaca        v ON v.id = a.vaca_id
  JOIN propriedade p ON p.id = v.propriedade_id
 WHERE a.resolvido = 0
 GROUP BY p.nome, a.tipo
 ORDER BY p.nome, alertas_abertos DESC, a.tipo;


-- -----------------------------------------------------------------------------
-- CONSULTA 4 · Vacas em carência hoje e quando o leite volta para o tanque
-- Uso: lista o que o produtor NÃO pode mandar para o laticínio hoje.
-- Regra (a mesma da API): em carência de data_inicio até data_fim +
-- dias_carencia, inclusive; sem data_fim, o tratamento não terminou e a
-- carência continua (a data de volta ao tanque fica em aberto).
-- Recursos: JOIN de 3 tabelas, INTERVAL/DATEDIFF, MAX, SUM, COUNT,
-- GROUP_CONCAT, GROUP BY (a vaca pode ter mais de um tratamento ativo).
-- Esperado na carga: a SL-010 (Pintada); o leite volta ao tanque em 3 dias.
-- -----------------------------------------------------------------------------
SELECT v.brinco,
       v.nome                                         AS vaca,
       p.nome                                         AS propriedade,
       COUNT(t.id)                                    AS tratamentos_ativos,
       GROUP_CONCAT(t.medicamento ORDER BY t.data_inicio SEPARATOR ', ') AS medicamentos,
       CASE WHEN SUM(t.data_fim IS NULL) > 0 THEN NULL
            ELSE MAX(t.data_fim + INTERVAL t.dias_carencia DAY) END AS ultimo_dia_carencia,
       CASE WHEN SUM(t.data_fim IS NULL) > 0 THEN NULL
            ELSE MAX(t.data_fim + INTERVAL t.dias_carencia DAY) + INTERVAL 1 DAY
       END                                            AS leite_volta_ao_tanque_em,
       CASE WHEN SUM(t.data_fim IS NULL) > 0 THEN NULL
            ELSE DATEDIFF(MAX(t.data_fim + INTERVAL t.dias_carencia DAY) + INTERVAL 1 DAY, CURDATE())
       END                                            AS dias_restantes
  FROM vaca v
  JOIN propriedade p ON p.id = v.propriedade_id
  JOIN tratamento  t ON t.vaca_id = v.id
 WHERE CURDATE() >= t.data_inicio
   AND (t.data_fim IS NULL OR CURDATE() <= t.data_fim + INTERVAL t.dias_carencia DAY)
 GROUP BY v.id, v.brinco, v.nome, p.nome
 ORDER BY leite_volta_ao_tanque_em;


-- -----------------------------------------------------------------------------
-- CONSULTA 5 · Produção da propriedade por dia, separando o leite descartado
-- Uso: quanto leite cada propriedade tirou por dia e quanto pôde ir para o
-- tanque (o de vaca em carência é descartado). Mostra o custo da mastite.
-- Recursos: CTE com EXISTS, JOIN de 3 tabelas, SUM e COUNT(DISTINCT),
-- SUM condicional, GROUP BY por dia (DATE da data e hora).
-- Esperado na carga: na Fazendinha Santa Luzia, do dia -6 em diante aparece
-- leite descartado (o da SL-010 em tratamento).
-- -----------------------------------------------------------------------------
WITH ordenha_marcada AS (
    SELECT o.vaca_id,
           DATE(o.data_hora) AS dia,
           o.turno,
           o.litros,
           CASE WHEN EXISTS (SELECT 1
                               FROM tratamento t
                              WHERE t.vaca_id = o.vaca_id
                                AND DATE(o.data_hora) >= t.data_inicio
                                AND (t.data_fim IS NULL OR DATE(o.data_hora)
                                     <= t.data_fim + INTERVAL t.dias_carencia DAY))
                THEN 1 ELSE 0
           END AS em_carencia
      FROM ordenha o
)
SELECT p.nome                                                     AS propriedade,
       x.dia,
       COUNT(DISTINCT x.vaca_id)                                  AS vacas_ordenhadas,
       SUM(CASE WHEN x.turno = 'MANHA' THEN x.litros ELSE 0 END)  AS litros_manha,
       SUM(CASE WHEN x.turno = 'TARDE' THEN x.litros ELSE 0 END)  AS litros_tarde,
       SUM(x.litros)                                              AS litros_total,
       SUM(CASE WHEN x.em_carencia = 1 THEN x.litros ELSE 0 END)  AS litros_descartados,
       SUM(CASE WHEN x.em_carencia = 0 THEN x.litros ELSE 0 END)  AS litros_para_tanque
  FROM ordenha_marcada x
  JOIN vaca        v ON v.id = x.vaca_id
  JOIN propriedade p ON p.id = v.propriedade_id
 GROUP BY p.nome, x.dia
 ORDER BY p.nome, x.dia;


-- -----------------------------------------------------------------------------
-- CONSULTA 6 · Vacas com mais de um tratamento nos últimos 12 meses
-- Uso: vacas com problema recorrente, candidatas a avaliação do veterinário.
-- Recursos: JOIN, COUNT, MIN, MAX, SUM com DATEDIFF, GROUP_CONCAT, GROUP BY,
-- HAVING.
-- Esperado na carga: a SL-010 (Pintada), com 2 tratamentos.
-- -----------------------------------------------------------------------------
SELECT v.brinco,
       v.nome                                         AS vaca,
       p.nome                                         AS propriedade,
       COUNT(t.id)                                    AS tratamentos,
       MIN(t.data_inicio)                             AS primeiro,
       MAX(t.data_inicio)                             AS ultimo,
       SUM(DATEDIFF(COALESCE(t.data_fim, CURDATE()), t.data_inicio) + 1) AS dias_em_tratamento,
       GROUP_CONCAT(t.medicamento ORDER BY t.data_inicio SEPARATOR ' | ') AS historico
  FROM vaca v
  JOIN propriedade p ON p.id = v.propriedade_id
  JOIN tratamento  t ON t.vaca_id = v.id
 WHERE t.data_inicio >= CURDATE() - INTERVAL 12 MONTH
 GROUP BY v.id, v.brinco, v.nome, p.nome
HAVING COUNT(t.id) > 1
 ORDER BY tratamentos DESC;
