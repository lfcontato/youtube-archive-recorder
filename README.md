# YouTube Archive Recorder

Ferramenta local para baixar, organizar e recortar conteudo do YouTube em uma pasta de download.

Primeira entrega: Windows com interface desktop em Compose.

## O que o projeto faz

- Baixar videos do YouTube para uma pasta local
- Baixar audio separado, quando fizer sentido
- Extrair e salvar legendas
- Criar cortes/trechos a partir de um video baixado
- Organizar saidas em pastas por canal, data ou tema
- Operar de forma local, sem depender de um servico externo para o fluxo principal

## Objetivo

A ideia e ser um utilitario simples, rapido e local-first para arquivar conteudo do YouTube sem depender de um fluxo manual repetitivo.

## O que vai ser utilizado desta base anterior

Do projeto `meeting-archive-recorder`, vamos reaproveitar principalmente a forma de organizar o trabalho:

- Estrutura local-first, com saida em pasta do usuario
- Foco em Windows como primeiro alvo
- Execucao simples por comando ou atalho local
- Separacao clara entre interface, regras de negocio e integracoes externas
- Uso de logs para diagnostico
- Nomes de arquivos e pastas previsiveis
- Fluxo visual com cards, campos e botoes em tema escuro
- Organizacao em multiplos modulos para facilitar Windows, Linux e Android

O que nao sera reaproveitado diretamente:

- Captura de tela em tempo real
- Captura de microfone e audio do sistema
- Fluxo de gravacao de desktop

## Ferramentas que provavelmente serao usadas

- `yt-dlp` para download de videos, audio e legendas
- `ffmpeg` para cortes, conversao e remux
- Uma interface local simples para Windows, ou uma CLI bem organizada no inicio
- Pasta de download configuravel pelo usuario
- Kotlin + Compose Desktop para a interface Windows/Linux
- Android com Jetpack Compose para a versao mobile futura

## Estrutura atual

- `core/` modelos e regras compartilhadas entre plataformas
- `desktop/` interface principal para Windows e Linux
- `android/` base inicial para a versao Android
- `downloads/` pasta padrao de saida
- `logs/` historico das operacoes
- `commit.bat` e `commit.sh` atalhos de commit para Windows e Bash

## Fluxo esperado

1. O usuario cola a URL do video.
2. A ferramenta baixa o conteudo para a pasta escolhida.
3. O usuario escolhe se quer video completo, trecho ou legendas.
4. O corte e gerado localmente com `ffmpeg`.
5. Os arquivos finais ficam organizados para reutilizacao futura.

## Status atual

- A base do projeto ja foi criada.
- A interface desktop inicial ja existe para Windows.
- O modulo Android esta preparado como ponto de partida.
- A parte de download com `yt-dlp`, cortes com `ffmpeg` e legendas ainda sera implementada.

## Proximos passos

1. Implementar download com `yt-dlp`.
2. Implementar cortes com `ffmpeg`.
3. Implementar exportacao de legendas.
4. Conectar a interface desktop aos comandos reais.
5. Evoluir a versao Android depois da base desktop estabilizada.
