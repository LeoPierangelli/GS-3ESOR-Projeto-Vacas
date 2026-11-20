#!/usr/bin/env bash
# =============================================================================
# Ordenha Monitorada · Plano de testes da API (execução automatizada)
#
# Roda os casos do plano contra a API rodando em http://localhost:8080 e grava:
#   testes/evidencias/CTxx.txt  requisição, resposta e status de cada caso
#   testes/evidencias/relatorio.md  tabela com o resultado de todos os casos
#
# Pré-requisitos: banco e API rodando (ver api/README.md), bash, curl e GNU date
# (Linux, WSL ou Git Bash). Para partir sempre da mesma carga, recrie o banco antes:
#   cd api && docker compose down -v && docker compose up -d
#
# Uso (na raiz do repositório):  bash testes/executar_testes.sh
# =============================================================================
set -u

API="${API:-http://localhost:8080}"
DIR="$(cd "$(dirname "$0")" && pwd)"
EVID="$DIR/evidencias"
mkdir -p "$EVID"
rm -f "$EVID"/CT*.txt

PASSOU=0
FALHOU=0
LINHAS=()
CORPO=""
STATUS=""

# Data e hora local no formato da API, N segundos atrás (sempre no passado e sem repetir).
momento() { date -d "-$1 seconds" +%Y-%m-%dT%H:%M:%S; }

# req METODO ROTA [JSON] [TOKEN]  -> preenche STATUS e CORPO
req() {
  local metodo="$1" rota="$2" json="${3:-}" token="${4:-}"
  local args=(-s -o "$EVID/.corpo" -w "%{http_code}" -X "$metodo" "$API$rota")
  [ -n "$json" ] && args+=(-H "Content-Type: application/json" -d "$json")
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  STATUS="$(curl "${args[@]}")"
  CORPO="$(cat "$EVID/.corpo")"
  ULTIMA_REQ="$metodo $rota${json:+
$json}"
}

# registra ID CENARIO ENTRADA ESPERADO CONDICAO(0=passou)
registra() {
  local id="$1" cenario="$2" entrada="$3" esperado="$4" ok="$5" obtido="$6"
  local res
  if [ "$ok" -eq 0 ]; then res="PASSOU"; PASSOU=$((PASSOU + 1)); else res="FALHOU"; FALHOU=$((FALHOU + 1)); fi
  {
    echo "Caso:      $id · $cenario"
    echo "Executado: $(date '+%d/%m/%Y %H:%M:%S')"
    echo "Entrada:   $entrada"
    echo "Esperado:  $esperado"
    echo "Obtido:    $obtido"
    echo "Resultado: $res"
    echo
    echo "--- Requisição ---"
    echo "$ULTIMA_REQ"
    echo
    echo "--- Resposta (HTTP $STATUS) ---"
    echo "$CORPO"
  } > "$EVID/$id.txt"
  LINHAS+=("| $id | $cenario | $entrada | $esperado | $obtido | **$res** |")
  printf '%-5s %-46s %s\n' "$id" "$cenario" "$res ($obtido)"
}

token_de() { sed -n 's/.*"token":"\([^"]*\)".*/\1/p' <<<"$1"; }

if ! curl -s -o /dev/null "$API/v3/api-docs"; then
  echo "A API não respondeu em $API. Suba o banco e a API antes (ver api/README.md)."
  exit 1
fi

echo "Executando o plano de testes contra $API"
echo

# Login dos dois produtores da carga inicial
req POST /auth/login '{"email":"joao@exemplo.com","senha":"senha123"}'
JOAO="$(token_de "$CORPO")"
req POST /auth/login '{"email":"maria@exemplo.com","senha":"senha123"}'
MARIA="$(token_de "$CORPO")"
if [ -z "$JOAO" ] || [ -z "$MARIA" ]; then echo "Não consegui fazer login com os usuários da carga."; exit 1; fi

SUFIXO="$(date +%s)"

# CT01 · cadastro de usuário válido (senha só em hash, nunca na resposta)
EMAIL="teste.$SUFIXO@exemplo.com"
req POST /auth/cadastro "{\"nome\":\"Teste Plano\",\"email\":\"$EMAIL\",\"senha\":\"senhaforte1\",\"propriedade\":{\"nome\":\"Sítio Teste\",\"municipio\":\"Lavras\",\"uf\":\"MG\"}}"
[ "$STATUS" = 201 ] && ! grep -qi 'senha\|hash' <<<"$CORPO"; ok=$?
registra CT01 "Cadastro de usuário válido" "nome, email novo, senha válida" "201, sem senha nem hash na resposta" $ok "$STATUS"

# CT02 · login com senha errada
req POST /auth/login '{"email":"joao@exemplo.com","senha":"errada123"}'
[ "$STATUS" = 401 ] && grep -q 'Email ou senha inválidos' <<<"$CORPO"; ok=$?
registra CT02 "Login com senha errada" "email existente, senha incorreta" "401 e mensagem genérica" $ok "$STATUS"

# CT03 · cadastro de vaca válido
BRINCO="T-${SUFIXO: -6}"
req POST /vacas "{\"propriedadeId\":1,\"brinco\":\"$BRINCO\",\"nome\":\"Vaca de teste\",\"dataNascimento\":\"2023-05-01\"}" "$JOAO"
VACA="$(sed -n 's/^{"id":\([0-9]*\).*/\1/p' <<<"$CORPO")"
ST3="$STATUS"; C3="$CORPO"
req GET /vacas "" "$JOAO"
LISTA="$CORPO"; STATUS="$ST3"; CORPO="$C3"
[ "$ST3" = 201 ] && grep -q "\"brinco\":\"$BRINCO\"" <<<"$LISTA"; ok=$?
registra CT03 "Cadastro de vaca válido" "brinco novo $BRINCO" "201 e aparece em GET /vacas" $ok "$ST3, id $VACA"

# CT04 · brinco repetido na mesma propriedade
req POST /vacas "{\"propriedadeId\":1,\"brinco\":\"$BRINCO\",\"nome\":\"Repetida\"}" "$JOAO"
[ "$STATUS" = 409 ]; ok=$?
registra CT04 "Brinco repetido na propriedade" "brinco $BRINCO de novo" "409" $ok "$STATUS"

# CT05 · ordenha com litros negativos
req POST /ordenhas "{\"vacaId\":2,\"dataHora\":\"$(momento 90)\",\"turno\":\"MANHA\",\"litros\":-2,\"condutividade\":0.40}" "$JOAO"
[ "$STATUS" = 400 ] && grep -q '"litros"' <<<"$CORPO"; ok=$?
registra CT05 "Ordenha com litros negativos" "litros = -2" "400, erro no campo litros" $ok "$STATUS"

# CT06 · ordenha normal (Malhada, média em torno de 0,400 mS)
req POST /ordenhas "{\"vacaId\":2,\"dataHora\":\"$(momento 80)\",\"turno\":\"MANHA\",\"litros\":9.0,\"condutividade\":0.402}" "$JOAO"
[ "$STATUS" = 201 ] && grep -q '"alertasGerados":\[\]' <<<"$CORPO"; ok=$?
registra CT06 "Ordenha normal" "BR-002, 9,0 L, 0,402 mS" "201 e nenhum alerta" $ok "$STATUS"

# CT07 · condutividade 20% acima da média da vaca
req POST /ordenhas "{\"vacaId\":2,\"dataHora\":\"$(momento 70)\",\"turno\":\"MANHA\",\"litros\":8.8,\"condutividade\":0.480}" "$JOAO"
[ "$STATUS" = 201 ] && grep -q '"tipo":"SUSPEITA_MASTITE"' <<<"$CORPO"; ok=$?
registra CT07 "Condutividade acima da média" "BR-002, 0,480 mS (+20%)" "201 e alerta SUSPEITA_MASTITE" $ok "$STATUS"

# CT08 · vaca em carência (Pintada, em tratamento, da Maria)
req POST /ordenhas "{\"vacaId\":6,\"dataHora\":\"$(momento 60)\",\"turno\":\"MANHA\",\"litros\":9.5,\"condutividade\":0.44}" "$MARIA"
[ "$STATUS" = 201 ] && grep -q '"tipo":"LEITE_EM_CARENCIA"' <<<"$CORPO"; ok=$?
registra CT08 "Ordenha de vaca em carência" "SL-010 com tratamento ativo" "201 e alerta LEITE_EM_CARENCIA" $ok "$STATUS"

# CT09 · vaca inexistente
req GET /vacas/9999 "" "$JOAO"
[ "$STATUS" = 404 ]; ok=$?
registra CT09 "Vaca inexistente" "GET /vacas/9999" "404" $ok "$STATUS"

# CT10 · tentativa de SQL injection no login
req POST /auth/login "{\"email\":\"' OR '1'='1\",\"senha\":\"x\"}"
[ "$STATUS" = 401 ]; ok=$?
registra CT10 "SQL injection no login" "email ' OR '1'='1" "401, sem erro de banco" $ok "$STATUS"

# CT11 · rota protegida sem token
req GET /vacas
[ "$STATUS" = 401 ]; ok=$?
registra CT11 "Rota protegida sem token" "GET /vacas sem Authorization" "401" $ok "$STATUS"

# CT12 · controle de acesso: o João pede uma vaca da Maria
req GET /vacas/6 "" "$JOAO"
[ "$STATUS" = 404 ]; ok=$?
registra CT12 "Vaca de outro produtor" "João pede GET /vacas/6" "404 (não revela que existe)" $ok "$STATUS"

# CT13 · ordenha com data futura
req POST /ordenhas "{\"vacaId\":2,\"dataHora\":\"$(date -d '+1 day' +%Y-%m-%dT%H:%M:%S)\",\"turno\":\"MANHA\",\"litros\":9.0,\"condutividade\":0.40}" "$JOAO"
[ "$STATUS" = 400 ] && grep -q '"dataHora"' <<<"$CORPO"; ok=$?
registra CT13 "Ordenha com data futura" "dataHora amanhã" "400, erro no campo dataHora" $ok "$STATUS"

# CT14 · tentativa de XSS no nome da vaca
req POST /vacas '{"propriedadeId":1,"brinco":"X-1","nome":"<script>alert(1)</script>"}' "$JOAO"
[ "$STATUS" = 400 ]; ok=$?
registra CT14 "XSS no nome da vaca" "nome <script>…</script>" "400, nada gravado" $ok "$STATUS"

# CT15 · excluir vaca (limpa a vaca do CT03)
req DELETE "/vacas/$VACA" "" "$JOAO"
ST15="$STATUS"; C15="$CORPO"
req GET "/vacas/$VACA" "" "$JOAO"
DEPOIS="$STATUS"; STATUS="$ST15"; CORPO="$C15"
[ "$ST15" = 204 ] && [ "$DEPOIS" = 404 ]; ok=$?
registra CT15 "Excluir vaca" "DELETE /vacas/$VACA" "204 e depois 404" $ok "$ST15 → $DEPOIS"

rm -f "$EVID/.corpo"
TOTAL=$((PASSOU + FALHOU))
{
  echo "# Relatório de execução do plano de testes"
  echo
  echo "- **Executado em:** $(date '+%d/%m/%Y %H:%M:%S')"
  echo "- **API:** $API"
  echo "- **Resultado:** $PASSOU de $TOTAL casos passaram"
  echo
  echo "| Caso | Cenário | Entrada | Saída esperada | Obtido (HTTP) | Status |"
  echo "|---|---|---|---|---|---|"
  printf '%s\n' "${LINHAS[@]}"
  echo
  echo "A requisição e a resposta completas de cada caso estão em \`CTxx.txt\`, nesta pasta."
} > "$EVID/relatorio.md"

echo
echo "$PASSOU de $TOTAL casos passaram. Relatório: testes/evidencias/relatorio.md"
[ "$FALHOU" -eq 0 ]
