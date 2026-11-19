# Ordenha Monitorada

Global Solution 2026/2 · FIAP · CitizenTech · Engenharia de Software (3ESOR)

**O problema:** a mastite subclínica não aparece a olho nu. O pequeno produtor de leite descobre tarde, perde produção e corre o risco de mandar leite com antibiótico para o tanque. Equipamento que detecta isso existe, mas só em fazenda grande.

**A solução:** dois sensores na ordenha medem cada vaca: **litros** e **condutividade elétrica do leite**, que sobe quando há inflamação. O sistema compara cada leitura com a média da própria vaca e avisa o produtor no celular:

| Alerta | Quando dispara |
|---|---|
| Suspeita de mastite | condutividade 15% acima da média das últimas 7 ordenhas da vaca |
| Produção em queda | litros 20% abaixo da média, por 3 ordenhas seguidas |
| Leite em carência | ordenha de vaca em tratamento, dentro do período de carência |

O alerta é sempre uma **suspeita**, nunca um diagnóstico. Os percentuais são valores iniciais do MVP, não parâmetros veterinários validados.

- **Cidadão beneficiado:** o pequeno produtor de leite, com rebanho pequeno e ordenha simples.
- **ODS:** 2 (Fome zero e agricultura sustentável) e 3 (Saúde e bem-estar).

## Como as partes se conectam

```
 sensores na ordenha  ──leitura──▶  API Java  ◀──▶  banco MySQL
 (simulados em iot/)  POST /ordenhas   │  aplica as 3 regras,
                                       │  grava a ordenha e o alerta
                                       ▼
                              app mobile do produtor
                         (login, rebanho, ficha da vaca, alertas)
```

O enunciado diz que as partes não precisam estar integradas: cada uma roda sozinha pelo próprio README. A integração (o simulador enviando leituras para a API, e o app consumindo a API) conta como diferencial.

## Estrutura do repositório

| Pasta | Parte da entrega | Pontos | Status |
|---|---|---|---|
| [`banco/`](banco/README.md) | 1 · Banco de dados: diagrama ER, script `.sql`, carga inicial e consultas | 2 | pronta |
| [`api/`](api/README.md) | 2 · API RESTful (Java + Spring) e 5 · Segurança | 4 + 2 | pronta |
| [`testes/`](testes/README.md) | 3 · Plano de testes e evidências | 2 | a fazer |
| [`mobile/`](mobile/README.md) | 4 · Front-end mobile | 3 | a fazer |
| [`iot/`](iot/README.md) | 6 · Simulação de IoT | 2 | pronta |
| [`docs/`](docs/) | textos do PDF e o contrato entre as partes | — | — |
| [`integrantes.txt`](integrantes.txt) | o `.txt` da entrega: grupo, RM, nomes e link do pitch | — | a preencher |

**O contrato entre as partes** está em [`docs/REGRAS.md`](docs/REGRAS.md): regras de alerta, usuários de teste, o papel de cada vaca da carga e os casos de teste. Os nomes de tabelas, campos e rotas são os mesmos em todas as partes: `usuario`, `propriedade`, `vaca`, `ordenha`, `alerta` e `tratamento`.

## Rodar tudo junto (resumo)

Os detalhes estão no README de cada parte.

**1. Banco e API.** Precisa de Java 17+ e Docker. Dentro de `api/`:

```bash
docker compose up -d
```
```bash
./mvnw spring-boot:run
```

Depois, abra o Swagger em http://localhost:8080/swagger-ui.html e faça login com `joao@exemplo.com` / `senha123`.

**2. Simulador dos sensores enviando para a API.** Precisa de Python 3.8+. Dentro de `iot/`:

```bash
python simulador_sensores.py --propriedade 1 --doente 1 --api http://localhost:8080 --token SEU_TOKEN
```

**3. Testes automatizados.** Os da API rodam dentro de `api/`, e os do IoT dentro de `iot/`:

```bash
./mvnw test
```
```bash
python -m unittest -v test_regras.py
```

## Vídeo pitch

Link: _a definir_ (público; testar numa aba anônima antes de entregar)

## Créditos

- **Inspiração do sensor:** PORTUGAL, G.; GUEDES, G. *MastitAlert: towards detection of mastitis during the act of milking.* BreSci 2021, SBC. DOI 10.5753/bresci.2021.15800.
- **Demais fontes:** em [`docs/texto_pdf_parte1.md`](docs/texto_pdf_parte1.md).
- **Dados:** todos os dados são fictícios ou simulados.

## Uso de IA generativa

<!-- Cada integrante confere e completa com o que usou na própria parte. -->
O grupo usou o Claude (Anthropic) como apoio para pesquisar as fontes, estruturar o código, revisar e escrever a documentação. Todo o código foi revisado, executado e testado pelos integrantes, e cada um explica a própria parte.
