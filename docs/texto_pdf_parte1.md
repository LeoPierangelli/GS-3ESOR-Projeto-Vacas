# Textos para o PDF de entrega · Parte 1 (Banco de dados) e Parte 6 (Simulação de IoT)

> Para a pessoa 4: estes são os textos das partes 1 e 6, prontos para colar no PDF.
> A imagem do diagrama está em `banco/diagrama_er.png`.
> Os prints das consultas devem ser tirados no MySQL Workbench depois de rodar os scripts.

---

## Parte 1 · Banco de dados

### 1.1 Diagrama Entidade-Relacionamento

*(inserir `banco/diagrama_er.png`)*

O modelo tem **6 entidades** em MySQL 8 (InnoDB, utf8mb4). É o banco oficial do projeto e o mesmo que a API usa:

- **USUARIO**: o produtor que acessa o app. O email é único, e a senha é guardada apenas como hash BCrypt.
- **PROPRIEDADE**: o sítio ou fazenda. Um usuário possui de 0 a N propriedades (1:N).
- **VACA**: o animal, identificado pelo brinco. O brinco é único dentro da propriedade (UNIQUE composto `propriedade_id + brinco`). Uma propriedade tem de 0 a N vacas.
- **ORDENHA**: a leitura dos dois sensores (litros e condutividade) de uma vaca em uma ordenha. Uma vaca gera de 0 a N ordenhas, e não pode haver duas leituras da mesma vaca no mesmo instante.
- **ALERTA**: gerado quando uma ordenha dispara uma regra. Cada ordenha dispara de 0 a 3 alertas, no máximo um de cada tipo. O alerta pertence a uma ordenha e à vaca dessa ordenha.
- **TRATAMENTO**: medicamento aplicado na vaca, com o período de carência do leite. Uma vaca recebe de 0 a N tratamentos. A data de fim fica vazia enquanto o tratamento não termina.

**Decisões de modelagem**
- **`vaca_id` em ALERTA, protegido por FK composta.** A API lista e conta alertas direto pela vaca, então a coluna existe. Isso repete uma informação que também está na ordenha, o que é uma desnormalização controlada. Para ela nunca ficar inconsistente, a chave estrangeira é composta: `(ordenha_id, vaca_id)` precisa existir em `ordenha (id, vaca_id)`. O banco recusa um alerta que aponte para uma vaca diferente da vaca da ordenha.
- **"Suspeita" não é status da vaca.** O status guarda só o estado de manejo (ATIVA, EM_TRATAMENTO, SECA). A suspeita é derivada de existir alerta aberto, então nunca fica desatualizada.
- **Restrições de integridade:** NOT NULL nos campos obrigatórios; UNIQUE em email, em (propriedade, brinco), em (vaca, data_hora) e em (ordenha, tipo); CHECK em email, litros (> 0 e < 1000, o mesmo limite da API), condutividade (> 0), turno, status, tipo de alerta, UF, resolvido (0/1), hash da senha, período do tratamento (fim vazio ou ≥ início) e dias de carência (0 a 999). As restrições foram escolhidas para nunca recusar um dado que a API aceita.
- **Exclusão em cascata** de ordenhas, alertas e tratamentos quando uma vaca é removida, para que a operação `DELETE /vacas/{id}` da API não falhe por chave estrangeira.

### 1.2 Script SQL

- `01_criar_tabelas.sql`: cria o banco `ordenha` (o mesmo da API), apaga as tabelas do projeto se existirem (sem dar erro na primeira execução) e cria as 6 tabelas com PK, FK, constraints, índices e comentários. Os IDs são `BIGINT AUTO_INCREMENT`.
- `02_carga_inicial.sql`: 2 usuários, 2 propriedades, 8 vacas, 2 tratamentos, 140 ordenhas (10 dias × 2 turnos × 7 vacas em lactação) e 21 alertas (7 abertos). Os usuários e as vacas são os mesmos da documentação da API.
  - **As datas são relativas ao dia da execução** (`CURDATE() - INTERVAL n DAY`), então as consultas de "últimos 7 dias" e "carência hoje" sempre têm resultado, em qualquer dia em que o professor rodar o script.
  - **Os alertas da carga foram calculados pelas mesmas regras que a API aplica.** A carga conta uma história: a BR-003 (Mimosa) com mastite em desenvolvimento, a SL-010 (Pintada) em tratamento e carência, a BR-004 (Estrela) com queda de produção e a SL-011 (Faceira) com um falso positivo já resolvido.
- `03_consultas.sql`: 6 consultas de uso real.

### 1.3 Consultas de uso

| # | Pergunta do produtor | Recursos SQL | Resultado com a carga |
|---|---|---|---|
| 1 | Quais vacas mais produziram nos últimos 7 dias, em cada propriedade? | JOIN de 3 tabelas, SUM, AVG, COUNT, GROUP BY, RANK() | no Sítio Boa Vista, a BR-002 lidera e a BR-003 (mastite) fica entre as últimas |
| 2 | Qual a última condutividade de cada vaca comparada com a média dela? | CTE, ROW_NUMBER(), AVG/MAX condicionais, CASE | BR-003 em primeiro, 15% acima da média: SUSPEITA no sistema e ALARME no aparelho |
| 3 | Quantos alertas estão abertos, por propriedade e tipo? | JOIN de 4 tabelas, COUNT, COUNT DISTINCT, MIN, MAX, GROUP_CONCAT | Sítio Boa Vista: mastite (BR-003) e queda (BR-004); Fazendinha Santa Luzia: carência (SL-010) |
| 4 | Que vacas estão em carência hoje e quando o leite volta ao tanque? | JOIN, INTERVAL/DATEDIFF, MAX, SUM, COUNT, GROUP_CONCAT | SL-010, com o leite voltando ao tanque em 3 dias |
| 5 | Quanto leite cada propriedade tirou por dia e quanto foi descartado? | CTE com EXISTS, SUM condicional, COUNT DISTINCT, GROUP BY dia | leite descartado na Fazendinha Santa Luzia a partir do início do tratamento |
| 6 | Que vacas tiveram mais de um tratamento em 12 meses? | JOIN, COUNT, HAVING, DATEDIFF, COALESCE, GROUP_CONCAT | SL-010, com 2 tratamentos |

*(colar o print de cada consulta executada no MySQL Workbench)*

---

## Parte 6 · Simulação de IoT

### 6.1 Inspiração: o protótipo MastitAlert

O simulador foi inspirado no **MastitAlert** (Portugal & Guedes, 2021), um protótipo da Universidade Católica de Petrópolis para medir a condutividade elétrica do leite **durante** a ordenha. O mercado normalmente faz essa medição depois da ordenha ou em laboratório.
- **Hardware do protótipo:** um Arduino Uno, um display LCD 16×2, um resistor de 10 kΩ e dois fios em contato com o leite. Um sinal de 5 V atravessa o leite, e o Arduino lê um valor analógico de 0 a 1023, que três equações convertem em milisiemens (mS).
- **Decisão:** acima do limite de 0,537 mS, o aparelho emite um aviso sonoro e visual.
- **Proposta futura do artigo:** só deixar o leite seguir para o cano de saída quando não houver mastite.

**O nosso simulador reproduz esse funcionamento em software**, com o devido crédito ao trabalho original. Não usamos hardware real.

### 6.2 Os sensores

| Sensor | O que mede | Unidade | Como é simulado | Como influencia o sistema |
|---|---|---|---|---|
| Medidor de volume | Litros de leite da vaca na ordenha | L | Contador de pulsos (cada litro gera um número fixo de pulsos) | Alimenta o histórico de produção e a regra de **queda de produção** |
| Sensor de condutividade | Condutividade elétrica do leite, que sobe quando há inflamação na glândula mamária | mS | Mesmo circuito e mesmas equações do MastitAlert: gera o valor bruto de 0 a 1023 e converte em mS | Dispara a **suspeita de mastite**, aciona a válvula de desvio do leite e entra no histórico da vaca |

**Por que a condutividade indica mastite?** Na inflamação, a composição do leite muda: aumentam os íons de sódio e cloro e diminuem o potássio e a lactose. Isso aumenta a condutividade elétrica (Afimilk).

**As equações do MastitAlert, aplicadas pelo simulador:**

```
V  = (5 × leitura) / 1023          tensão lida (leitura de 0 a 1023)
R  = (5 × 1000) / V − 1000         resistência do leite (1000 = constante do artigo)
mS = (1 / R) × 1000                condutividade em milisiemens
```

### 6.3 Como os dados disparam ações

Cada leitura passa por **duas camadas**:

**1. Alarme do aparelho, como no MastitAlert.** Com condutividade ≥ 0,537 mS:
- o "display" mostra o aviso e o "buzzer" toca;
- a **válvula desvia o leite** para o descarte, como previsto no artigo.

Tudo isso acontece na hora, sem depender do histórico nem da internet.

**2. Alertas do sistema, as mesmas três regras da API:**
1. **Suspeita de mastite:** condutividade 15% acima da **média das 7 ordenhas anteriores da própria vaca**, como fazem os sistemas comerciais.
2. **Queda de produção:** 3 ordenhas seguidas abaixo de 80% da produção de referência da vaca.
3. **Leite em carência:** ordenha de vaca em tratamento ou dentro da carência do medicamento. Nesse caso, a válvula também desvia o leite.

**Por que duas camadas?** Uma cobre o ponto fraco da outra:
- o limite fixo erra em vacas que naturalmente têm condutividade mais alta;
- a comparação com a média demora a perceber uma vaca que piora aos poucos, porque a própria média vai subindo junto.

Na carga de exemplo, a BR-003 mostra isso: depois de alguns dias de alta, a regra do sistema para de disparar, mas o aparelho continua apitando.

**No modo integrado**, a leitura vai para `POST /ordenhas` da API, que grava a ordenha, aplica as regras e cria o alerta que aparece no app. O simulador confere se os alertas que a API gerou são os mesmos que ele previu.

**Robustez:** o artigo relata medições erradas com o leite frio (cerca de 10 °C), porque a gordura se separa. Por isso, o simulador **descarta leituras com o leite abaixo de 30 °C** em vez de gravar um dado errado. É uma forma de a solução continuar útil fora do cenário ideal.

### 6.4 Por que comparar com a própria vaca (e por que é só uma suspeita)

- **A condutividade normal varia de vaca para vaca.** Num estudo da APTA com mais de 1.100 vacas Holandesas, a média foi de cerca de 4,8 mS/cm, com variação individual relevante. O mesmo estudo destaca que uma **variação brusca entre ordenhas** é o que sinaliza o início da doença. Por isso a regra principal compara cada vaca com o próprio histórico.
- **A condutividade sozinha erra.** Um protótipo on-line avaliado por Lansbergen et al. (1994) teve sensibilidade de 25% e especificidade de 99%. Por isso o sistema emite uma **suspeita** e orienta o produtor a confirmar com o teste da caneca ou o CMT. Na carga de exemplo, a vaca SL-011 (Faceira) mostra um falso positivo confirmado e resolvido.
- **A mastite subclínica é comum:** num estudo publicado na Ars Veterinaria, a prevalência de mastite subclínica foi de cerca de 14,5% das vacas avaliadas.

> **Escala e limites do MVP.** Os valores em mS seguem a escala do protótipo MastitAlert, cujo eletrodo não é calibrado em mS/cm. Os percentuais das regras (15%, 20%, 3 ordenhas) são **valores iniciais do MVP, não parâmetros veterinários validados**, e ficam como constantes configuráveis no código.

### 6.5 Como rodar

Ver `iot/README.md` e `banco/README.md`.

---

## Fontes e créditos

- PORTUGAL, G.; GUEDES, G. *MastitAlert: towards detection of mastitis during the act of milking.* In: Brazilian e-Science Workshop (BreSci), 15., 2021. Anais. Porto Alegre: SBC, 2021. p. 141-144. DOI 10.5753/bresci.2021.15800. https://sol.sbc.org.br/index.php/bresci/article/view/15800
- SOUZA, V. et al. *Uso da condutividade elétrica do leite para detecção de mastite subclínica caprina.* Embrapa Caprinos e Ovinos, Comunicado Técnico 192, 2019. https://www.infoteca.cnptia.embrapa.br/infoteca/handle/doc/1116655
- VERCESI FILHO, A. E.; EL FARO, L.; VILLAS BOAS, D. F. *Estudo da condutividade elétrica como característica indicadora de mastite em rebanhos leiteiros.* Pesquisa & Tecnologia, v. 10, n. 1, 2013. https://repositorio-aptaregional.agricultura.sp.gov.br/bitstreams/a36b9187-0d17-4394-8538-ea80ed262c0a/download
- NASCIF JÚNIOR, I. A.; NADER FILHO, A.; OLIVEIRA, J. A. *Sensibilidade e especificidade do California Mastitis Test (CMT) e da condutividade elétrica do leite de vacas durante a lactação.* Ars Veterinaria, v. 22, n. 2, p. 135-137, 2006. https://arsveterinaria.org.br/index.php/ars/article/download/92/81/262
- AFIMILK. *Afimilk and Electrical Conductivity.* https://www.afimilk.com/?p=3696
- LANSBERGEN, L. M. T. E. et al. *Evaluation of a prototype on-line electrical conductivity system for detection of subclinical mastitis.* Journal of Dairy Science, v. 77, n. 4, p. 1132-1140, 1994. https://agris.fao.org/search/en/records/65deb0ae4c5aef494fdd50cd
