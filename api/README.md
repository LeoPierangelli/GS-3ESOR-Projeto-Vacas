# Ordenha Monitorada: API RESTful + Segurança

Esta é a API da Ordenha Monitorada (Global Solution 2026/2, CitizenTech). Ela recebe as leituras dos sensores da ordenha (litros e condutividade do leite de cada vaca), compara cada leitura com o histórico da própria vaca e gera três tipos de alerta para o pequeno produtor de leite:

| Alerta | Quando dispara | Mensagem |
|---|---|---|
| `SUSPEITA_MASTITE` | Condutividade 15% acima da média das últimas 7 ordenhas da vaca | "Suspeita de mastite: faça o teste da caneca ou o CMT" |
| `QUEDA_PRODUCAO` | Litros 20% abaixo da média das 7 ordenhas anteriores, por 3 ordenhas seguidas | "Produção em queda" |
| `LEITE_EM_CARENCIA` | Ordenha de vaca com tratamento ainda dentro do período de carência | "Não enviar este leite ao tanque" |

> Os percentuais são valores iniciais do MVP, **não são parâmetros veterinários validados**. Eles ficam em constantes configuráveis no `application.properties` (prefixo `ordenha.regras`). O alerta é sempre uma **suspeita**, nunca um diagnóstico.

## Tecnologias

- Java 17, Spring Boot 4 (Spring Web MVC, Spring Data JPA, Bean Validation)
- MySQL 8.4 (rodando no Docker)
- BCrypt (`spring-security-crypto`) para o hash das senhas
- Swagger UI (springdoc-openapi) e Postman Collection
- JUnit 5 para os testes de unidade das regras

## Como rodar

**Pré-requisitos:** Java 17 ou mais novo, e o Docker (com Docker Compose). Não precisa instalar o Maven: o projeto usa o Maven Wrapper (`./mvnw`).

Todos os comandos abaixo rodam dentro da pasta `api/`.

**1. Subir o banco.** Na primeira vez, o MySQL cria as tabelas e a carga inicial com os scripts da pasta `banco/`:

```bash
docker compose up -d
```

Espere uns 20 segundos. O comando `docker compose ps` deve mostrar o container `ordenha-mysql` como `healthy`.

**2. Subir a API**, que fica em `http://localhost:8080`:

```bash
./mvnw spring-boot:run
```

No Windows, use `mvnw.cmd spring-boot:run`.

**3. Abrir o Swagger:** http://localhost:8080/swagger-ui.html

Para usar o Swagger:
1. Abra `POST /auth/login`, clique em *Try it out* e envie as credenciais abaixo.
2. Copie o `token` da resposta.
3. Clique em **Authorize**, no alto da página, e cole o token.

```json
{ "email": "joao@exemplo.com", "senha": "senha123" }
```

**Usuários de exemplo** (a senha está gravada no banco só como hash BCrypt):

| Email | Senha | Propriedade | Vacas |
|---|---|---|---|
| `joao@exemplo.com` | `senha123` | Sítio Boa Vista (id 1) | 1 a 5 |
| `maria@exemplo.com` | `senha123` | Fazendinha Santa Luzia (id 2) | 6 a 8 |

A carga inicial (o banco oficial de [`banco/`](../banco/README.md), com datas dos 10 dias anteriores à criação do banco) já tem os casos que aparecem no app e no vídeo:
- **Vaca 3 (Mimosa):** condutividade subindo, com alertas de suspeita de mastite.
- **Vaca 4 (Estrela):** produção em queda.
- **Vaca 6 (Pintada):** em tratamento, com o leite em carência.
- **Vaca 8 (Serena):** seca.

**Rodar os testes automatizados** (não precisam do banco):

```bash
./mvnw test
```

**Recomeçar o banco do zero**, apagando os dados e rodando os scripts de novo:

```bash
docker compose down -v
```
```bash
docker compose up -d
```

Se a porta 3306 já estiver em uso na sua máquina, suba o banco em outra porta e aponte a API para ela:

```bash
MYSQL_PORT=3307 docker compose up -d
```
```bash
DB_URL="jdbc:mysql://localhost:3307/ordenha?useSSL=false&allowPublicKeyRetrieval=true" ./mvnw spring-boot:run
```

## Endpoints

Todas as rotas fora de `/auth` exigem o cabeçalho `Authorization: Bearer <token>`.

| Método | Rota | O que faz | Sucesso | Erros |
|---|---|---|---|---|
| POST | `/auth/cadastro` | Cria usuário (senha em hash) e a primeira propriedade dele | 201 | 400, 409 |
| POST | `/auth/login` | Valida email e senha e devolve o token | 200 | 400, 401 |
| GET | `/vacas` | Lista o rebanho com status e alertas abertos | 200 | 401 |
| GET | `/vacas/{id}` | Ficha: últimas ordenhas, condutividade contra a média, alertas, tratamentos | 200 | 401, 404 |
| POST | `/vacas` | Cadastra vaca | 201 | 400, 404, 409 |
| PUT | `/vacas/{id}` | Atualiza vaca | 200 | 400, 404, 409 |
| DELETE | `/vacas/{id}` | Remove vaca (e o histórico dela) | 204 | 401, 404 |
| POST | `/ordenhas` | Recebe a leitura, grava e aplica as regras de alerta | 201 | 400, 404, 409 |
| GET | `/vacas/{id}/ordenhas` | Histórico de ordenhas da vaca | 200 | 401, 404 |
| GET | `/alertas?resolvido=false` | Lista alertas (o filtro é opcional) | 200 | 401 |
| PUT | `/alertas/{id}/resolver` | Marca o alerta como resolvido | 200 | 401, 404 |
| POST | `/tratamentos` | Registra tratamento e põe a vaca em `EM_TRATAMENTO` | 201 | 400, 404 |

Exemplo de leitura enviada pelos sensores (`POST /ordenhas`):

```json
{
  "vacaId": 2,
  "dataHora": "2026-11-19T06:10:00",
  "turno": "MANHA",
  "litros": 8.6,
  "condutividade": 0.480
}
```

- **Unidades:** litros com 1 casa decimal e condutividade em mS, na escala do MastitAlert (leite normal em torno de 0,40). Veja [`docs/README.md`](../docs/README.md).
- **`dataHora`:** use o momento atual ou um horário recente. Data futura responde 400, e a mesma vaca no mesmo horário de uma leitura que já existe responde 409.
- **Resposta:** traz a ordenha gravada e a lista `alertasGerados`, que vem vazia quando nenhuma regra dispara. No exemplo acima, a Malhada (média em torno de 0,400 mS) gera `SUSPEITA_MASTITE`.

O simulador de sensores da pasta [`iot/`](../iot/README.md) envia leituras para esta rota com `--api` e `--token`.

**Documentação:**
- O Swagger roda junto com a API.
- A Postman Collection está em [`docs/ordenha-api.postman_collection.json`](docs/ordenha-api.postman_collection.json). Importe no Postman e rode *Auth > Login (João)* primeiro: o token é salvo sozinho. As ordenhas usam a variável `{{agora}}` (o horário do envio), então a coleção pode rodar várias vezes. Só o *Cadastro de usuário* volta 409 a partir da segunda vez, porque o email já existe.
- A especificação OpenAPI exportada está em [`docs/openapi.json`](docs/openapi.json).

## Arquitetura em camadas

```
src/main/java/br/com/fiap/ordenha/
├── controller/   recebe a requisição HTTP e devolve o status; nenhuma regra de negócio
├── service/      regras de negócio: as 3 regras de alerta (RegrasAlertaService), cadastro, login
├── repository/   acesso ao banco (Spring Data JPA, no papel do DAO)
├── model/        entidades (os mesmos nomes das tabelas) e enums
├── dto/          dados de entrada (com validação) e de saída (sem senha nem hash)
├── exception/    exceções e o tratador global que as transforma em status HTTP
├── security/     TokenService (sessões) e AutenticacaoFilter (o filtro de login)
└── config/       BCrypt, CORS, Swagger e as constantes das regras
```

Fluxo de uma leitura: `OrdenhaController` → `OrdenhaService` (busca o histórico, grava a ordenha e chama `RegrasAlertaService`) → `OrdenhaRepository` / `AlertaRepository`.

## Segurança

1. **Login seguro.** A senha é gravada com **BCrypt** (custo 10, salt próprio por senha) e nunca em texto puro. Para conferir no banco:

   ```bash
   docker exec ordenha-mysql mysql -uordenha -pordenha ordenha -e "SELECT id, email, senha_hash FROM usuario"
   ```

   O login errado responde **401** com uma mensagem genérica ("Email ou senha inválidos"), que não revela se o email existe.
2. **Validação de entrada.** Todo corpo de requisição passa pelo Bean Validation, e dado inválido volta como **400** com o erro de cada campo. As regras são:
   - email em formato válido e senha com 8 caracteres ou mais;
   - litros e condutividade maiores que zero;
   - brinco obrigatório;
   - data da ordenha que não pode ser futura;
   - turno `MANHA` ou `TARDE`;
   - UF com 2 letras;
   - `dataFim` do tratamento igual ou posterior a `dataInicio`.
3. **Proteção contra SQL injection.** Todo acesso ao banco passa pelo JPA com consultas parametrizadas, e nenhum SQL é montado concatenando texto. Por exemplo, o login com o email `' OR '1'='1` responde 401, sem nenhum erro de banco.
4. **Controle de acesso.** O `AutenticacaoFilter` bloqueia com **401** toda rota fora de `/auth` que chegue sem um token válido. Toda consulta filtra pelo dono da propriedade: o usuário só enxerga e altera as vacas, ordenhas, alertas e tratamentos das próprias propriedades. Pedir uma vaca de outro produtor responde **404**, igual a uma vaca inexistente, para não revelar que ela existe.
5. **Proteção contra XSS.** Os campos de texto (nomes, brinco, medicamento, observação) recusam `<` e `>`, e as respostas são sempre JSON, com o cabeçalho `X-Content-Type-Options: nosniff`.
6. **Nada de senha nas respostas.** Os DTOs de saída nunca incluem a senha nem o hash. Os erros também não expõem stack trace nem mensagens internas do banco.

O token de login é um valor aleatório de 256 bits (`SecureRandom`) guardado em memória, com validade de 8 horas. Ele faz o mesmo papel da sessão do estudo de caso (LoginServlet + Filtro). Por isso, reiniciar a API desloga todo mundo.

## Banco de dados

O Docker da API roda os scripts oficiais da pasta [`banco/`](../banco/README.md) (`01_criar_tabelas.sql` e `02_carga_inicial.sql`), onde também estão o diagrama ER e as consultas. Não há cópia dos scripts dentro de `api/`. Depois de atualizar o repositório, recrie o banco com `docker compose down -v` e depois `docker compose up -d`.

As datas da carga são relativas ao dia em que o banco é criado (os 10 dias anteriores). Por isso os alertas e a carência fazem sentido em qualquer dia.

## Créditos

- A ideia do sensor de condutividade vem do protótipo **MastitAlert: em direção à detecção da mastite durante o ato da ordenha**, da Universidade Católica de Petrópolis. Aqui as leituras são simuladas.
- Embrapa e APTA, sobre o uso da condutividade elétrica do leite na detecção de mastite subclínica.

## Uso de IA generativa

Usamos o Claude (Anthropic) como apoio para montar o esqueleto do projeto, revisar o código e escrever a documentação. Todo o código foi revisado, executado e testado pelo grupo.
