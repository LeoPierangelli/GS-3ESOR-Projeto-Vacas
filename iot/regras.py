"""
Regras de alerta do SISTEMA (as mesmas da API).

Este módulo reproduz, linha a linha, o RegrasAlertaService e o OrdenhaService
da API (api/src/main/java/br/com/fiap/ordenha/service/), com as mesmas
constantes do application.properties. Ele é usado pelo gerador da carga
inicial (para os alertas da carga serem exatamente os que a API geraria) e
pelo simulador (para mostrar no console o que a API vai responder).

O alarme do próprio aparelho, inspirado no MastitAlert (limite fixo de
0,537 mS), fica em sensores.py: é uma decisão local do sensor, separada
destas regras, que comparam a vaca com o histórico dela.

Os percentuais são valores iniciais do MVP, NÃO parâmetros veterinários
validados. O alerta é sempre uma SUSPEITA, nunca um diagnóstico.
"""
from __future__ import annotations

from dataclasses import dataclass
from datetime import date, datetime, timedelta

# Mesmos valores de api/src/main/resources/application.properties (ordenha.regras.*)
LIMITE_MASTITE = 0.15    # condutividade 15% acima da média da vaca
LIMITE_QUEDA = 0.20      # litros 20% abaixo da média da vaca
JANELA = 7               # quantas ordenhas anteriores entram na média
ORDENHAS_SEGUIDAS = 3    # quantas ordenhas seguidas abaixo do limite para a queda
HISTORICO_MINIMO = 3     # sem isso não há média confiável

SUSPEITA_MASTITE = "SUSPEITA_MASTITE"
QUEDA_PRODUCAO = "QUEDA_PRODUCAO"
LEITE_EM_CARENCIA = "LEITE_EM_CARENCIA"

MENSAGENS = {
    SUSPEITA_MASTITE: "Suspeita de mastite: faça o teste da caneca ou o CMT",
    QUEDA_PRODUCAO: "Produção em queda",
    LEITE_EM_CARENCIA: "Não enviar este leite ao tanque",
}

# Tipos que significam "este leite não deve ir para o tanque" (ação da válvula).
TIPOS_QUE_DESVIAM_LEITE = {SUSPEITA_MASTITE, LEITE_EM_CARENCIA}


@dataclass
class Disparo:
    tipo: str
    mensagem: str
    motivo: str          # explicação curta, para console e logs
    novo: bool = True    # False = a API não grava (queda com alerta de queda já aberto)


def _media(valores):
    return sum(valores) / len(valores) if valores else 0.0


def _dia(valor) -> date:
    return valor.date() if isinstance(valor, datetime) else valor


def em_carencia(dia_ordenha, tratamento) -> bool:
    """Igual a Tratamento.emCarencia() da API: do início até data_fim + dias_carencia,
    inclusive. Sem data_fim, o tratamento não terminou e o leite segue em carência."""
    d = _dia(dia_ordenha)
    if d < _dia(tratamento["data_inicio"]):
        return False
    if tratamento.get("data_fim") is None:
        return True
    ultimo_dia = _dia(tratamento["data_fim"]) + timedelta(days=int(tratamento["dias_carencia"]))
    return d <= ultimo_dia


def suspeita_mastite(cond_atual: float, anteriores_recente_primeiro: list[float]) -> tuple[bool, float]:
    """RegrasAlertaService.suspeitaMastite(): média das até 7 últimas, com no mínimo 3."""
    if len(anteriores_recente_primeiro) < HISTORICO_MINIMO:
        return False, 0.0
    media = _media(anteriores_recente_primeiro[:min(JANELA, len(anteriores_recente_primeiro))])
    return cond_atual > media * (1 + LIMITE_MASTITE), media


def queda_producao(litros_atual: float, anteriores_recente_primeiro: list[float]) -> tuple[bool, float]:
    """RegrasAlertaService.quedaProducao(): a atual e as 2 anteriores abaixo de 80% da
    média das até 7 ordenhas que vieram antes delas (mínimo de 3 nessa base)."""
    recentes = ORDENHAS_SEGUIDAS - 1
    if len(anteriores_recente_primeiro) < recentes + HISTORICO_MINIMO:
        return False, 0.0
    base = anteriores_recente_primeiro[recentes:min(recentes + JANELA, len(anteriores_recente_primeiro))]
    limite = _media(base) * (1 - LIMITE_QUEDA)
    if litros_atual >= limite:
        return False, limite
    return all(x < limite for x in anteriores_recente_primeiro[:recentes]), limite


def avaliar(historico, leitura, tratamentos=(), tipos_abertos=()) -> list[Disparo]:
    """Aplica as três regras como o OrdenhaService.registrar() da API.

    historico     ordenhas ANTERIORES da vaca, da mais antiga para a mais recente,
                  com "litros" e "condutividade" (a leitura atual NÃO entra aqui).
    leitura       dict com "data_hora" (datetime), "litros" e "condutividade".
    tratamentos   tratamentos da vaca: "data_inicio", "data_fim" (ou None), "dias_carencia".
    tipos_abertos tipos de alerta que a vaca já tem em aberto. Como na API, só a queda
                  deixa de ser gravada de novo quando já existe uma aberta.
    """
    recente_primeiro = list(reversed(historico))[:JANELA + ORDENHAS_SEGUIDAS - 1]
    conds = [float(o["condutividade"]) for o in recente_primeiro]
    litros_ant = [float(o["litros"]) for o in recente_primeiro]
    cond, litros = float(leitura["condutividade"]), float(leitura["litros"])
    disparos: list[Disparo] = []

    dispara, media = suspeita_mastite(cond, conds)
    if dispara:
        disparos.append(Disparo(SUSPEITA_MASTITE, MENSAGENS[SUSPEITA_MASTITE],
                                f"{cond:.3f} mS está {(cond / media - 1) * 100:.0f}% acima da "
                                f"média da vaca ({media:.3f} mS)"))

    dispara, limite = queda_producao(litros, litros_ant)
    if dispara:
        disparos.append(Disparo(QUEDA_PRODUCAO, MENSAGENS[QUEDA_PRODUCAO],
                                f"{ORDENHAS_SEGUIDAS} ordenhas seguidas abaixo de {limite:.2f} L "
                                f"(80% da média da vaca)",
                                novo=QUEDA_PRODUCAO not in set(tipos_abertos)))

    for t in tratamentos:
        if em_carencia(leitura["data_hora"], t):
            fim = ("tratamento ainda sem data de fim" if t.get("data_fim") is None else
                   "último dia de carência " + (_dia(t["data_fim"]) + timedelta(
                       days=int(t["dias_carencia"]))).strftime("%d/%m/%Y"))
            disparos.append(Disparo(LEITE_EM_CARENCIA, MENSAGENS[LEITE_EM_CARENCIA],
                                    f"tratamento com {t['medicamento']}; {fim}"))
            break
    return disparos
