# Textos para o PDF de entrega · Abertura, Partes 2 a 5 e ODS

> Junto com o [`texto_pdf_parte1.md`](texto_pdf_parte1.md) (partes 1 e 6), estes textos formam o PDF.
> Ordem sugerida: **Abertura → 1 Banco → 2 API → 3 Testes → 4 Mobile → 5 Segurança → 6 IoT → ODS → Fontes**.
> Onde aparece *(inserir print)*, tire o print com o sistema rodando, seguindo os READMEs.

---

## Abertura

**Ordenha Monitorada** · Global Solution 2026/2 · CitizenTech · Engenharia de Software (3ESOR)

**Integrantes:** *(nome e RM de cada integrante, iguais ao `integrantes.txt`)*

**Repositório:** https://github.com/LeoPierangelli/GS-3ESOR-Projeto-Vacas · **Vídeo pitch:** *(link público)*

**Cidadão beneficiado:** o pequeno produtor de leite, com rebanho pequeno e ordenha simples.

**Problema:** a mastite é a inflamação do úbere da vaca e a doença infecciosa mais comum do rebanho leiteiro. Na forma subclínica, ela não aparece a olho nu. O produtor descobre tarde, perde produção e corre o risco de mandar leite com resíduo de antibiótico para o tanque. A detecção pela condutividade elétrica do leite já existe, mas em equipamentos caros, de fazendas grandes.

**Solução:** dois sensores na ordenha medem, para cada vaca, os **litros** e a **condutividade elétrica do leite**, que sobe quando há inflamação. O sistema compara cada leitura com a **média da própria vaca** e avisa o produtor no celular. Há três alertas:
- **suspeita de mastite:** condutividade 15% acima da média das últimas 7 ordenhas;
- **produção em queda:** litros 20% abaixo da média, por 3 ordenhas seguidas;
- **leite em carência:** ordenha de uma vaca em tratamento, dentro do período de carência.

O alerta é sempre uma **suspeita**, nunca um diagnóstico. Os percentuais são valores iniciais do MVP e ficam em constantes configuráveis, porque **não são parâmetros veterinários validados**.

**Como as partes se conectam:** os sensores, simulados na Parte 6, enviam a leitura para a API (`POST /ordenhas`). A API aplica as regras, grava a ordenha e o alerta no banco, e o app mostra o rebanho e os alertas. As seis partes usam o mesmo domínio: as tabelas do banco são os recursos da API e os dados do app (`usuario`, `propriedade`, `vaca`, `ordenha`, `alerta`, `tratamento`).

---

## Parte 2 · API RESTful

A API foi feita em **Java 17 com Spring Boot** (Spring Web MVC, Spring Data JPA e Bean Validation) e usa o banco **MySQL** da Parte 1. Ela faz a ponte entre os sensores, o banco e o app.

### 2.1 Endpoints

São **12 endpoints**, cobrindo GET, POST, PUT e DELETE:

| Método | Rota | O que faz | Sucesso | Erros |
|---|---|---|---|---|
| POST | `/auth/cadastro` | cria o usuário (senha em hash) e a primeira propriedade dele | 201 | 400, 409 |
| POST | `/auth/login` | valida email e senha e devolve o token de acesso | 200 | 400, 401 |
| GET | `/vacas` | lista o rebanho do usuário, com status e alertas abertos | 200 | 401 |
| GET | `/vacas/{id}` | ficha da vaca: últimas ordenhas, condutividade contra a média, alertas e tratamentos | 200 | 401, 404 |
| POST | `/vacas` | cadastra uma vaca | 201 | 400, 404, 409 |
| PUT | `/vacas/{id}` | atualiza uma vaca | 200 | 400, 404, 409 |
| DELETE | `/vacas/{id}` | remove a vaca e o histórico dela | 204 | 401, 404 |
| POST | `/ordenhas` | recebe a leitura dos sensores, grava e aplica as regras de alerta | 201 | 400, 404, 409 |
| GET | `/vacas/{id}/ordenhas` | histórico de ordenhas da vaca | 200 | 401, 404 |
| GET | `/alertas?resolvido=` | lista os alertas, com filtro opcional | 200 | 401 |
| PUT | `/alertas/{id}/resolver` | marca o alerta como resolvido | 200 | 401, 404 |
| POST | `/tratamentos` | registra o tratamento e passa a vaca para `EM_TRATAMENTO` | 201 | 400, 404 |

A resposta do `POST /ordenhas` traz a ordenha gravada e a lista `alertasGerados`, que vem vazia quando nenhuma regra dispara.

*(inserir print do Swagger com a lista de endpoints)*

### 2.2 Camadas

| Camada | Pacote | Responsabilidade |
|---|---|---|
| Controller | `controller/` | recebe a requisição HTTP, valida o corpo (`@Valid`) e devolve o status. Não tem regra de negócio. |
| Service | `service/` | as regras de negócio. As **3 regras de alerta** ficam em `RegrasAlertaService`, e a gravação da leitura com os alertas em `OrdenhaService`. |
| Repository | `repository/` | acesso ao banco com Spring Data JPA, no papel do DAO. |
| Model e DTO | `model/`, `dto/` | as entidades, com os mesmos nomes das tabelas, e os dados de entrada e saída. A saída nunca inclui a senha nem o hash. |

Exemplo do fluxo de uma leitura: `OrdenhaController` → `OrdenhaService` → busca o histórico da vaca no `OrdenhaRepository` → `RegrasAlertaService` decide os alertas → grava com `OrdenhaRepository` e `AlertaRepository`.

### 2.3 Respostas e validação

- **Status HTTP:** 200 nas consultas, 201 na criação, 204 na exclusão, 400 para dado inválido, 401 para login errado ou sem token, 404 para id inexistente e 409 para email, brinco ou leitura repetidos.
- **Erros num formato único** (status, erro, mensagem e o erro de cada campo), sem stack trace e sem mensagens internas do banco.
- **Validações de entrada:**
  - email válido e senha com 8 caracteres ou mais;
  - litros e condutividade maiores que zero;
  - brinco obrigatório;
  - data da ordenha que não pode ser futura;
  - turno `MANHA` ou `TARDE`;
  - UF com 2 letras;
  - fim do tratamento igual ou posterior ao início.

### 2.4 Documentação

- **Swagger** (OpenAPI), que roda junto com a API em `http://localhost:8080/swagger-ui.html`, com o botão *Authorize* para o token.
- **Postman Collection** com as 18 requisições e testes de status: `api/docs/ordenha-api.postman_collection.json`.
- A especificação OpenAPI exportada fica em `api/docs/openapi.json`.

---

## Parte 3 · Plano de testes

O plano tem **15 casos de teste da API**, 8 deles de erro. Os 15 foram **executados** por um script (`testes/executar_testes.sh`), que grava a requisição e a resposta de cada caso como evidência.

*(colar aqui a tabela de `testes/evidencias/relatorio.md`, que já tem cenário, entrada, saída esperada, obtido e status)*

**Evidências:**
- `testes/evidencias/CT01.txt` a `CT15.txt`: a requisição e a resposta completas de cada caso.
- `testes/evidencias/hash_senhas.txt`: a tabela `usuario` mostrando as senhas só como hash.
- *(inserir print do terminal com o resumo "15 de 15 casos passaram")*
- *(opcional: print do Postman Runner com a coleção passando)*

**Testes automatizados de unidade:** são 37, que rodam por comando e geram relatório.
- **API:** 7 testes JUnit das regras de alerta (`./mvnw test`).
- **App:** 8 testes JUnit, que conferem os dados, o login por produtor e as regras (`./gradlew testDebugUnitTest`).
- **Simulador:** 22 testes com unittest (`python -m unittest -v test_regras.py`).

A saída dos três está em `testes/evidencias/testes_unidade.txt`.

---

## Parte 4 · Front-end mobile

O app foi feito em **Kotlin com Jetpack Compose** (Material 3 e Navigation Compose), para Android 8.0 ou superior. Ele usa **a mesma carga do banco** como dados mockados (`DadosCarga.kt`, gerado junto com o `.sql`), então mostra as mesmas vacas, ordenhas e alertas que a API.

### 4.1 Telas

| Tela | O que mostra | Para onde vai |
|---|---|---|
| Login | email, senha, validação, erro de credenciais e indicador de carregamento | Rebanho |
| Rebanho | as vacas do produtor, com brinco, nome e selo de status, e o contador de alertas abertos | Ficha, Cadastro, Sair |
| Ficha da vaca | histórico de ordenhas, a última condutividade contra a média das 7 anteriores, alertas e tratamentos | Voltar ao Rebanho |
| Cadastro de vaca | brinco, nome e nascimento, com validação e mensagem de sucesso | Volta ao Rebanho com a vaca na lista |

*(inserir um print de cada tela: Login, Rebanho, Ficha da Mimosa com a suspeita de mastite e Cadastro com erro de validação)*

### 4.2 Navegação

Toda tela tem como voltar, e o Rebanho tem o botão **Sair**, que volta para o Login. Não existe tela sem saída. Como na API, cada produtor só vê o próprio rebanho: o João vê as vacas 1 a 5, e a Maria, as vacas 6 a 8.

### 4.3 Usabilidade

- **Pensado para o curral:** botões grandes e texto legível.
- **Status com texto, ícone e cor**, nunca só a cor: Normal ✓, Suspeita ⚠, Em tratamento e Seca.
- **Retorno em toda ação:**
  - indicador de carregamento no login;
  - mensagem de erro nos campos inválidos (data impossível ou futura, brinco repetido);
  - mensagem de sucesso depois do cadastro.

---

## Parte 5 · Segurança

### 5.1 Login seguro

- A senha é gravada com **BCrypt**, com custo 10 e um salt próprio para cada senha. A senha em texto puro nunca chega ao banco.
- O login errado responde **401** com uma mensagem genérica ("Email ou senha inválidos"), que não revela se o email existe.
- Depois do login, a API devolve um **token** aleatório de 256 bits (`SecureRandom`), válido por 8 horas. Ele é enviado no cabeçalho `Authorization: Bearer <token>`.

*(inserir print de `testes/evidencias/hash_senhas.txt` ou da consulta `SELECT id, email, senha_hash FROM usuario`)*

### 5.2 Boas práticas

1. **Validação de entrada.** Todo corpo de requisição passa pelo Bean Validation, e um dado inválido é recusado com **400**, com o erro de cada campo (casos CT05 e CT13).
2. **Proteção contra SQL injection.** Todo acesso ao banco usa JPA com consultas parametrizadas, e nenhum SQL é montado concatenando texto. Por exemplo, o login com o email `' OR '1'='1` responde 401, sem erro de banco (caso CT10).
3. **Controle de acesso.**
   - Um filtro de autenticação bloqueia com **401** toda rota fora de `/auth` que chegue sem um token válido (caso CT11).
   - Toda consulta filtra pelo dono da propriedade, então o produtor só vê e altera as próprias vacas.
   - Pedir uma vaca de outro produtor responde **404**, igual a uma vaca inexistente, para não revelar que ela existe (caso CT12).
4. **Proteção contra XSS.** Os campos de texto recusam `<` e `>` (caso CT14), e as respostas são sempre JSON, com o cabeçalho `X-Content-Type-Options: nosniff`.
5. **Nada de senha nas respostas.** Nenhuma resposta da API devolve a senha ou o hash (caso CT01), e os erros não expõem stack trace nem mensagens internas.

---

## Conexão com os ODS

- **ODS 2 · Fome zero e agricultura sustentável.**
  - **Meta 2.3** (produtividade e renda dos pequenos produtores): a detecção precoce reduz a perda de leite e o descarte de produção, o que aumenta a renda de quem tem poucas vacas.
  - **Meta 2.4** (sistemas de produção sustentáveis): o produtor passa a tratar só a vaca com suspeita confirmada, em vez de medicar às cegas.
  - Leva ao pequeno produtor uma tecnologia que hoje só existe em equipamento caro.
- **ODS 3 · Saúde e bem-estar.**
  - **Meta 3.9** (reduzir doenças por contaminação): o alerta "não enviar este leite ao tanque" evita que leite com resíduo de antibiótico chegue ao consumidor.
  - A detecção precoce também ajuda a **usar menos antibiótico**, o que contribui contra a resistência antimicrobiana.

**Como o impacto pode ser medido:**
- quantos alertas de suspeita foram confirmados pelo teste da caneca ou pelo CMT;
- quantos litros deixaram de ir ao tanque durante a carência;
- quanto a produção mudou entre as vacas tratadas cedo e as tratadas tarde.

**Como pode ser ampliado:**
- sensores de baixo custo, no estilo do MastitAlert;
- uso por cooperativas de leite;
- notificações no celular.
