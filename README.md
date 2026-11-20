# Ordenha Monitorada

Global Solution 2026/2 · FIAP · CitizenTech · Engenharia de Software (3ESOR)

**Vídeo pitch:** https://youtu.be/Cz5no7r_b1U · **PDF da entrega:** [`Ordenha_Monitorada.pdf`](Ordenha_Monitorada.pdf) · **Integrantes:** [`integrantes.txt`](integrantes.txt)

## O projeto

**O problema:** a mastite subclínica não aparece a olho nu. O pequeno produtor de leite descobre tarde, perde produção e corre o risco de mandar leite com antibiótico para o tanque. O equipamento que detecta isso existe, mas só nas fazendas grandes.

**A solução:** dois sensores na ordenha medem cada vaca: os **litros** e a **condutividade elétrica do leite**, que sobe quando há inflamação. O sistema compara cada leitura com a média da própria vaca e avisa o produtor no celular:

| Alerta | Quando dispara |
|---|---|
| Suspeita de mastite | condutividade 15% acima da média das últimas 7 ordenhas da vaca |
| Produção em queda | litros 20% abaixo da média, por 3 ordenhas seguidas |
| Leite em carência | ordenha de vaca em tratamento, dentro do período de carência |

O alerta é sempre uma **suspeita**, nunca um diagnóstico. Os percentuais são valores iniciais do MVP, não parâmetros veterinários validados.

- **Cidadão beneficiado:** o pequeno produtor de leite, com rebanho pequeno e ordenha simples.
- **ODS:** 2 (Fome zero e agricultura sustentável) e 3 (Saúde e bem-estar).

## Arquitetura

```
 sensores na ordenha  ──leitura──▶  API Java  ◀──▶  banco MySQL
 (simulados em iot/)  POST /ordenhas   │  aplica as 3 regras,
                                       │  grava a ordenha e o alerta
                                       ▼
                              app Android do produtor
                         (login, rebanho, ficha da vaca, cadastro)
```

Cada parte tem uma pasta com um README que explica como ela funciona e como rodá-la sozinha:

| Pasta | Parte da entrega | Tecnologia |
|---|---|---|
| [`banco/`](banco/README.md) | 1 · Banco de dados: diagrama ER, scripts `.sql`, carga inicial e consultas | MySQL 8 |
| [`api/`](api/README.md) | 2 · API RESTful e 5 · Segurança | Java 17, Spring Boot, BCrypt, Swagger |
| [`testes/`](testes/README.md) | 3 · Plano de testes, script de execução e evidências | bash + curl, JUnit, unittest |
| [`mobile/`](mobile/README.md) | 4 · Front-end mobile | Kotlin, Jetpack Compose |
| [`iot/`](iot/README.md) | 6 · Simulação de IoT | Python 3 |

As partes usam o mesmo domínio: as tabelas do banco (`usuario`, `propriedade`, `vaca`, `ordenha`, `alerta` e `tratamento`) são os recursos da API e os dados do app. O simulador grava as leituras pela API. O app usa dados mockados, com a mesma carga do banco, e não chama a API (o enunciado permite).

## Como rodar localmente

**Pré-requisitos**

| Para | Precisa de |
|---|---|
| banco e API | Docker (com Docker Compose) e Java 17 ou mais novo. O Maven vem junto (`./mvnw`). |
| simulador de IoT | Python 3.8 ou mais novo, sem bibliotecas extras. No Linux e no WSL o comando é `python3`; no Windows, `python`. |
| plano de testes | `bash` e `curl` (Linux, WSL ou Git Bash) |
| app | Android Studio, com um emulador ou um aparelho Android 8.0+ |

**1. Clonar o repositório**

```bash
git clone https://github.com/LeoPierangelli/GS-3ESOR-Projeto-Vacas.git
```
```bash
cd GS-3ESOR-Projeto-Vacas
```

**2. Subir o banco.** Dentro de `api/`. Na primeira vez, o MySQL cria as tabelas e a carga inicial com os scripts de `banco/`:

```bash
docker compose up -d
```

Espere uns 30 segundos, até o `docker ps` mostrar o `ordenha-mysql` como `(healthy)`.

**3. Subir a API**, também dentro de `api/`. Deixe esse terminal aberto:

```bash
./mvnw spring-boot:run
```

Abra o Swagger em http://localhost:8080/swagger-ui.html. Faça login em `POST /auth/login` com `joao@exemplo.com` / `senha123`, copie o `token`, clique em **Authorize** e cole o token. A partir daí dá para chamar todos os endpoints (por exemplo, `GET /vacas` e `GET /alertas`).

**4. Rodar o simulador dos sensores enviando para a API.** Dentro de `iot/`, com o token do passo 3:

```bash
python3 simulador_sensores.py --propriedade 1 --doente 1 --api http://localhost:8080 --token SEU_TOKEN
```

Cada leitura vai para `POST /ordenhas`, e o simulador mostra os alertas que a API gerou. Eles aparecem em `GET /alertas` no Swagger.

**5. Rodar o plano de testes da API.** Na raiz do repositório, com a API rodando. Ele roda os 15 casos e grava as evidências em `testes/evidencias/`:

```bash
bash testes/executar_testes.sh
```

**6. Rodar os testes de unidade.** São 37 no total: 7 da API (dentro de `api/`), 22 do simulador (dentro de `iot/`) e 8 do app (dentro de `mobile/`, com `./gradlew testDebugUnitTest`):

```bash
./mvnw test
```
```bash
python3 -m unittest -v test_regras.py
```

**7. Abrir o app.** No Android Studio, escolha **File > Open**, selecione a pasta `mobile/` e clique em **Run**. Para entrar, use `joao@exemplo.com` / `senha123`. Os detalhes estão em [`mobile/README.md`](mobile/README.md).

**Para desligar:** `Ctrl+C` no terminal da API e, dentro de `api/`, `docker compose down`. Para recomeçar o banco do zero, com a carga inicial, use `docker compose down -v` e depois `docker compose up -d`.

## Entrega

O `.zip` entregue na plataforma tem o código com um README de execução por parte, o [PDF](Ordenha_Monitorada.pdf) com os entregáveis de cada parte (diagramas, plano de testes e evidências) e o [`integrantes.txt`](integrantes.txt), com o nome do grupo, o RM e o nome de cada integrante e o link do vídeo pitch.

## Créditos

- **Inspiração do sensor:** PORTUGAL, G.; GUEDES, G. *MastitAlert: towards detection of mastitis during the act of milking.* BreSci 2021, SBC. DOI 10.5753/bresci.2021.15800.
- **Demais fontes:** na seção "Fontes e créditos" do [PDF](Ordenha_Monitorada.pdf).
- **Dados:** todos os dados são fictícios ou simulados.

## Uso de IA generativa

O grupo usou o Claude (Anthropic) como apoio para pesquisar as fontes, estruturar o código, revisar e escrever a documentação. Todo o código foi revisado, executado e testado pelos integrantes, e cada um explica a própria parte.
