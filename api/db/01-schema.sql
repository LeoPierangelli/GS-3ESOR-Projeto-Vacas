-- CÓPIA de banco/01_criar_tabelas.sql feita por ferramentas/gerar_dados.py.
-- O original (e oficial) fica na pasta banco/. Não edite esta cópia.
-- =============================================================================
-- ORDENHA MONITORADA · Global Solution 2026/2 · Parte 1 (Banco de dados)
-- Script 01 de 03: criação do banco e das tabelas (MySQL 8.0.16 ou superior)
--
-- Como rodar: MySQL Workbench > File > Open SQL Script > este arquivo >
-- botão do raio "Execute" (Ctrl+Shift+Enter), que executa o script inteiro.
-- Pode ser executado várias vezes: o banco ordenha é criado se não
-- existir, e as 6 tabelas são apagadas e recriadas.
-- É o mesmo script que o Docker da API usa (cópia em api/db/01-schema.sql).
-- O banco se chama ordenha, o mesmo nome que a API usa (application.properties).
-- Se você não tiver permissão para criar banco, apague as linhas
-- CREATE DATABASE e USE e selecione antes o banco que for usar.
--
-- Convenções combinadas com o grupo:
--   * Banco em snake_case (vaca_id, data_hora); JSON da API/app em camelCase.
--   * IDs BIGINT AUTO_INCREMENT (equivalem a Long no Java e a long no C#).
--   * Condutividade em mS (milisiemens), na escala do protótipo MastitAlert
--     (Portugal & Guedes, 2021): vaca sadia ~0,4 a 0,5 mS; limite 0,537 mS.
--   * Booleano: TINYINT(1) com 0 = não, 1 = sim.
-- Os CHECKs só são aplicados a partir do MySQL 8.0.16.
-- =============================================================================

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS ordenha CHARACTER SET utf8mb4;
USE ordenha;

-- Filhas antes das mães, por causa das chaves estrangeiras.
DROP TABLE IF EXISTS alerta, ordenha, tratamento, vaca, propriedade, usuario;

-- -----------------------------------------------------------------------------
-- USUARIO: produtor que acessa o app. A senha é guardada só como hash BCrypt.
-- -----------------------------------------------------------------------------
CREATE TABLE usuario (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    nome        VARCHAR(100)  NOT NULL,
    email       VARCHAR(150)  NOT NULL,
    senha_hash  VARCHAR(100)  NOT NULL COMMENT 'Hash BCrypt da senha, nunca texto puro',
    CONSTRAINT pk_usuario        PRIMARY KEY (id),
    CONSTRAINT uk_usuario_email  UNIQUE (email),
    CONSTRAINT ck_usuario_email  CHECK (email LIKE '_%@_%'),
    CONSTRAINT ck_usuario_hash   CHECK (CHAR_LENGTH(senha_hash) >= 50)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Produtor que acessa o app';

-- -----------------------------------------------------------------------------
-- PROPRIEDADE: sítio/fazenda do produtor (um usuário pode ter mais de uma).
-- -----------------------------------------------------------------------------
CREATE TABLE propriedade (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    usuario_id  BIGINT        NOT NULL,
    nome        VARCHAR(100)  NOT NULL,
    municipio   VARCHAR(100)  NOT NULL,
    uf          CHAR(2)       NOT NULL,
    CONSTRAINT pk_propriedade         PRIMARY KEY (id),
    CONSTRAINT fk_propriedade_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT ck_propriedade_uf      CHECK (CHAR_LENGTH(uf) = 2)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Sítio ou fazenda do produtor';

-- -----------------------------------------------------------------------------
-- VACA: animal do rebanho, identificado pelo brinco dentro da propriedade.
-- O status "suspeita" mostrado no app NÃO é guardado aqui: ele é derivado
-- de existir alerta aberto para a vaca.
-- -----------------------------------------------------------------------------
CREATE TABLE vaca (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    propriedade_id   BIGINT        NOT NULL,
    brinco           VARCHAR(20)   NOT NULL,
    nome             VARCHAR(60),
    data_nascimento  DATE,
    status           VARCHAR(20)   NOT NULL DEFAULT 'ATIVA' COMMENT 'ATIVA, EM_TRATAMENTO ou SECA',
    CONSTRAINT pk_vaca             PRIMARY KEY (id),
    CONSTRAINT fk_vaca_propriedade FOREIGN KEY (propriedade_id) REFERENCES propriedade (id),
    CONSTRAINT uk_vaca_brinco      UNIQUE (propriedade_id, brinco),
    CONSTRAINT ck_vaca_status      CHECK (status IN ('ATIVA', 'EM_TRATAMENTO', 'SECA'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Animal do rebanho';

-- -----------------------------------------------------------------------------
-- ORDENHA: uma leitura dos dois sensores (volume e condutividade) por vaca e
-- por ordenha. É o registro que o simulador de IoT envia para POST /ordenhas.
-- -----------------------------------------------------------------------------
CREATE TABLE ordenha (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    vaca_id        BIGINT        NOT NULL,
    data_hora      DATETIME      NOT NULL,
    turno          VARCHAR(10)   NOT NULL,
    litros         DECIMAL(5,2)  NOT NULL COMMENT 'Medidor de volume, em litros',
    condutividade  DECIMAL(6,3)  NOT NULL COMMENT 'Sensor de condutividade, em mS (escala MastitAlert)',
    CONSTRAINT pk_ordenha               PRIMARY KEY (id),
    CONSTRAINT fk_ordenha_vaca          FOREIGN KEY (vaca_id) REFERENCES vaca (id) ON DELETE CASCADE,
    CONSTRAINT uk_ordenha_vaca_momento  UNIQUE (vaca_id, data_hora),
    CONSTRAINT uk_ordenha_id_vaca       UNIQUE (id, vaca_id),  -- alvo da FK composta de alerta
    CONSTRAINT ck_ordenha_turno         CHECK (turno IN ('MANHA', 'TARDE')),
    CONSTRAINT ck_ordenha_litros        CHECK (litros > 0 AND litros < 1000),  -- mesmo limite da API (999.99)
    CONSTRAINT ck_ordenha_condutividade CHECK (condutividade > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Leitura dos sensores de uma vaca em uma ordenha';

-- -----------------------------------------------------------------------------
-- ALERTA: gerado quando uma ordenha dispara uma das três regras.
-- vaca_id repete a vaca da ordenha porque a API busca alertas direto pela vaca
-- (lista do rebanho, contador de alertas abertos). Para essa repetição nunca
-- ficar inconsistente, a FK é COMPOSTA: (ordenha_id, vaca_id) precisa existir
-- em ordenha (id, vaca_id), ou seja, o alerta sempre aponta para a vaca da
-- própria ordenha.
-- -----------------------------------------------------------------------------
CREATE TABLE alerta (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    vaca_id     BIGINT        NOT NULL,
    ordenha_id  BIGINT        NOT NULL,
    tipo        VARCHAR(30)   NOT NULL,
    mensagem    VARCHAR(200)  NOT NULL,
    data_hora   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolvido   TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '0 = aberto, 1 = resolvido pelo produtor',
    CONSTRAINT pk_alerta              PRIMARY KEY (id),
    CONSTRAINT fk_alerta_ordenha      FOREIGN KEY (ordenha_id, vaca_id)
                                      REFERENCES ordenha (id, vaca_id) ON DELETE CASCADE,
    CONSTRAINT uk_alerta_ordenha_tipo UNIQUE (ordenha_id, tipo),
    CONSTRAINT ck_alerta_tipo         CHECK (tipo IN ('SUSPEITA_MASTITE', 'QUEDA_PRODUCAO', 'LEITE_EM_CARENCIA')),
    CONSTRAINT ck_alerta_resolvido    CHECK (resolvido IN (0, 1)),
    INDEX ix_alerta_vaca_aberto (vaca_id, resolvido, tipo)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Alerta gerado pelas regras ao gravar uma ordenha';

-- -----------------------------------------------------------------------------
-- TRATAMENTO: medicamento aplicado na vaca. Enquanto a data da ordenha estiver
-- entre data_inicio e data_fim + dias_carencia (inclusive), o leite não pode ir
-- ao tanque. data_fim fica NULL enquanto o tratamento não terminou: nesse caso
-- o leite segue em carência.
-- -----------------------------------------------------------------------------
CREATE TABLE tratamento (
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    vaca_id        BIGINT         NOT NULL,
    medicamento    VARCHAR(100)   NOT NULL,
    data_inicio    DATE           NOT NULL,
    data_fim       DATE,
    dias_carencia  SMALLINT       NOT NULL COMMENT 'Dias após data_fim em que o leite ainda não pode ir ao tanque',
    observacao     VARCHAR(255),
    CONSTRAINT pk_tratamento          PRIMARY KEY (id),
    CONSTRAINT fk_tratamento_vaca     FOREIGN KEY (vaca_id) REFERENCES vaca (id) ON DELETE CASCADE,
    CONSTRAINT ck_tratamento_periodo  CHECK (data_fim IS NULL OR data_fim >= data_inicio),
    CONSTRAINT ck_tratamento_carencia CHECK (dias_carencia BETWEEN 0 AND 999)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Tratamento com medicamento e período de carência do leite';

-- Índice para "tratamentos da vaca X por data" (regra de carência e consultas).
-- As demais FKs já são cobertas pelos UNIQUE ou pelos índices que o InnoDB cria.
CREATE INDEX ix_tratamento_vaca ON tratamento (vaca_id, data_inicio);
