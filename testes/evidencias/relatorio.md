# Relatório de execução do plano de testes

- **Executado em:** 20/11/2026 18:13:23
- **API:** http://localhost:8080
- **Resultado:** 15 de 15 casos passaram

| Caso | Cenário | Entrada | Saída esperada | Obtido (HTTP) | Status |
|---|---|---|---|---|---|
| CT01 | Cadastro de usuário válido | nome, email novo, senha válida | 201, sem senha nem hash na resposta | 201 | **PASSOU** |
| CT02 | Login com senha errada | email existente, senha incorreta | 401 e mensagem genérica | 401 | **PASSOU** |
| CT03 | Cadastro de vaca válido | brinco novo T-209202 | 201 e aparece em GET /vacas | 201, id 9 | **PASSOU** |
| CT04 | Brinco repetido na propriedade | brinco T-209202 de novo | 409 | 409 | **PASSOU** |
| CT05 | Ordenha com litros negativos | litros = -2 | 400, erro no campo litros | 400 | **PASSOU** |
| CT06 | Ordenha normal | BR-002, 9,0 L, 0,402 mS | 201 e nenhum alerta | 201 | **PASSOU** |
| CT07 | Condutividade acima da média | BR-002, 0,480 mS (+20%) | 201 e alerta SUSPEITA_MASTITE | 201 | **PASSOU** |
| CT08 | Ordenha de vaca em carência | SL-010 com tratamento ativo | 201 e alerta LEITE_EM_CARENCIA | 201 | **PASSOU** |
| CT09 | Vaca inexistente | GET /vacas/9999 | 404 | 404 | **PASSOU** |
| CT10 | SQL injection no login | email ' OR '1'='1 | 401, sem erro de banco | 401 | **PASSOU** |
| CT11 | Rota protegida sem token | GET /vacas sem Authorization | 401 | 401 | **PASSOU** |
| CT12 | Vaca de outro produtor | João pede GET /vacas/6 | 404 (não revela que existe) | 404 | **PASSOU** |
| CT13 | Ordenha com data futura | dataHora amanhã | 400, erro no campo dataHora | 400 | **PASSOU** |
| CT14 | XSS no nome da vaca | nome <script>…</script> | 400, nada gravado | 400 | **PASSOU** |
| CT15 | Excluir vaca | DELETE /vacas/9 | 204 e depois 404 | 204 → 404 | **PASSOU** |

A requisição e a resposta completas de cada caso estão em `CTxx.txt`, nesta pasta.
