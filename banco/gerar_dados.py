"""
Gera, a partir de UMA única definição do rebanho, todos os dados de exemplo:

  banco/02_carga_inicial.sql          INSERTs da carga inicial (MySQL)
  mobile/app/src/main/java/com/ordenha/monitorada/DadosCarga.kt
                                      mock do app Android (a mesma carga, em Kotlin)
  iot/dados/rebanho.json              perfil de cada vaca, usado pelo simulador
  iot/dados/historico_inicial.json    histórico que o simulador usa como ponto de partida

Assim o banco, a API, o app e o simulador mostram as mesmas vacas, com os
mesmos IDs e brincos, e os alertas da carga são calculados pelas mesmas
regras da API (iot/regras.py reproduz o RegrasAlertaService).

Usuários, propriedades e vacas são os mesmos da carga provisória da API
(api/README.md), para o Swagger, a Postman Collection e o README da API
continuarem valendo.

As datas do .sql são relativas a CURDATE(): a carga cobre os 10 dias
anteriores ao dia em que o script for executado.

Uso (na raiz do repositório):  python banco/gerar_dados.py

O Docker da API lê os scripts direto desta pasta (api/docker-compose.yml),
então não há cópia para manter em dia.
Só usa a biblioteca padrão do Python. A semente é fixa.
"""
from __future__ import annotations

import json
import random
import sys
from datetime import date, datetime, time, timedelta
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(RAIZ / "iot"))

import regras    # noqa: E402
import sensores  # noqa: E402

SEMENTE = 2026
DIAS = 10                      # dias de ordenha na carga (de -10 a -1)
FATOR_TURNO = {"MANHA": 1.06, "TARDE": 0.94}
HORA_TURNO = {"MANHA": (6, 0), "TARDE": (16, 30)}
RUIDO_LITROS = 0.03            # desvio padrão relativo
RUIDO_COND = 0.025

# Mesmos usuários da API. Senha dos dois: senha123 (BCrypt, custo 10, $2a$).
USUARIOS = [
    {"id": 1, "nome": "João Ferreira", "email": "joao@exemplo.com",
     "senha_hash": "$2a$10$dJLHVPXnJCFg2aOsSeMXkuGM0cHLJ1x5w1ZfXJGY.gg2q.GtbdWta"},
    {"id": 2, "nome": "Maria Souza", "email": "maria@exemplo.com",
     "senha_hash": "$2a$10$yv/JYJu1n4mCM0SbBcfUqO/NxVYjDnDuLnaoEBPkxFrkpapUekcZK"},
]
SENHA_TESTE = "senha123"

PROPRIEDADES = [
    {"id": 1, "usuario_id": 1, "nome": "Sítio Boa Vista", "municipio": "Itamonte", "uf": "MG"},
    {"id": 2, "usuario_id": 2, "nome": "Fazendinha Santa Luzia", "municipio": "Pouso Alegre", "uf": "MG"},
]


def mult(pontos: dict[int, float]):
    """Multiplicador por índice de ordenha (0 a 19); fora dos pontos vale 1.0."""
    return lambda i: pontos.get(i, 1.0)


# Índice da ordenha: 0 = dia -10 manhã, 1 = dia -10 tarde, ..., 19 = dia -1 tarde.
# Condutividade em mS (escala do MastitAlert); litros por ordenha.
VACAS = [
    {"id": 1, "propriedade_id": 1, "brinco": "BR-001", "nome": "Bonita",
     "data_nascimento": "2020-03-12", "status": "ATIVA", "litros": 7.5, "cond": 0.42,
     "papel": "Normal"},
    {"id": 2, "propriedade_id": 1, "brinco": "BR-002", "nome": "Malhada",
     "data_nascimento": "2019-08-02", "status": "ATIVA", "litros": 9.0, "cond": 0.40,
     "papel": "Normal"},
    {"id": 3, "propriedade_id": 1, "brinco": "BR-003", "nome": "Mimosa",
     "data_nascimento": "2021-01-20", "status": "ATIVA", "litros": 6.5, "cond": 0.43,
     "papel": "Condutividade subindo nos últimos dias: alertas de SUSPEITA_MASTITE abertos",
     "m_cond": mult({14: 1.05, 15: 1.10, 16: 1.22, 17: 1.28, 18: 1.33, 19: 1.38}),
     "m_litros": mult({14: 0.97, 15: 0.95, 16: 0.92, 17: 0.90, 18: 0.88, 19: 0.87})},
    {"id": 4, "propriedade_id": 1, "brinco": "BR-004", "nome": "Estrela",
     "data_nascimento": "2018-11-05", "status": "ATIVA", "litros": 8.0, "cond": 0.39,
     "papel": "Produção caindo nas 4 últimas ordenhas: QUEDA_PRODUCAO aberta",
     "m_litros": mult({16: 0.74, 17: 0.72, 18: 0.71, 19: 0.70})},
    {"id": 5, "propriedade_id": 1, "brinco": "BR-005", "nome": "Princesa",
     "data_nascimento": "2022-02-14", "status": "ATIVA", "litros": 6.0, "cond": 0.41,
     "papel": "Normal"},
    {"id": 6, "propriedade_id": 2, "brinco": "SL-010", "nome": "Pintada",
     "data_nascimento": "2020-06-30", "status": "EM_TRATAMENTO", "litros": 9.5, "cond": 0.44,
     "papel": "Mastite nos dias -8/-7, tratada desde o dia -6: LEITE_EM_CARENCIA aberto; "
              "2 tratamentos em 12 meses",
     "m_cond": mult({5: 1.38, 6: 1.42, 7: 1.45, 8: 1.18, 9: 1.10, 10: 1.05, 11: 1.02}),
     "m_litros": mult({5: 0.92, 6: 0.88, 7: 0.86, 8: 0.88, 9: 0.92, 10: 0.95, 11: 0.97})},
    {"id": 7, "propriedade_id": 2, "brinco": "SL-011", "nome": "Faceira",
     "data_nascimento": "2021-09-09", "status": "ATIVA", "litros": 7.0, "cond": 0.38,
     "papel": "Pico isolado no dia -4: alerta resolvido (CMT negativo, falso positivo)",
     "m_cond": mult({12: 1.22})},
    {"id": 8, "propriedade_id": 2, "brinco": "SL-012", "nome": "Serena",
     "data_nascimento": "2017-04-18", "status": "SECA", "litros": 0, "cond": 0.40,
     "papel": "Seca: não é ordenhada"},
]

TRATAMENTOS = [
    {"id": 1, "vaca_id": 6, "medicamento": "Anti-inflamatório",
     "inicio": -48, "fim": -46, "dias_carencia": 2,
     "observacao": "Tratamento anterior, já liberado. Dados fictícios."},
    {"id": 2, "vaca_id": 6, "medicamento": "Cefalexina intramamária",
     "inicio": -6, "fim": -2, "dias_carencia": 4,
     "observacao": "Quarto posterior esquerdo, após alerta do sistema e teste da caneca. Dados fictícios."},
]

# Ações do produtor (marcar alertas como resolvidos), antes de avaliar a ordenha:
#   (índice, vaca, tipo) -> resolve todos os alertas abertos desse tipo da vaca.
RESOLUCOES = {
    (8, 6, regras.SUSPEITA_MASTITE),   # iniciou o tratamento
    (13, 7, regras.SUSPEITA_MASTITE),  # CMT negativo: falso positivo
}
# Alertas de carência de dias anteriores a este já foram vistos pelo produtor.
CARENCIA_RESOLVIDA_ANTES_DO_DIA = -1

# O que a carga TEM de produzir: (vaca, tipo) -> (abertos > 0?, resolvidos > 0?)
ALERTAS_ESPERADOS = {
    (3, regras.SUSPEITA_MASTITE): (True, False),
    (4, regras.QUEDA_PRODUCAO): (True, False),
    (6, regras.SUSPEITA_MASTITE): (False, True),
    (6, regras.LEITE_EM_CARENCIA): (True, True),
    (7, regras.SUSPEITA_MASTITE): (False, True),
}


# ---------------------------------------------------------------------------
def offset_e_turno(i: int):
    return -DIAS + i // 2, ("MANHA" if i % 2 == 0 else "TARDE")


def sql_str(s):
    return "NULL" if s is None else "'" + str(s).replace("'", "''") + "'"


def sql_dia(off: int) -> str:
    """Data relativa a hoje, em MySQL: CURDATE() - INTERVAL n DAY."""
    if off == 0:
        return "CURDATE()"
    return f"CURDATE() {'-' if off < 0 else '+'} INTERVAL {abs(off)} DAY"


def sql_momento(off: int, hh: int, mm: int) -> str:
    """Data e hora relativas a hoje: TIMESTAMP(data, 'hh:mm:00') devolve um DATETIME."""
    return f"TIMESTAMP({sql_dia(off)}, '{hh:02d}:{mm:02d}:00')"


def gerar():
    rnd = random.Random(SEMENTE)
    hoje = date.today()
    ordenhas, alertas = [], []
    hist = {v["id"]: [] for v in VACAS}

    trat_por_vaca = {}
    for t in TRATAMENTOS:
        trat_por_vaca.setdefault(t["vaca_id"], []).append({
            "data_inicio": hoje + timedelta(days=t["inicio"]),
            "data_fim": None if t["fim"] is None else hoje + timedelta(days=t["fim"]),
            "dias_carencia": t["dias_carencia"], "medicamento": t["medicamento"]})

    for i in range(DIAS * 2):
        off, turno = offset_e_turno(i)
        for pos, v in enumerate(VACAS):
            if v["status"] == "SECA":
                continue
            for (ri, rv, rt) in RESOLUCOES:
                if ri == i and rv == v["id"]:
                    for a in alertas:
                        if a["vaca_id"] == rv and a["tipo"] == rt:
                            a["resolvido"] = 1

            litros_real = (v["litros"] * FATOR_TURNO[turno] * v.get("m_litros", mult({}))(i)
                           * rnd.gauss(1, RUIDO_LITROS))
            cond_real = v["cond"] * v.get("m_cond", mult({}))(i) * rnd.gauss(1, RUIDO_COND)
            medida = sensores.medir(cond_real, litros_real)
            hh, mm = HORA_TURNO[turno]
            mm += pos * 4  # cada vaca entra na ordenha alguns minutos depois da outra
            momento = datetime.combine(hoje + timedelta(days=off), time(hh + mm // 60, mm % 60))

            o = {"id": len(ordenhas) + 1, "vaca_id": v["id"], "off": off, "hh": hh + mm // 60,
                 "mm": mm % 60, "data_hora": momento, "turno": turno,
                 "litros": medida["litros"], "condutividade": medida["condutividade"]}
            abertos = [a["tipo"] for a in alertas if a["vaca_id"] == v["id"] and not a["resolvido"]]
            for d in regras.avaliar(hist[v["id"]], o, trat_por_vaca.get(v["id"], []), abertos):
                if d.novo:
                    alertas.append({"id": len(alertas) + 1, "ordenha_id": o["id"], "vaca_id": v["id"],
                                    "tipo": d.tipo, "mensagem": d.mensagem, "ordenha": o,
                                    "resolvido": 0})
            ordenhas.append(o)
            hist[v["id"]].append(o)

    for a in alertas:
        if a["tipo"] == regras.LEITE_EM_CARENCIA and a["ordenha"]["off"] < CARENCIA_RESOLVIDA_ANTES_DO_DIA:
            a["resolvido"] = 1

    obtido = {}
    for a in alertas:
        ab, re_ = obtido.get((a["vaca_id"], a["tipo"]), (False, False))
        obtido[(a["vaca_id"], a["tipo"])] = (ab or not a["resolvido"], re_ or bool(a["resolvido"]))
    if obtido != ALERTAS_ESPERADOS:
        sys.exit(f"ERRO: alertas gerados {obtido} diferentes do esperado {ALERTAS_ESPERADOS}.")
    return hoje, ordenhas, alertas


def escrever_sql(ordenhas, alertas, caminho: Path, cabecalho_extra=""):
    L = []
    w = L.append
    w("-- =============================================================================")
    w("-- ORDENHA MONITORADA · Parte 1 (Banco de dados)")
    w("-- Script 02 de 03: carga inicial. Rode DEPOIS do 01_criar_tabelas.sql.")
    w("-- ARQUIVO GERADO por banco/gerar_dados.py; não edite à mão.")
    if cabecalho_extra:
        w(cabecalho_extra)
    w("--")
    w("-- As datas são relativas ao dia da execução: a carga cobre os 10 dias")
    w("-- anteriores a hoje (CURDATE() - 10 dias até CURDATE() - 1 dia).")
    w(f"-- Usuários de teste (senha {SENHA_TESTE}, gravada só como hash BCrypt):")
    for u in USUARIOS:
        w(f"--   {u['email']}")
    w("-- Os alertas foram calculados com as mesmas regras da API (RegrasAlertaService).")
    w("--")
    w("-- Roteiro do rebanho (para consultas, testes e vídeo):")
    for v in VACAS:
        w(f"--   vaca {v['id']} {v['brinco']} {v['nome']:<9} {v['papel']}")
    w("-- =============================================================================")
    w("")
    w("SET NAMES utf8mb4;")
    w("USE ordenha;")
    w("")
    w("START TRANSACTION;")
    w("")
    w("-- USUARIO")
    for u in USUARIOS:
        w(f"INSERT INTO usuario (id, nome, email, senha_hash) VALUES ({u['id']}, "
          f"{sql_str(u['nome'])}, {sql_str(u['email'])}, {sql_str(u['senha_hash'])});")
    w("")
    w("-- PROPRIEDADE")
    for p in PROPRIEDADES:
        w(f"INSERT INTO propriedade (id, usuario_id, nome, municipio, uf) VALUES ({p['id']}, "
          f"{p['usuario_id']}, {sql_str(p['nome'])}, {sql_str(p['municipio'])}, {sql_str(p['uf'])});")
    w("")
    w("-- VACA")
    for v in VACAS:
        w(f"INSERT INTO vaca (id, propriedade_id, brinco, nome, data_nascimento, status) VALUES "
          f"({v['id']}, {v['propriedade_id']}, {sql_str(v['brinco'])}, {sql_str(v['nome'])}, "
          f"DATE '{v['data_nascimento']}', {sql_str(v['status'])});")
    w("")
    w("-- TRATAMENTO")
    for t in TRATAMENTOS:
        fim = "NULL" if t["fim"] is None else sql_dia(t["fim"])
        w(f"INSERT INTO tratamento (id, vaca_id, medicamento, data_inicio, data_fim, dias_carencia, "
          f"observacao) VALUES ({t['id']}, {t['vaca_id']}, {sql_str(t['medicamento'])}, "
          f"{sql_dia(t['inicio'])}, {fim}, {t['dias_carencia']}, {sql_str(t['observacao'])});")
    w("")
    w(f"-- ORDENHA ({len(ordenhas)} leituras: 7 vacas em lactação x 10 dias x 2 turnos)")
    for o in ordenhas:
        w(f"INSERT INTO ordenha (id, vaca_id, data_hora, turno, litros, condutividade) VALUES "
          f"({o['id']}, {o['vaca_id']}, {sql_momento(o['off'], o['hh'], o['mm'])}, "
          f"{sql_str(o['turno'])}, {o['litros']:.1f}, {o['condutividade']:.3f});")
    w("")
    w("-- ALERTA (resolvido: 0 = aberto, 1 = resolvido pelo produtor)")
    for a in alertas:
        o = a["ordenha"]
        w(f"INSERT INTO alerta (id, vaca_id, ordenha_id, tipo, mensagem, data_hora, resolvido) VALUES "
          f"({a['id']}, {a['vaca_id']}, {a['ordenha_id']}, {sql_str(a['tipo'])}, {sql_str(a['mensagem'])}, "
          f"{sql_momento(o['off'], o['hh'], o['mm'])}, {a['resolvido']});")
    w("")
    w("COMMIT;")
    w("")
    w("-- Os IDs acima foram informados à mão. No MySQL, o AUTO_INCREMENT de cada")
    w("-- tabela passa sozinho para o maior ID + 1, então o próximo INSERT feito")
    w("-- pela API (sem id) não colide com a carga. Conferência:")
    w("SELECT 'usuario' AS tabela, COUNT(*) AS linhas FROM usuario")
    for tab in ["propriedade", "vaca", "tratamento", "ordenha", "alerta"]:
        w(f"UNION ALL SELECT '{tab}', COUNT(*) FROM {tab}")
    L[-1] += ";"
    caminho.write_text("\n".join(L) + "\n", encoding="utf-8")


def iso(dt: datetime) -> str:
    return dt.strftime("%Y-%m-%dT%H:%M:%S")


def kt(s):
    """String literal em Kotlin (escapa barra, aspas e o $ dos templates)."""
    return '"' + str(s).replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$") + '"'


def escrever_kotlin(ordenhas, alertas, caminho: Path):
    """Mock do app Android: a mesma carga do banco, em Kotlin, com datas relativas a hoje."""
    L = []
    w = L.append
    w("// ARQUIVO GERADO por banco/gerar_dados.py; não edite à mão.")
    w("// É a mesma carga inicial do banco (banco/02_carga_inicial.sql): mesmos IDs, brincos,")
    w("// usuários, ordenhas, alertas e tratamentos. As datas são relativas ao dia em que o")
    w("// app roda (dia -10 a -1), como no banco. Condutividade em mS (escala do MastitAlert).")
    w("package com.ordenha.monitorada")
    w("")
    w("internal data class UsuarioCarga(val id: Int, val nome: String, val email: String)")
    w("internal data class OrdenhaCarga(val id: Int, val vacaId: Int, val dia: Int, val hora: String,")
    w("                                 val turno: Turno, val litros: Double, val condutividade: Double)")
    w("internal data class AlertaCarga(val id: Int, val vacaId: Int, val ordenhaId: Int, val tipo: TipoAlerta,")
    w("                                val mensagem: String, val resolvido: Boolean)")
    w("internal data class TratamentoCarga(val id: Int, val vacaId: Int, val medicamento: String, val inicio: Int,")
    w("                                    val fim: Int?, val diasCarencia: Int, val observacao: String)")
    w("")
    w("internal object DadosCarga {")
    w(f"    /** Senha dos usuários de exemplo, a mesma da API (lá ela fica só como hash BCrypt). */")
    w(f"    const val SENHA = {kt(SENHA_TESTE)}")
    w("")
    w("    val usuarios = listOf(")
    w(",\n".join(f"        UsuarioCarga({u['id']}, {kt(u['nome'])}, {kt(u['email'])})" for u in USUARIOS))
    w("    )")
    w("")
    w("    val propriedades = listOf(")
    w(",\n".join(f"        Propriedade({p['id']}, {p['usuario_id']}, {kt(p['nome'])}, {kt(p['municipio'])}, {kt(p['uf'])})"
                   for p in PROPRIEDADES))
    w("    )")
    w("")
    w("    val vacas = listOf(")
    w(",\n".join(f"        Vaca({v['id']}, {v['propriedade_id']}, {kt(v['brinco'])}, {kt(v['nome'])}, "
                   f"{kt(v['data_nascimento'])}, StatusVaca.{v['status']})" for v in VACAS))
    w("    )")
    w("")
    w("    // id, vaca, dia relativo a hoje, hora, turno, litros, condutividade (mS)")
    w("    val ordenhas = listOf(")
    w(",\n".join(f"        OrdenhaCarga({o['id']}, {o['vaca_id']}, {o['off']}, \"{o['hh']:02d}:{o['mm']:02d}\", "
                   f"Turno.{o['turno']}, {o['litros']}, {o['condutividade']})" for o in ordenhas))
    w("    )")
    w("")
    w("    val alertas = listOf(")
    w(",\n".join(f"        AlertaCarga({a['id']}, {a['vaca_id']}, {a['ordenha_id']}, TipoAlerta.{a['tipo']}, "
                   f"{kt(a['mensagem'])}, {'true' if a['resolvido'] else 'false'})" for a in alertas))
    w("    )")
    w("")
    w("    val tratamentos = listOf(")
    w(",\n".join(f"        TratamentoCarga({t['id']}, {t['vaca_id']}, {kt(t['medicamento'])}, {t['inicio']}, "
                   f"{'null' if t['fim'] is None else t['fim']}, {t['dias_carencia']}, {kt(t['observacao'])})"
                   for t in TRATAMENTOS))
    w("    )")
    w("}")
    caminho.write_text("\n".join(L) + "\n", encoding="utf-8")

def escrever_iot(ordenhas, alertas, dir_dados: Path):
    rebanho = [{"id": v["id"], "propriedadeId": v["propriedade_id"], "brinco": v["brinco"],
                "nome": v["nome"], "status": v["status"],
                "perfil": {"litrosBase": v["litros"], "condutividadeBaseMs": v["cond"],
                           # condição no fim da carga: o simulador continua a história
                           "fatorLitrosAtual": v.get("m_litros", mult({}))(DIAS * 2 - 1),
                           "fatorCondutividadeAtual": v.get("m_cond", mult({}))(DIAS * 2 - 1)}}
               for v in VACAS]
    hist = {
        "_leiaMe": ("Ponto de partida do simulador: as mesmas ordenhas, tratamentos e alertas "
                    "abertos da carga do banco. diaRelativo é contado a partir do dia em que "
                    "o simulador roda pela primeira vez (ou com --reset)."),
        "ordenhas": [{"vacaId": o["vaca_id"], "diaRelativo": o["off"],
                      "hora": f"{o['hh']:02d}:{o['mm']:02d}", "turno": o["turno"],
                      "litros": o["litros"], "condutividade": o["condutividade"]} for o in ordenhas],
        "tratamentos": [{"vacaId": t["vaca_id"], "medicamento": t["medicamento"],
                         "inicioRelativo": t["inicio"], "fimRelativo": t["fim"],
                         "diasCarencia": t["dias_carencia"]} for t in TRATAMENTOS],
        "alertasAbertos": [{"vacaId": a["vaca_id"], "tipo": a["tipo"]}
                           for a in alertas if not a["resolvido"]],
    }
    (dir_dados / "rebanho.json").write_text(
        json.dumps(rebanho, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (dir_dados / "historico_inicial.json").write_text(
        json.dumps(hist, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")


def main():
    hoje, ordenhas, alertas = gerar()
    (RAIZ / "iot" / "dados").mkdir(parents=True, exist_ok=True)
    escrever_sql(ordenhas, alertas, RAIZ / "banco" / "02_carga_inicial.sql")
    escrever_kotlin(ordenhas, alertas,
                    RAIZ / "mobile" / "app" / "src" / "main" / "java" / "com" / "ordenha" / "monitorada" / "DadosCarga.kt")
    escrever_iot(ordenhas, alertas, RAIZ / "iot" / "dados")
    print(f"OK: {len(ordenhas)} ordenhas, {len(alertas)} alertas "
          f"({sum(1 for a in alertas if not a['resolvido'])} abertos).")
    for a in alertas:
        o = a["ordenha"]
        print(f"  alerta {a['id']:>2}: vaca {a['vaca_id']} {a['tipo']:<18} dia {o['off']:>3} "
              f"{o['turno']:<5} {'resolvido' if a['resolvido'] else 'ABERTO'}")


if __name__ == "__main__":
    main()
