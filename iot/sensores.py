"""
Modelo simulado dos dois sensores da ordenha.

Sensor de condutividade: reproduz o circuito do protótipo MastitAlert
(Portugal & Guedes, 2021): Arduino Uno, resistor de 10 kOhm e dois fios
mergulhados no leite. Um sinal de 5 V passa pelo leite e o Arduino lê um
valor analógico de 0 a 1023. O firmware converte esse valor em mS com as
três equações do artigo:

    Eq. 1  V  = (5 * sn) / 1023
    Eq. 2  R  = (5 * ref) / V - ref        (ref = 1000, calibrado com leite a ~35 °C)
    Eq. 3  mS = (1 / R) * 1000

Aqui fazemos o caminho completo: a partir da condutividade "real" do leite
geramos o valor bruto do conversor analógico (com a perda de precisão do
conversor de 10 bits) e depois aplicamos as equações, como o firmware faria.

Medidor de volume: modelado como um medidor de vazão por pulsos. Cada litro
gera um número fixo de pulsos; o firmware conta os pulsos e divide pela
constante. O valor da constante é um parâmetro da simulação.
"""
from __future__ import annotations

VCC = 5.0              # tensão do sinal (V)
ADC_MAX = 1023         # conversor analógico-digital de 10 bits do Arduino Uno
REF = 1000.0           # constante empírica do artigo, leite a ~35 °C
PULSOS_POR_LITRO = 450 # parâmetro da simulação do medidor de vazão

TEMPERATURA_MINIMA_C = 30.0  # abaixo disso a leitura é descartada (ver artigo)

# Alarme do próprio aparelho, como no MastitAlert: acima de 0,537 mS ele avisa
# na hora (som e luz) e fecha a válvula, sem depender do histórico da vaca.
# As regras do sistema (regras.py / API) comparam com a média da própria vaca.
ALARME_APARELHO_MS = 0.537


def alarme_aparelho(condutividade_ms: float) -> bool:
    return condutividade_ms >= ALARME_APARELHO_MS


def condutividade_para_adc(ms: float) -> int:
    """Caminho inverso das equações: condutividade do leite -> leitura bruta 0..1023."""
    if ms <= 0:
        return 0
    r = 1000.0 / ms                 # inverso da Eq. 3
    v = VCC * REF / (r + REF)       # inverso da Eq. 2
    sn = round(v * ADC_MAX / VCC)   # inverso da Eq. 1 (o arredondamento é o conversor)
    return max(0, min(ADC_MAX, sn))


def adc_para_condutividade(sn: int) -> float:
    """O que o firmware do MastitAlert faz: leitura bruta 0..1023 -> mS (Eqs. 1, 2 e 3)."""
    if sn <= 0:
        return 0.0
    v = (VCC * sn) / ADC_MAX        # Eq. 1
    r = (VCC * REF) / v - REF       # Eq. 2
    return (1.0 / r) * 1000.0       # Eq. 3


def litros_para_pulsos(litros: float) -> int:
    return round(litros * PULSOS_POR_LITRO)


def pulsos_para_litros(pulsos: int) -> float:
    return pulsos / PULSOS_POR_LITRO


def medir(condutividade_real_ms: float, litros_reais: float) -> dict:
    """Simula uma medição completa: sinais brutos e valores convertidos."""
    adc = condutividade_para_adc(condutividade_real_ms)
    pulsos = litros_para_pulsos(litros_reais)
    return {
        "adc": adc,
        "pulsos": pulsos,
        "condutividade": round(adc_para_condutividade(adc), 3),
        "litros": round(pulsos_para_litros(pulsos), 1),
    }
