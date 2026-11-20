# Ordenha Monitorada: Plano de testes

Parte 3 da entrega (2 pts). O plano tem **15 casos de teste da API**, 9 deles de erro (4 de segurança), todos executados por script e com evidência salva. Além deles, há **37 testes automatizados de unidade** nas outras partes.

| Arquivo | O que é |
|---|---|
| [`executar_testes.sh`](executar_testes.sh) | roda os 15 casos contra a API e grava as evidências |
| [`evidencias/relatorio.md`](evidencias/relatorio.md) | o resultado da última execução: cenário, entrada, esperado, obtido e status |
| [`evidencias/CT01.txt` … `CT15.txt`](evidencias/) | a requisição e a resposta completas de cada caso |
| [`evidencias/hash_senhas.txt`](evidencias/hash_senhas.txt) | a tabela `usuario` no MySQL, mostrando as senhas só como hash BCrypt |
| [`evidencias/testes_unidade.txt`](evidencias/testes_unidade.txt) | a saída dos testes de unidade da API, do app e do simulador |

## Como rodar

Na primeira vez, recrie o banco para partir sempre da mesma carga. Dentro de `api/`:

```bash
docker compose down -v
```
```bash
docker compose up -d
```

Depois suba a API, também dentro de `api/`:

```bash
./mvnw spring-boot:run
```

Com a API rodando, execute o plano num outro terminal, na raiz do repositório:

```bash
bash testes/executar_testes.sh
```

O script precisa de `bash`, `curl` e do `date` do GNU, que vêm no Linux, no WSL e no Git Bash do Windows. Ele mostra um resumo no terminal e grava as evidências em `testes/evidencias/`.

## Casos de teste da API

| Caso | Cenário | Entrada | Saída esperada | Tipo |
|---|---|---|---|---|
| CT01 | Cadastro de usuário válido | nome, email novo, senha válida | 201, sem senha nem hash na resposta | sucesso |
| CT02 | Login com senha errada | email existente, senha incorreta | 401 e mensagem genérica | erro |
| CT03 | Cadastro de vaca válido | brinco novo | 201 e a vaca aparece em `GET /vacas` | sucesso |
| CT04 | Brinco repetido na propriedade | o mesmo brinco de novo | 409 | erro |
| CT05 | Ordenha com litros negativos | `litros = -2` | 400, com erro no campo `litros` | erro |
| CT06 | Ordenha normal | BR-002, 9,0 L, 0,402 mS (na média da vaca) | 201 e nenhum alerta | sucesso |
| CT07 | Condutividade acima da média | BR-002, 0,480 mS (+20%) | 201 e alerta `SUSPEITA_MASTITE` | regra |
| CT08 | Ordenha de vaca em carência | SL-010 Pintada, com tratamento ativo | 201 e alerta `LEITE_EM_CARENCIA` | regra |
| CT09 | Vaca inexistente | `GET /vacas/9999` | 404 | erro |
| CT10 | SQL injection no login | email `' OR '1'='1` | 401, sem erro de banco | segurança |
| CT11 | Rota protegida sem token | `GET /vacas` sem `Authorization` | 401 | segurança |
| CT12 | Vaca de outro produtor | o João pede `GET /vacas/6` (da Maria) | 404, sem revelar que a vaca existe | segurança |
| CT13 | Ordenha com data futura | `dataHora` de amanhã | 400, com erro no campo `dataHora` | erro |
| CT14 | XSS no nome da vaca | nome `<script>alert(1)</script>` | 400, nada gravado | segurança |
| CT15 | Excluir vaca | `DELETE` da vaca do CT03 | 204 e depois 404 | sucesso |

A tabela com o status de cada caso, passou ou falhou, sai em [`evidencias/relatorio.md`](evidencias/relatorio.md).

## Testes automatizados de unidade

Eles rodam por comando e geram relatório, o que conta como diferencial.

| Parte | Comando | Testes | O que cobrem |
|---|---|---|---|
| API | `cd api && ./mvnw test` | 7 (JUnit 5) | as 3 regras de alerta do `RegrasAlertaService` |
| App mobile | `cd mobile && ./gradlew testDebugUnitTest` | 8 (JUnit 4) | os dados do app iguais à carga do banco, login por produtor, alertas seguindo as regras da API, brinco repetido |
| Simulador de IoT | `cd iot && python3 -m unittest -v test_regras.py` | 22 (unittest) | as regras de alerta, as equações do sensor e o alarme do aparelho |

## Outras formas de testar

- **Postman:** importe [`api/docs/ordenha-api.postman_collection.json`](../api/docs/ordenha-api.postman_collection.json) e clique em *Run*. Cada requisição já confere o status esperado.
- **Swagger:** abra http://localhost:8080/swagger-ui.html com a API rodando.
- **Casos das regras de alerta, de R1 a R9:** em [`docs/REGRAS.md`](../docs/REGRAS.md), seção 5. Estão cobertos pelos testes do simulador de IoT.
