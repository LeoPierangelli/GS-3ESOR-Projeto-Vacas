package com.ordenha.monitorada

/**
 * Valores iniciais do MVP. NÃO são parâmetros veterinários validados:
 * foram definidos pelo grupo para a simulação e podem ser ajustados aqui.
 * A API aplica as mesmas regras no Service; o app só usa estes valores para
 * mostrar a comparação com a média na ficha da vaca.
 */
object Regras {
    /** Condutividade acima da média da vaca em mais de X% = suspeita de mastite. */
    const val LIMITE_CONDUTIVIDADE_PCT = 15.0

    /** Quantas ordenhas anteriores entram no cálculo da média da vaca. */
    const val JANELA_MEDIA_ORDENHAS = 7
}
