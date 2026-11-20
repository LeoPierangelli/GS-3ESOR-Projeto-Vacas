# Contrato entre as partes: banco, regras de alerta e dados de exemplo

A referência comum a todas as partes: banco, API, app, simulador, testes e vídeo.

## 1. Banco

O banco oficial fica em `banco/`, e a carga é gerada por `banco/gerar_dados.py` (que gera também os dados do app e do simulador). O `docker-compose.yml` da API roda direto `banco/01_criar_tabelas.sql` e `banco/02_carga_inicial.sql`, sem cópia na pasta da API. Para recriar o banco do zero, dentro de `api/`:

```bash
docker compose down -v
docker compose up -d
```

- **Tabelas e colunas:** `usuario`, `propriedade`, `vaca`, `ordenha`, `alerta` e `tratamento`, com os mesmos nomes na API e no app.
- **`alerta.vaca_id`:** FK composta `(ordenha_id, vaca_id)` → `ordenha(id, vaca_id)`, então o alerta sempre aponta para a vaca da própria ordenha.
- **`tratamento.data_fim`:** opcional. NULL significa tratamento em andamento, com o leite ainda em carência.
- **Datas da carga:** relativas ao dia em que o banco é criado (os 10 dias anteriores), então as consultas e a carência funcionam em qualquer dia.
- **Condutividade:** em mS, na escala do MastitAlert (de cerca de 0,34 a 0,65).

## 2. Usuários de teste

| Email | Senha | Propriedade | Vacas |
|---|---|---|---|
| joao@exemplo.com | senha123 | 1 · Sítio Boa Vista (Itamonte/MG) | 1 a 5 |
| maria@exemplo.com | senha123 | 2 · Fazendinha Santa Luzia (Pouso Alegre/MG) | 6 a 8 |

O hash é BCrypt `$2a$`, custo 10, o mesmo da API.
**Controle de acesso:** com o login do João, `GET /vacas/6` responde 404, porque a vaca 6 é da Maria.

## 3. JSON de uma leitura (`POST /ordenhas`)

```json
{ "vacaId": 1, "dataHora": "2026-11-18T06:10:00", "turno": "MANHA", "litros": 6.4, "condutividade": 0.468 }
```

Litros com 1 casa decimal e condutividade em mS com 3 casas. A resposta da API traz `ordenha` e `alertasGerados`.

## 4. Regras de alerta: duas camadas

**1) Alarme do aparelho (inspirado no MastitAlert), em `iot/sensores.py`**
- Condutividade ≥ **0,537 mS** dispara o buzzer e o aviso no display, e a válvula desvia o leite, **na hora**.
- É um limite fixo, como no protótipo do artigo. Não depende do histórico e funciona mesmo sem internet.

**2) Alertas do sistema (gravados no banco pela API), em `RegrasAlertaService`, reproduzido em `iot/regras.py`**

| Regra | Dispara quando | Observação |
|---|---|---|
| SUSPEITA_MASTITE | condutividade > média das últimas 7 ordenhas da vaca × 1,15 | precisa de pelo menos 3 ordenhas anteriores; gera um alerta a cada ordenha alterada |
| QUEDA_PRODUCAO | a atual e as 2 anteriores < 80% da média das 7 ordenhas antes delas | precisa de 5 ordenhas anteriores; **não repete** enquanto houver queda aberta |
| LEITE_EM_CARENCIA | dia da ordenha entre `data_inicio` e `data_fim + dias_carencia` (inclusive), ou tratamento sem `data_fim` | um alerta por ordenha em carência |

**Por que as duas camadas?** A vaca BR-003 (Mimosa) mostra a diferença:
- a condutividade dela subiu durante alguns dias e se estabilizou alta;
- depois que a média dela também sobe, a regra do sistema para de disparar;
- **o alarme do aparelho continua apitando**, porque o valor está acima de 0,537 mS.

Cada camada cobre o ponto fraco da outra:
- o limite fixo erra em vacas com condutividade naturalmente alta;
- a regra relativa demora a perceber uma vaca que piora devagar.

## 5. Casos de teste das regras

`iot/test_regras.py` tem 22 testes automatizados (`python3 -m unittest -v test_regras.py`, dentro de `iot/`). Os principais, para o plano de testes:

| # | Histórico (anteriores) | Leitura | Esperado |
|---|---|---|---|
| R1 | 10 × 0,42 mS | 0,43 mS | nenhum alerta |
| R2 | 10 × 0,42 mS | 0,49 mS | SUSPEITA_MASTITE |
| R3 | 7 × 0,40 mS | 0,459 / 0,461 mS | nenhum / SUSPEITA_MASTITE |
| R4 | 2 ordenhas | 0,60 mS | nenhum alerta do sistema (histórico curto), mas o **aparelho** dispara (≥ 0,537) |
| R5 | 7 × 10 L, depois 7,5 e 7,0 | 7,2 L | QUEDA_PRODUCAO |
| R6 | 8 × 10 L, depois 7,0 | 7,0 L | nenhum (só 2 seguidas) |
| R7 | vaca com queda já aberta | 7,0 L de novo | não grava outra queda |
| R8 | tratamento de 12/11 a 16/11 + 4 dias | ordenha em 20/11 / 21/11 | LEITE_EM_CARENCIA / nenhum |
| R9 | tratamento sem `data_fim` | qualquer dia depois do início | LEITE_EM_CARENCIA |

## 6. Roteiro da carga inicial (para testes e vídeo)

| Vaca | Prop. | Situação na carga | Use para |
|---|---|---|---|
| 1 BR-001 Bonita | 1 | normal | **demo ao vivo**: `--doente 1` gera alerta novo na API e apita o aparelho |
| 2 BR-002 Malhada | 1 | normal (média ~0,400 mS) | leitura normal, 201 sem alerta; `0.480` gera suspeita |
| 3 BR-003 Mimosa | 1 | condutividade subindo: 4 alertas de mastite **abertos** | selo de suspeita no app; diferença entre aparelho e sistema |
| 4 BR-004 Estrela | 1 | queda de produção **aberta** | selo de atenção no app |
| 5 BR-005 Princesa | 1 | normal | |
| 6 SL-010 Pintada | 2 | mastite (resolvida), tratamento desde o dia -6, carência **aberta**, 2 tratamentos em 12 meses | carência, consultas 4, 5 e 6 |
| 7 SL-011 Faceira | 2 | mastite **resolvida** (falso positivo) | "suspeita, não diagnóstico" |
| 8 SL-012 Serena | 2 | SECA, sem ordenhas | status SECA |

A carga tem 140 ordenhas e 21 alertas: 7 abertos e 14 resolvidos.
