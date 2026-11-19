"""
Simulador dos sensores da Ordenha Monitorada, inspirado no MastitAlert.

Cada vaca em lactação passa pela ordenha e gera uma leitura com dois sensores:
  * medidor de volume (litros), modelado como contador de pulsos;
  * sensor de condutividade elétrica (mS), com o mesmo circuito e as mesmas
    equações do protótipo MastitAlert (Portugal & Guedes, 2021).

Para cada leitura o simulador:
  1. gera os sinais brutos (pulsos e valor analógico 0..1023) e converte;
  2. descarta a leitura se o leite estiver frio (o artigo relata leitura errada
     com leite a ~10 °C, porque a gordura se separa);
  3. ALARME DO APARELHO (como o MastitAlert): acima de 0,537 mS toca o buzzer,
     mostra o aviso no display e a válvula desvia o leite, na hora;
  4. ALERTAS DO SISTEMA: aplica as três regras da API (iot/regras.py reproduz o
     RegrasAlertaService), que comparam a vaca com o histórico dela;
  5. grava a leitura em saida/leituras.jsonl e os alertas em saida/alertas.jsonl;
  6. opcionalmente envia a leitura para POST /ordenhas da API (--api) e mostra
     os alertas que a API gerou de verdade.

Exemplos:
  python simulador_sensores.py                      uma ordenha normal do rebanho
  python simulador_sensores.py --doente 1           vaca 1 com sinais de mastite
  python simulador_sensores.py --doente 1 --sessoes 3   (3 ordenhas: dispara a queda)
  python simulador_sensores.py --leite-frio 2       leitura da vaca 2 descartada
  python simulador_sensores.py --reset              volta ao histórico da carga
  python simulador_sensores.py --propriedade 1 --doente 1 --api http://localhost:8080 --token SEU_TOKEN

Só usa a biblioteca padrão do Python 3.8+.
"""
from __future__ import annotations

import argparse
import json
import random
import sys
import urllib.error
import urllib.request
from datetime import date, datetime, time, timedelta
from pathlib import Path

PASTA = Path(__file__).resolve().parent
sys.path.insert(0, str(PASTA))

import regras    # noqa: E402
import sensores  # noqa: E402

DADOS = PASTA / "dados"
SAIDA = PASTA / "saida"
ESTADO = SAIDA / "estado.json"

FATOR_TURNO = {"MANHA": 1.06, "TARDE": 0.94}
RUIDO_LITROS, RUIDO_COND = 0.03, 0.025
DOENTE_COND, DOENTE_LITROS = 1.30, 0.72   # efeito do modo --doente

if hasattr(sys.stdout, "reconfigure"):     # acentos no console do Windows
    sys.stdout.reconfigure(encoding="utf-8")


# ---------------------------------------------------------------------------
# Estado local (histórico de cada vaca, tratamentos e alertas abertos)
# ---------------------------------------------------------------------------
def estado_inicial() -> dict:
    """Converte o histórico da carga (dias relativos) em datas a partir de hoje."""
    h = json.loads((DADOS / "historico_inicial.json").read_text(encoding="utf-8"))
    hoje = date.today()
    ordenhas = []
    for o in h["ordenhas"]:
        hh, mm = map(int, o["hora"].split(":"))
        dt = datetime.combine(hoje + timedelta(days=o["diaRelativo"]), time(hh, mm))
        ordenhas.append({"vacaId": o["vacaId"], "dataHora": dt.isoformat(timespec="seconds"),
                         "turno": o["turno"], "litros": o["litros"],
                         "condutividade": o["condutividade"]})
    tratamentos = [{"vacaId": t["vacaId"], "medicamento": t["medicamento"],
                    "dataInicio": (hoje + timedelta(days=t["inicioRelativo"])).isoformat(),
                    "dataFim": (None if t["fimRelativo"] is None
                                else (hoje + timedelta(days=t["fimRelativo"])).isoformat()),
                    "diasCarencia": t["diasCarencia"]} for t in h["tratamentos"]]
    return {"criadoEm": hoje.isoformat(), "ordenhas": ordenhas,
            "tratamentos": tratamentos, "alertasAbertos": h["alertasAbertos"]}


def carregar_estado(reset: bool) -> dict:
    SAIDA.mkdir(exist_ok=True)
    if reset or not ESTADO.exists():
        estado = estado_inicial()
        salvar_estado(estado)
        for nome in ("leituras.jsonl", "alertas.jsonl"):
            (SAIDA / nome).unlink(missing_ok=True)
        print("Histórico local recriado a partir da carga inicial do banco.\n")
        return estado
    return json.loads(ESTADO.read_text(encoding="utf-8"))


def salvar_estado(estado: dict):
    ESTADO.write_text(json.dumps(estado, ensure_ascii=False, indent=1), encoding="utf-8")


def anexar(nome: str, registro: dict):
    with open(SAIDA / nome, "a", encoding="utf-8") as f:
        f.write(json.dumps(registro, ensure_ascii=False) + "\n")


# ---------------------------------------------------------------------------
# Saída no estilo do aparelho
# ---------------------------------------------------------------------------
def lcd(linha1: str, linha2: str) -> str:
    l1, l2 = linha1[:16].ljust(16), linha2[:16].ljust(16)
    return f"    +----------------+\n    |{l1}|\n    |{l2}|\n    +----------------+"


def enviar_api(url_base: str, token: str | None, payload: dict, previstos: list[str]):
    """POST /ordenhas. A resposta da API traz a ordenha gravada e a lista alertasGerados."""
    req = urllib.request.Request(
        url_base.rstrip("/") + "/ordenhas", method="POST",
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json",
                 **({"Authorization": f"Bearer {token}"} if token else {})})
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            corpo = r.read().decode("utf-8", "replace")
        try:
            resposta = json.loads(corpo)
            gerados = sorted(a.get("tipo") for a in resposta.get("alertasGerados", []))
            ordenha_id = (resposta.get("ordenha") or {}).get("id")
            confere = "igual ao previsto" if gerados == sorted(previstos) else f"previsto: {sorted(previstos)}"
            print(f"    API: {r.status} · ordenha {ordenha_id} gravada · alertas gerados: "
                  f"{gerados or 'nenhum'} ({confere})")
        except (ValueError, AttributeError):
            print(f"    API: {r.status} {corpo[:300]}")
    except urllib.error.HTTPError as e:
        dica = {401: " (faça o login e passe o token com --token)",
                404: " (vaca de outra propriedade? use --propriedade com a do usuário logado)"}.get(e.code, "")
        print(f"    API: {e.code}{dica} {e.read().decode('utf-8', 'replace')[:200]}")
    except urllib.error.URLError as e:
        print(f"    API: sem conexão com {url_base} ({e.reason}). A leitura ficou só no arquivo local.")


# ---------------------------------------------------------------------------
def main():
    ap = argparse.ArgumentParser(description="Simulador dos sensores da ordenha (inspirado no MastitAlert).")
    ap.add_argument("--sessoes", type=int, default=1, help="quantas ordenhas seguidas simular (padrão 1)")
    ap.add_argument("--doente", type=int, action="append", default=[], metavar="VACA_ID",
                    help="vaca com mastite: condutividade +30%% e litros -28%% (pode repetir)")
    ap.add_argument("--leite-frio", type=int, action="append", default=[], metavar="VACA_ID",
                    help="simula leite frio nessa vaca: a leitura é descartada")
    ap.add_argument("--propriedade", type=int, help="só as vacas dessa propriedade")
    ap.add_argument("--api", help="URL base da API, ex.: http://localhost:8080 (envia para POST /ordenhas)")
    ap.add_argument("--token", help="token do login, enviado como 'Authorization: Bearer'")
    ap.add_argument("--semente", type=int, help="semente aleatória, para repetir exatamente a simulação")
    ap.add_argument("--reset", action="store_true", help="recria o histórico local a partir da carga do banco")
    args = ap.parse_args()

    rnd = random.Random(args.semente)
    rebanho = json.loads((DADOS / "rebanho.json").read_text(encoding="utf-8"))
    estado = carregar_estado(args.reset)
    vacas = [v for v in rebanho if v["status"] != "SECA"
             and (args.propriedade is None or v["propriedadeId"] == args.propriedade)]
    ids_validos = {v["id"] for v in rebanho}
    for vid in args.doente + args.leite_frio:
        if vid not in ids_validos:
            sys.exit(f"Vaca {vid} não existe no rebanho (ids válidos: {sorted(ids_validos)}).")

    agora = datetime.now().replace(microsecond=0)
    turno = "MANHA" if agora.hour < 12 else "TARDE"
    resumo = {"leituras": 0, "descartadas": 0, "alertas": 0, "desviadas": 0}

    for s in range(args.sessoes):
        # Leituras sempre no passado recente (a API recusa data futura):
        # cada sessão fica 1 minuto depois da anterior e cada vaca 3 s depois da outra.
        inicio = agora - timedelta(minutes=args.sessoes - s)
        print(f"=== Ordenha {s + 1}/{args.sessoes} · {inicio:%d/%m/%Y %H:%M} · turno {turno} ===")
        for k, v in enumerate(vacas):
            vid = v["id"]
            perfil = v["perfil"]
            momento = inicio + timedelta(seconds=3 * k)
            doente = vid in args.doente
            # A vaca continua a história da carga (ex.: a BR-003 segue com condutividade
            # alta e a BR-004 com produção baixa); --doente piora a vaca escolhida.
            litros_real = (perfil["litrosBase"] * perfil.get("fatorLitrosAtual", 1)
                           * FATOR_TURNO[turno] * rnd.gauss(1, RUIDO_LITROS)
                           * (DOENTE_LITROS if doente else 1))
            cond_real = (perfil["condutividadeBaseMs"] * perfil.get("fatorCondutividadeAtual", 1)
                         * rnd.gauss(1, RUIDO_COND) * (DOENTE_COND if doente else 1))
            temperatura = rnd.gauss(10.5, 0.8) if vid in args.leite_frio else rnd.gauss(35.0, 0.7)
            m = sensores.medir(cond_real, litros_real)

            print(f"\n  Vaca {vid} · brinco {v['brinco']} · {v['nome']}"
                  f"{'  [modo doente]' if doente else ''}")
            print(f"    sinais brutos: medidor de volume = {m['pulsos']} pulsos | "
                  f"condutividade = {m['adc']}/1023 | leite a {temperatura:.1f} °C")

            if temperatura < sensores.TEMPERATURA_MINIMA_C:
                print(lcd(f"{v['brinco']} FRIO", "LEITURA IGNORADA"))
                print(f"    Leitura descartada: abaixo de {sensores.TEMPERATURA_MINIMA_C:.0f} °C a gordura "
                      "se separa e a condutividade fica errada. Nada foi gravado.")
                resumo["descartadas"] += 1
                continue

            payload = {"vacaId": vid, "dataHora": momento.isoformat(timespec="seconds"),
                       "turno": turno, "litros": m["litros"], "condutividade": m["condutividade"]}
            print("    leitura enviada: " + json.dumps(payload, ensure_ascii=False))

            hist = [o for o in estado["ordenhas"] if o["vacaId"] == vid]
            trats = [{"data_inicio": date.fromisoformat(t["dataInicio"]),
                      "data_fim": date.fromisoformat(t["dataFim"]) if t.get("dataFim") else None,
                      "dias_carencia": t["diasCarencia"], "medicamento": t["medicamento"]}
                     for t in estado["tratamentos"] if t["vacaId"] == vid]
            abertos = [a["tipo"] for a in estado["alertasAbertos"] if a["vacaId"] == vid]
            disparos = regras.avaliar(hist, {"data_hora": momento, "litros": m["litros"],
                                             "condutividade": m["condutividade"]}, trats, abertos)

            # 1) Alarme do aparelho (MastitAlert): limite fixo, sem depender do histórico.
            alarme = sensores.alarme_aparelho(m["condutividade"])
            em_carencia = any(d.tipo == regras.LEITE_EM_CARENCIA for d in disparos)
            linha2 = ("ALERTA MASTITE!" if alarme else "LEITE CARENCIA" if em_carencia
                      else f"{m['litros']:.1f}L  OK")
            print(lcd(f"{v['brinco']} {m['condutividade']:.3f}mS", linha2))
            if alarme:
                print(f"    APARELHO: BUZZER bip-bip · {m['condutividade']:.3f} mS >= "
                      f"{sensores.ALARME_APARELHO_MS} mS (limite do MastitAlert)")

            # 2) Alertas do sistema: as regras da API, com o histórico da vaca.
            gravados = []
            for d in disparos:
                if d.novo:
                    print(f"    SISTEMA: alerta {d.tipo} · {d.mensagem}")
                    gravados.append(d.tipo)
                    estado["alertasAbertos"].append({"vacaId": vid, "tipo": d.tipo})
                    anexar("alertas.jsonl", {"vacaId": vid, "brinco": v["brinco"], "tipo": d.tipo,
                                             "mensagem": d.mensagem, "motivo": d.motivo,
                                             "dataHora": payload["dataHora"]})
                    resumo["alertas"] += 1
                else:
                    print(f"    SISTEMA: {d.tipo} já está aberto para a vaca (a API não repete a queda).")
                print(f"             motivo: {d.motivo}")

            desviar = alarme or any(d.tipo in regras.TIPOS_QUE_DESVIAM_LEITE for d in disparos)
            if desviar:
                print("    VÁLVULA: leite DESVIADO para o balde de descarte (não vai ao tanque).")
                resumo["desviadas"] += 1

            estado["ordenhas"].append(payload)
            anexar("leituras.jsonl", {**payload, "sensores": {"pulsos": m["pulsos"], "adc": m["adc"],
                                                                "temperaturaC": round(temperatura, 1)},
                                      "alarmeAparelho": alarme, "alertasSistema": gravados,
                                      "leiteDesviado": desviar})
            resumo["leituras"] += 1
            if args.api:
                enviar_api(args.api, args.token, payload, gravados)
        print()

    salvar_estado(estado)
    print(f"Resumo: {resumo['leituras']} leituras gravadas, {resumo['descartadas']} descartadas, "
          f"{resumo['alertas']} alertas novos, {resumo['desviadas']} com leite desviado.")
    print(f"Arquivos: {SAIDA / 'leituras.jsonl'} e {SAIDA / 'alertas.jsonl'}")


if __name__ == "__main__":
    main()
