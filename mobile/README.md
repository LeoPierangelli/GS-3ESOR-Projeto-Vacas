# Ordenha Monitorada: Front-end mobile

> **A fazer** (parte 4 · 3 pts). Esta pasta recebe o projeto do app.

## O que já está aqui

- [`mock_app.json`](mock_app.json): os dados de exemplo do app, no mesmo formato das tabelas do banco e com as mesmas vacas da carga inicial. Pelo enunciado, o app pode usar dados mockados. O arquivo é gerado por `banco/gerar_dados.py`, junto com a carga do banco.

## O que o app precisa ter (do planejamento)

| Tela | O que mostra | Para onde vai |
|---|---|---|
| Login | email, senha, link para criar conta e mensagem de erro no login inválido | Rebanho |
| Rebanho | lista de vacas com brinco, nome e selo de status (normal, suspeita, em tratamento), mais o contador de alertas abertos | Ficha, Cadastro, Sair |
| Ficha da vaca | produção das últimas ordenhas, última condutividade contra a média, alertas e tratamentos | Voltar |
| Cadastro de vaca | brinco, nome e data de nascimento, com validação e mensagem de sucesso | Rebanho |
| Alertas (opcional) | alertas abertos, do mais recente ao mais antigo | Ficha |

- **Navegação:** toda tela tem como voltar, e o Rebanho tem o botão de sair.
- **Usabilidade:** o produtor usa o celular no curral. Então os botões são grandes, o texto é legível e o status usa cor **mais** ícone, não só cor. Toda ação dá retorno: carregando, sucesso ou erro.

## Se for consumir a API (diferencial)

- **Rotas e formato dos dados:** em [`api/README.md`](../api/README.md) e no Swagger (`http://localhost:8080/swagger-ui.html`).
- **Login:** `POST /auth/login` devolve um `token`. As outras rotas exigem o cabeçalho `Authorization: Bearer <token>`.
- **No celular** (app nativo, Expo Go), `localhost` é o próprio celular. Use o IP do computador na rede, por exemplo `http://192.168.0.10:8080`.
- **No navegador** (Expo Web), a API aceita chamadas de páginas em `localhost` e `127.0.0.1` (CORS).
