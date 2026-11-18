-- =============================================================
-- Ordenha Monitorada - schema MySQL (PROVISÓRIO)
-- Usado pela API para rodar localmente. O script oficial do banco
-- é o da parte 1; os nomes de tabelas e colunas seguem o planejamento.
-- =============================================================
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS usuario (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    nome        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL,
    senha_hash  VARCHAR(100) NOT NULL,
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uk_usuario_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS propriedade (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    usuario_id  BIGINT       NOT NULL,
    nome        VARCHAR(100) NOT NULL,
    municipio   VARCHAR(100) NOT NULL,
    uf          CHAR(2)      NOT NULL,
    CONSTRAINT pk_propriedade PRIMARY KEY (id),
    CONSTRAINT fk_propriedade_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);

CREATE TABLE IF NOT EXISTS vaca (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    propriedade_id   BIGINT      NOT NULL,
    brinco           VARCHAR(20) NOT NULL,
    nome             VARCHAR(60),
    data_nascimento  DATE,
    status           VARCHAR(20) NOT NULL DEFAULT 'ATIVA',
    CONSTRAINT pk_vaca PRIMARY KEY (id),
    CONSTRAINT fk_vaca_propriedade FOREIGN KEY (propriedade_id) REFERENCES propriedade (id),
    CONSTRAINT uk_vaca_brinco UNIQUE (propriedade_id, brinco),
    CONSTRAINT ck_vaca_status CHECK (status IN ('ATIVA', 'EM_TRATAMENTO', 'SECA'))
);

CREATE TABLE IF NOT EXISTS ordenha (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    vaca_id        BIGINT       NOT NULL,
    data_hora      DATETIME     NOT NULL,
    turno          VARCHAR(10)  NOT NULL,
    litros         DECIMAL(5,2) NOT NULL,
    condutividade  DECIMAL(5,2) NOT NULL,
    CONSTRAINT pk_ordenha PRIMARY KEY (id),
    CONSTRAINT fk_ordenha_vaca FOREIGN KEY (vaca_id) REFERENCES vaca (id) ON DELETE CASCADE,
    CONSTRAINT ck_ordenha_turno CHECK (turno IN ('MANHA', 'TARDE')),
    CONSTRAINT ck_ordenha_litros CHECK (litros > 0),
    CONSTRAINT ck_ordenha_condutividade CHECK (condutividade > 0)
);

CREATE TABLE IF NOT EXISTS alerta (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    vaca_id     BIGINT       NOT NULL,
    ordenha_id  BIGINT       NOT NULL,
    tipo        VARCHAR(30)  NOT NULL,
    mensagem    VARCHAR(200) NOT NULL,
    data_hora   DATETIME     NOT NULL,
    resolvido   BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_alerta PRIMARY KEY (id),
    CONSTRAINT fk_alerta_vaca FOREIGN KEY (vaca_id) REFERENCES vaca (id) ON DELETE CASCADE,
    CONSTRAINT fk_alerta_ordenha FOREIGN KEY (ordenha_id) REFERENCES ordenha (id) ON DELETE CASCADE,
    CONSTRAINT ck_alerta_tipo CHECK (tipo IN ('SUSPEITA_MASTITE', 'QUEDA_PRODUCAO', 'LEITE_EM_CARENCIA'))
);

CREATE TABLE IF NOT EXISTS tratamento (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    vaca_id        BIGINT       NOT NULL,
    medicamento    VARCHAR(100) NOT NULL,
    data_inicio    DATE         NOT NULL,
    data_fim       DATE,
    dias_carencia  INT          NOT NULL,
    observacao     VARCHAR(255),
    CONSTRAINT pk_tratamento PRIMARY KEY (id),
    CONSTRAINT fk_tratamento_vaca FOREIGN KEY (vaca_id) REFERENCES vaca (id) ON DELETE CASCADE,
    CONSTRAINT ck_tratamento_carencia CHECK (dias_carencia >= 0),
    CONSTRAINT ck_tratamento_periodo CHECK (data_fim IS NULL OR data_fim >= data_inicio)
);
