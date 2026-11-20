# Ordenha Monitorada: aplicativo Android

O app do produtor, feito em **Kotlin + Jetpack Compose (Material 3)**, com **Navigation Compose**. Ele usa dados mockados e não depende da API para rodar, como o enunciado permite.

## Requisitos

- **Android Studio** (o JDK 17 já vem junto) e o **Android SDK 35**, que o Android Studio instala.
- Um **emulador** ou um aparelho com **Android 8.0** (API 26) ou superior.
- Internet na primeira abertura, para o Gradle baixar as dependências.

## Como rodar

**No Android Studio:**
1. Escolha **File > Open** e selecione a pasta **`mobile/`** deste repositório (é a pasta que tem o `settings.gradle.kts`).
2. Espere o **Gradle Sync** terminar. O projeto já traz o Gradle Wrapper (`gradlew`, Gradle 8.9), então não é preciso instalar o Gradle.
3. Escolha um emulador ou um aparelho com depuração USB e clique em **Run ▶** na configuração `app`.

**Pelo terminal** (opcional), dentro de `mobile/` e com a variável `ANDROID_HOME` apontando para o Android SDK:

```bash
./gradlew assembleDebug
```
```bash
./gradlew testDebugUnitTest
```

O primeiro comando gera o APK em `app/build/outputs/apk/debug/app-debug.apk`. O segundo roda os testes de unidade. No Windows, use `gradlew.bat`.

## Login de demonstração

São os mesmos usuários da API. Cada produtor vê só o próprio rebanho, como no controle de acesso da API.

| Email | Senha | Propriedade | Vacas |
|---|---|---|---|
| `joao@exemplo.com` | `senha123` | Sítio Boa Vista | 1 a 5 (BR-001 a BR-005) |
| `maria@exemplo.com` | `senha123` | Fazendinha Santa Luzia | 6 a 8 (SL-010 a SL-012) |

O login é simulado no próprio app, sem chamar a API.

## Telas

| Tela | O que tem |
|---|---|
| Login | email e senha, validação dos campos, erro de credenciais e indicador de carregamento |
| Rebanho | as vacas do produtor com brinco, nome e status (texto, ícone e cor), o contador de alertas abertos e os botões Cadastrar e Sair |
| Ficha da vaca | histórico de ordenhas, a última condutividade contra a média das 7 anteriores, os alertas abertos e os tratamentos |
| Cadastro de vaca | brinco, nome e nascimento, com validação de data (impossível ou futura) e de brinco repetido na propriedade |

- **Navegação:** toda tela tem como voltar, e o Rebanho tem o botão Sair.
- **Não implementado:** a tela opcional de Alertas, e também registrar tratamento e resolver alerta (no projeto, essas ações estão na API).

## Dados

O app usa **a mesma carga inicial do banco**, em [`DadosCarga.kt`](app/src/main/java/com/ordenha/monitorada/DadosCarga.kt):
- os mesmos usuários, vacas, ordenhas, alertas e tratamentos;
- as datas são relativas ao dia em que o app roda (os 10 dias anteriores), como no banco;
- esse arquivo é **gerado** por `banco/gerar_dados.py`, junto com o `.sql`. Não edite à mão.

As telas leem tudo pelo [`RepositorioMock.kt`](app/src/main/java/com/ordenha/monitorada/RepositorioMock.kt). Para o app consumir a API no futuro, só esse arquivo precisa mudar.

Os casos que aparecem no app (e no banco, na API e no vídeo):

| Vaca | Produtor | O que mostra |
|---|---|---|
| BR-003 Mimosa | João | condutividade subindo: **suspeita de mastite** |
| BR-004 Estrela | João | **queda de produção** |
| SL-010 Pintada | Maria | **em tratamento**, com o leite em carência |
| SL-012 Serena | Maria | **seca** |

O alerta é sempre uma suspeita, nunca um diagnóstico. Os percentuais são valores iniciais do MVP, não parâmetros veterinários validados.

## Testes

**Automatizados:** são 8 testes JUnit em [`RepositorioMockTest.kt`](app/src/test/java/com/ordenha/monitorada/RepositorioMockTest.kt), que rodam com `./gradlew testDebugUnitTest`. Eles conferem:
- que os dados são a mesma carga do banco;
- que o login mostra só as vacas do produtor;
- que os alertas da Mimosa, da Estrela e da Pintada seguem as mesmas regras da API;
- que o cadastro recusa brinco repetido.

**Roteiro manual:**
1. Entrar com os campos vazios: o app pede email e senha.
2. Entrar com senha errada: aparece "Email ou senha inválidos".
3. Entrar como `joao@exemplo.com`: abre o Rebanho com 5 vacas e os alertas abertos.
4. Conferir os status: a Mimosa e a Estrela com alerta, as outras normais.
5. Abrir a ficha da Mimosa: a última condutividade aparece acima da média, com o alerta de suspeita de mastite.
6. No Cadastro, salvar vazio: os campos obrigatórios ficam sinalizados.
7. Informar uma data impossível (`31/02/2022`) ou futura: o cadastro é bloqueado.
8. Informar o brinco `BR-001`: aparece o erro de brinco repetido.
9. Cadastrar com um brinco novo: o app volta ao Rebanho mostrando a vaca nova.
10. Sair e entrar como `maria@exemplo.com`: aparecem só a Pintada (em tratamento), a Faceira e a Serena (seca).

## Créditos

A ideia de usar a condutividade elétrica do leite como indicador de suspeita de mastite vem do protótipo MastitAlert (Universidade Católica de Petrópolis) e das fontes listadas no [PDF de entrega](../Ordenha_Monitorada.pdf), na seção "Fontes e créditos". Todos os dados são fictícios.
