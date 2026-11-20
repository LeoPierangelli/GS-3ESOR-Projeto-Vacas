# Ordenha Monitorada: Simulação de IoT

Simulador dos dois sensores da ordenha (volume e condutividade elétrica do leite), inspirado no protótipo **MastitAlert** (Portugal & Guedes, 2021).
Funciona sozinho ou integrado à API (`POST /ordenhas`). Precisa de Python 3.8+ e não instala nada. No Linux e no WSL o comando é `python3`; no Windows, `python`.

## Como rodar (dentro da pasta `iot/`)

```bash
python3 simulador_sensores.py --reset                  # recria o histórico igual à carga do banco + 1 ordenha
python3 simulador_sensores.py --doente 1               # vaca 1 com mastite: aparelho apita, sistema alerta, válvula desvia
python3 simulador_sensores.py --doente 1 --sessoes 3   # 3 ordenhas seguidas: dispara também a queda de produção
python3 simulador_sensores.py --leite-frio 2           # leitura da vaca 2 descartada (leite frio)
python3 -m unittest -v test_regras.py                  # 22 testes automatizados
```

**Integrado com a API** (diferencial):
1. Suba o banco e a API (ver `api/README.md`).
2. Faça o login no Swagger com `joao@exemplo.com` / `senha123` e copie o token.
3. Rode o simulador:
   ```bash
   python3 simulador_sensores.py --propriedade 1 --doente 1 --api http://localhost:8080 --token SEU_TOKEN
   ```

Cada leitura vai para `POST /ordenhas`. O simulador mostra os alertas que a API gerou de verdade e confere se são os mesmos que ele previu. Use `--propriedade 1` com o login do João: as vacas da propriedade 2 são da Maria, e a API responde 404 para elas.

## O que acontece em cada leitura

1. **Sinais brutos:** o medidor de volume gera pulsos, e o sensor de condutividade gera o valor de 0 a 1023 do Arduino. A conversão para mS usa as 3 equações do MastitAlert.
2. **Leite frio:** abaixo de 30 °C, a leitura é descartada, porque o artigo relata medição errada com leite a cerca de 10 °C.
3. **Alarme do aparelho:** com condutividade ≥ 0,537 mS (o limite do MastitAlert), o simulador mostra o "display 16×2", toca o buzzer e a válvula desvia o leite.
4. **Alertas do sistema:** as 3 regras da API, que comparam a leitura com o histórico da vaca. Estão em `regras.py`, que reproduz o `RegrasAlertaService`.
5. **Arquivos de saída:** tudo fica gravado em `saida/leituras.jsonl` e `saida/alertas.jsonl`.

| Arquivo | O que é |
|---|---|
| `simulador_sensores.py` | o simulador (o que se roda) |
| `sensores.py` | modelo dos sensores: equações do MastitAlert, medidor de volume, alarme do aparelho |
| `regras.py` | as 3 regras de alerta, iguais às da API |
| `test_regras.py` | testes automatizados |
| `dados/` | rebanho e histórico iniciais (gerados junto com a carga do banco) |

## Créditos

- **Sensor de condutividade inspirado em:** PORTUGAL, G.; GUEDES, G. *MastitAlert: towards detection of mastitis during the act of milking.* BreSci 2021, SBC, p. 141-144. DOI 10.5753/bresci.2021.15800.
- **Demais referências:** na seção "Fontes e créditos" do [PDF de entrega](../Ordenha_Monitorada.pdf).
- **Dados:** todos os dados são simulados.

## Uso de IA generativa

O banco e a simulação de IoT foram desenvolvidos com apoio do Claude (Anthropic) para pesquisar as fontes, estruturar o modelo, gerar o código e revisar os textos. Todo o código foi revisado e testado pelo integrante responsável.
