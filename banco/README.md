# Ordenha Monitorada: Banco de dados (MySQL)

Modelo oficial do banco: 6 tabelas, carga inicial e 6 consultas de uso real.
É o mesmo banco que a API usa: `api/db/` tem uma cópia destes scripts, e o Docker da API os roda na primeira subida.

| Arquivo | O que é |
|---|---|
| `01_criar_tabelas.sql` | cria o banco `ordenha` e as 6 tabelas, com PK, FK, UNIQUE, CHECK, índices e comentários |
| `02_carga_inicial.sql` | carga inicial (gerada por `ferramentas/gerar_dados.py`; não edite à mão) |
| `03_consultas.sql` | 6 consultas com JOIN e agregação |
| `diagrama_er.png` | diagrama entidade-relacionamento (fonte: `diagrama_er.dot`) |

## Como rodar (MySQL 8.0.16 ou superior)

**MySQL Workbench**
1. Abra o `01_criar_tabelas.sql` (File > Open SQL Script) e execute com o **raio** (Ctrl+Shift+Enter).
2. Faça o mesmo com o `02_carga_inicial.sql`. No fim, ele mostra a contagem: 2 usuários, 2 propriedades, 8 vacas, 2 tratamentos, 140 ordenhas e 21 alertas.
3. Abra o `03_consultas.sql`. Ctrl+Shift+Enter roda tudo, com uma aba de resultado por consulta; Ctrl+Enter roda só a consulta onde está o cursor.

**Terminal**
```bash
mysql -u root -p < banco/01_criar_tabelas.sql
mysql -u root -p < banco/02_carga_inicial.sql
mysql -u root -p -t < banco/03_consultas.sql
```

**Docker da API** (o mesmo banco, já com a carga), dentro de `api/`:
```bash
docker compose down -v
docker compose up -d
```

**Observações**
- Os scripts foram testados no MySQL 8.0 e podem rodar várias vezes: o 01 apaga e recria as 6 tabelas do banco `ordenha`.
- As datas da carga são relativas ao dia da execução (os 10 dias anteriores a hoje). Por isso "últimos 7 dias" e "carência hoje" sempre têm resultado.
- As consultas usam CTE e funções de janela (`WITH`, `RANK`, `ROW_NUMBER`), então precisam do MySQL 8.
- Usuários de teste: `joao@exemplo.com` e `maria@exemplo.com`, senha `senha123` (no banco fica só o hash BCrypt).

## Decisões de modelagem

- **`alerta.vaca_id` com FK composta.** A API busca alertas direto pela vaca, então a coluna existe. Para nunca ficar inconsistente, a FK é `(ordenha_id, vaca_id) → ordenha(id, vaca_id)`: o banco recusa um alerta que aponte para uma vaca diferente da vaca da ordenha (testado: erro 1452).
- **`tratamento.data_fim` opcional.** NULL significa que o tratamento não terminou, e o leite segue em carência, como na API.
- **"Suspeita" não é status da vaca.** O status guarda o manejo (ATIVA, EM_TRATAMENTO, SECA). A suspeita vem de existir alerta aberto.
- **Exclusão em cascata.** Ordenhas, alertas e tratamentos caem junto com a vaca, então `DELETE /vacas/{id}` funciona.
- **Condutividade em mS**, na escala do protótipo MastitAlert (vaca sadia com cerca de 0,4 a 0,5 mS).

O contrato completo (regras, casos de teste e roteiro das vacas) está em [`compartilhado/REGRAS.md`](../compartilhado/REGRAS.md).
