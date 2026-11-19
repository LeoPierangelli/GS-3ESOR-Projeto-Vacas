# Ordenha Monitorada: Plano de testes

> **A fazer** (parte 3 · 2 pts). O enunciado pede:
> - um plano com 8 ou mais casos (cenário, entrada, saída esperada e status), incluindo casos de erro;
> - 5 ou mais testes executados, manuais ou automatizados;
> - evidências (prints, logs ou relatórios) de cada teste executado.
>
> O plano e as evidências vão no PDF da entrega. Esta pasta guarda o material de apoio.

## Ponto de partida

- **Casos de API**, no planejamento do grupo: os 10 casos, de cadastro de usuário a SQL injection no login.
- **Casos das regras de alerta (R1 a R9):** em [`docs/REGRAS.md`](../docs/REGRAS.md), seção 5.
- **Postman Collection com testes de status:** em [`api/docs/ordenha-api.postman_collection.json`](../api/docs/ordenha-api.postman_collection.json). Importe no Postman e clique em *Run*. O relatório do Runner serve de evidência.

## Testes automatizados que já existem

Eles contam como diferencial: são testes de unidade rodando por comando, com relatório.

| Onde | Comando | O que testa |
|---|---|---|
| `api/` | `./mvnw test` | 7 testes JUnit das 3 regras de alerta (`RegrasAlertaServiceTest`). O relatório sai em `api/target/surefire-reports/`. |
| `iot/` | `python -m unittest -v test_regras.py` | 22 testes das regras e dos sensores do simulador |

Antes de tirar os prints, recrie o banco (dentro de `api/`) para partir sempre da mesma carga:

```bash
docker compose down -v
```
```bash
docker compose up -d
```
