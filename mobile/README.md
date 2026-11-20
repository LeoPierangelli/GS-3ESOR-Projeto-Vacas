# Ordenha Monitorada — aplicativo Android

Aplicativo demonstrativo do projeto GS 2026/2, desenvolvido em **Kotlin + Jetpack Compose (Material 3)** e **Navigation Compose**. O aplicativo utiliza dados mockados e não depende da API para executar.

## Requisitos

- Android Studio (versão compatível com Android Gradle Plugin 8.7.3)
- JDK 17
- Android SDK 35 instalado
- Emulador Android ou aparelho com Android 8.0 (API 26) ou superior
- Acesso à internet na primeira sincronização do Gradle para baixar dependências

## Como abrir e executar

1. Extraia o ZIP.
2. No Android Studio, selecione **Open** e escolha a pasta `OrdenhaMonitorada_app` (a pasta que contém `settings.gradle.kts`).
3. Configure o Gradle para usar a versão 8.9 ou compatível com o Android Gradle Plugin 8.7.3, instale o Android SDK 35 e configure o JDK 17 em `Settings > Build, Execution, Deployment > Build Tools > Gradle`.
4. Selecione um emulador ou conecte um aparelho Android com depuração USB.
5. Execute a configuração `app` pelo botão **Run**.

O projeto usa Gradle Kotlin DSL e as dependências estão declaradas em `app/build.gradle.kts`. **Atenção:** este pacote ainda não inclui o Gradle Wrapper (`gradlew`, `gradlew.bat` e `gradle/wrapper/gradle-wrapper.jar`). Se o Android Studio exigir o Wrapper, será necessário gerá-lo com Gradle 8.9 em um ambiente com Gradle instalado (`gradle wrapper --gradle-version 8.9`) antes de executar o projeto.

## Acesso de demonstração

- Email: `produtor@teste.com`
- Senha: `123456`

O login é simulado localmente. Não use essas credenciais como autenticação real.

## Telas e funcionalidades

| Tela | Implementação |
|---|---|
| Login | Email, senha, validação de campos, erro de credenciais e indicador de carregamento |
| Rebanho | Lista de 8 vacas, brinco, status com texto/ícone/cor e contador de alertas abertos |
| Ficha da vaca | Histórico de ordenhas, comparação de condutividade com a média das 7 anteriores, alertas e tratamentos |
| Cadastro de vaca | Brinco, nome, nascimento, validação de calendário/data futura e prevenção de brinco duplicado na mesma propriedade |

A tela opcional de Alertas não foi implementada. O acesso de criação de usuário informa que o cadastro real pertence ao endpoint `POST /auth/cadastro`, que não é integrado neste protótipo.

## Dados mockados

Os dados estão centralizados em `app/src/main/java/com/ordenha/monitorada/RepositorioMock.kt` e seguem os modelos definidos em `Models.kt`:

- 2 propriedades de demonstração;
- 8 vacas, distribuídas entre as duas propriedades;
- 10 dias de registros, com dois turnos por dia para cada vaca (160 ordenhas simuladas);
- uma vaca com suspeita de mastite por aumento de condutividade;
- uma vaca com queda de produção nas últimas três ordenhas;
- uma vaca em tratamento, com alerta de leite em período de carência.

Os dados, datas e leituras são fictícios. Os percentuais de alerta são parâmetros iniciais do MVP, não parâmetros veterinários validados. O aplicativo apresenta suspeitas, nunca diagnósticos.

## Roteiro de teste manual

1. Abrir o aplicativo e tentar entrar com campos vazios: deve solicitar email e senha.
2. Informar senha incorreta: deve exibir mensagem de credenciais inválidas.
3. Entrar com as credenciais de demonstração: deve abrir o Rebanho e mostrar 3 alertas abertos.
4. Conferir os oito animais e os status: Bela (suspeita), Pintada (suspeita/queda de produção), Fumaça (em tratamento) e Dorinha (seca).
5. Abrir a ficha da Bela: conferir condutividade, média das sete leituras anteriores, alerta e histórico.
6. Abrir Cadastro de vaca e tentar salvar vazio: os campos obrigatórios devem ser sinalizados.
7. Informar data impossível (ex.: `31/02/2022`) ou futura: o cadastro deve ser bloqueado.
8. Informar brinco já utilizado na propriedade (ex.: `101`): deve exibir erro de duplicidade.
9. Cadastrar uma vaca com brinco novo e data válida: deve retornar ao Rebanho e exibir a nova vaca.
10. Abrir uma ficha e voltar; sair pelo botão Sair: deve retornar ao Login.

## Testes automatizados de consistência dos mocks

Foi incluída a classe `RepositorioMockTest.kt`, com testes JUnit para quantidade de propriedades/vacas/ordenhas, integridade das referências, suspeita de mastite, queda de produção e tratamento em carência. Esses testes foram escritos, mas **ainda precisam ser executados** em ambiente Android/Gradle. No Android Studio, execute a tarefa `testDebugUnitTest`.

## Estrutura

```text
OrdenhaMonitorada_app/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/ordenha/monitorada/
│       │   ├── AppNavigation.kt
│       │   ├── CadastroVacaScreen.kt
│       │   ├── Componentes.kt
│       │   ├── FichaVacaScreen.kt
│       │   ├── LoginScreen.kt
│       │   ├── MainActivity.kt
│       │   ├── Models.kt
│       │   ├── RebanhoScreen.kt
│       │   ├── Regras.kt
│       │   ├── RepositorioMock.kt
│       │   └── Theme.kt
│       └── res/values/strings.xml
├── build.gradle.kts
├── gradle.properties
└── settings.gradle.kts
```

## Integração e limites

O front-end não chama a API: login, listagem e cadastro são simulados em `RepositorioMock`. A integração é uma etapa separada do MVP e pode substituir o repositório mock por chamadas HTTP mantendo as telas. Não há registro de tratamento, resolução de alerta ou notificações push no aplicativo.

## Integrantes e entrega

- Integrantes e RMs: preencher com os dados confirmados pelo grupo.
- Link do pitch: inserir o link público após a gravação.

## Créditos

A ideia de uso da condutividade elétrica como indicador de suspeita de mastite tem como referência o protótipo MastitAlert (Universidade Católica de Petrópolis) e as fontes técnicas listadas no documento de planejamento do grupo. Leituras e animais deste aplicativo são simulados.
