"""
Testes automatizados das regras de alerta (iguais às da API) e do modelo do sensor.

Rodar:  python -m unittest -v test_regras.py   (dentro da pasta iot)

As regras testadas aqui são as do RegrasAlertaService da API: estes testes
garantem que o simulador e a carga inicial preveem exatamente o que a API gera.
"""
import sys
import unittest
from datetime import date, datetime
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import regras    # noqa: E402
import sensores  # noqa: E402

DIA = datetime(2026, 11, 18, 6, 10)


def hist(conds=None, litros=None, n=10):
    """Histórico da mais antiga para a mais recente."""
    conds = conds or [0.42] * n
    litros = litros or [8.0] * len(conds)
    return [{"condutividade": c, "litros": l} for c, l in zip(conds, litros)]


def leitura(cond=0.42, litros=8.0, quando=DIA):
    return {"data_hora": quando, "condutividade": cond, "litros": litros}


def tipos(disparos, so_novos=True):
    return sorted(d.tipo for d in disparos if d.novo or not so_novos)


class TestMastite(unittest.TestCase):
    def test_leitura_normal_nao_gera_alerta(self):
        self.assertEqual(regras.avaliar(hist(), leitura(0.43)), [])

    def test_acima_de_15_por_cento_da_media_dispara(self):
        self.assertEqual(tipos(regras.avaliar(hist(), leitura(0.49))), ["SUSPEITA_MASTITE"])

    def test_logo_abaixo_de_15_por_cento_nao_dispara(self):
        # média 0,400 x 1,15 = 0,460
        self.assertEqual(regras.avaliar(hist([0.40] * 7), leitura(0.459)), [])
        self.assertEqual(tipos(regras.avaliar(hist([0.40] * 7), leitura(0.461))), ["SUSPEITA_MASTITE"])

    def test_com_menos_de_3_ordenhas_nao_avalia(self):
        self.assertEqual(regras.avaliar(hist(n=2), leitura(0.60)), [])

    def test_com_3_ordenhas_ja_avalia(self):
        self.assertEqual(tipos(regras.avaliar(hist(n=3), leitura(0.60))), ["SUSPEITA_MASTITE"])

    def test_media_usa_so_as_7_ultimas(self):
        antigas_altas = [0.60] * 5 + [0.40] * 7
        self.assertEqual(tipos(regras.avaliar(hist(antigas_altas), leitura(0.47))), ["SUSPEITA_MASTITE"])

    def test_mastite_repete_a_cada_ordenha_como_na_api(self):
        d = regras.avaliar(hist(), leitura(0.60), tipos_abertos=["SUSPEITA_MASTITE"])
        self.assertTrue(d[0].novo)


class TestQuedaProducao(unittest.TestCase):
    def test_tres_ordenhas_seguidas_abaixo_de_80_por_cento_dispara(self):
        h = hist(litros=[10.0] * 7 + [7.5, 7.0])
        self.assertEqual(tipos(regras.avaliar(h, leitura(litros=7.2))), ["QUEDA_PRODUCAO"])

    def test_so_duas_seguidas_nao_dispara(self):
        h = hist(litros=[10.0] * 8 + [7.0])
        self.assertEqual(regras.avaliar(h, leitura(litros=7.0)), [])

    def test_uma_normal_no_meio_quebra_a_sequencia(self):
        h = hist(litros=[10.0] * 7 + [7.0, 9.5])
        self.assertEqual(regras.avaliar(h, leitura(litros=7.0)), [])

    def test_precisa_de_5_ordenhas_anteriores(self):
        self.assertEqual(regras.avaliar(hist(litros=[10.0] * 2 + [7.0, 7.0]), leitura(litros=7.0)), [])
        self.assertEqual(tipos(regras.avaliar(hist(litros=[10.0] * 3 + [7.0, 7.0]), leitura(litros=7.0))),
                         ["QUEDA_PRODUCAO"])

    def test_queda_nao_repete_com_alerta_aberto(self):
        h = hist(litros=[10.0] * 7 + [7.0, 7.0])
        d = regras.avaliar(h, leitura(litros=7.0), tipos_abertos=["QUEDA_PRODUCAO"])
        self.assertEqual(tipos(d), [])
        self.assertEqual(tipos(d, so_novos=False), ["QUEDA_PRODUCAO"])


class TestCarencia(unittest.TestCase):
    TRAT = [{"data_inicio": date(2026, 11, 12), "data_fim": date(2026, 11, 16),
             "dias_carencia": 4, "medicamento": "Cefalexina"}]

    def test_ordenha_durante_a_carencia_dispara(self):
        self.assertEqual(tipos(regras.avaliar(hist(), leitura(), self.TRAT)), ["LEITE_EM_CARENCIA"])

    def test_ultimo_dia_de_carencia_ainda_dispara(self):
        d = regras.avaliar(hist(), leitura(quando=datetime(2026, 11, 20, 17, 0)), self.TRAT)
        self.assertEqual(tipos(d), ["LEITE_EM_CARENCIA"])

    def test_dia_seguinte_nao_dispara(self):
        self.assertEqual(regras.avaliar(hist(), leitura(quando=datetime(2026, 11, 21, 6, 0)), self.TRAT), [])

    def test_antes_do_inicio_nao_dispara(self):
        self.assertEqual(regras.avaliar(hist(), leitura(quando=datetime(2026, 11, 11, 6, 0)), self.TRAT), [])

    def test_tratamento_sem_data_fim_continua_em_carencia(self):
        aberto = [{"data_inicio": date(2026, 11, 1), "data_fim": None,
                   "dias_carencia": 4, "medicamento": "Cefalexina"}]
        self.assertEqual(tipos(regras.avaliar(hist(), leitura(), aberto)), ["LEITE_EM_CARENCIA"])


class TestAcumulo(unittest.TestCase):
    def test_uma_ordenha_pode_disparar_as_tres_regras(self):
        h = hist(litros=[10.0] * 7 + [7.0, 7.0])
        d = regras.avaliar(h, leitura(0.60, 7.0), TestCarencia.TRAT)
        self.assertEqual(tipos(d), ["LEITE_EM_CARENCIA", "QUEDA_PRODUCAO", "SUSPEITA_MASTITE"])


class TestSensorMastitAlert(unittest.TestCase):
    def test_equacoes_do_artigo_ida_e_volta(self):
        for ms in (0.346, 0.40, 0.45, 0.537, 0.646, 0.673):
            adc = sensores.condutividade_para_adc(ms)
            self.assertAlmostEqual(sensores.adc_para_condutividade(adc), ms, delta=0.003)

    def test_conversor_fica_entre_0_e_1023(self):
        self.assertEqual(sensores.condutividade_para_adc(0), 0)
        self.assertLessEqual(sensores.condutividade_para_adc(1000), 1023)

    def test_alarme_do_aparelho_no_limite_do_mastitalert(self):
        self.assertTrue(sensores.alarme_aparelho(0.537))
        self.assertFalse(sensores.alarme_aparelho(0.536))

    def test_medidor_de_volume(self):
        self.assertEqual(sensores.medir(0.42, 8.04)["litros"], 8.0)


if __name__ == "__main__":
    unittest.main(verbosity=2)
