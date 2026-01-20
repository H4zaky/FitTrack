ESCOLA SUPERIOR DE TECNOLOGIA E GESTÃO — POLITÉCNICO DO PORTO  
LICENCIATURA EM ENGENHARIA INFORMÁTICA  
Unidade Curricular: Computação Móvel e Ubíqua (CMU)

# FitTrack — Aplicação de Registo e Monitorização de Atividade Física

**Aluno:** Carlos Leite (N.º 8200377)  
**Ano letivo:** 2025/2026  
**Data:** 01/2026  

> Relatório de Trabalho Prático apresentado para cumprimento dos requisitos necessários à obtenção de aprovação à Unidade Curricular de Computação Móvel e Ubíqua na Licenciatura em Engenharia Informática pela Escola Superior de Tecnologia e Gestão do Instituto Politécnico do Porto. :contentReference[oaicite:0]{index=0}

---

## Declaração de Integridade

Eu, Carlos Leite (8200377), aluno da Licenciatura em Engenharia Informática da Escola Superior de Tecnologia e Gestão do Instituto Politécnico do Porto, declaro que não fiz plágio nem auto-plágio. O trabalho intitulado **“FitTrack”** é original e da minha autoria, não tendo sido usado previamente para qualquer outro fim. Declaro ainda que todas as fontes usadas estão citadas no texto e na bibliografia final, segundo as regras de referenciação adotadas na instituição. :contentReference[oaicite:1]{index=1}

---

# RESUMO

O presente Trabalho Prático é realizado no âmbito da Unidade Curricular de Computação Móvel e Ubíqua da Licenciatura em Engenharia Informática (ESTG/IPP). :contentReference[oaicite:2]{index=2}

O **FitTrack** é uma aplicação Android focada em promover hábitos saudáveis, permitindo ao utilizador **registar e consultar atividades físicas** (ex.: caminhada, corrida e outros), com **histórico offline (Room)** e **backup/sincronização no Firebase (Firestore)**. O registo inclui dados essenciais por sessão, como **localização inicial/final, pontos intermédios (trajeto), velocidade/altitude por ponto**, e **estatísticas finais** (distância, duração, velocidade média e elevação acumulada). :contentReference[oaicite:3]{index=3}

Adicionalmente, a aplicação suporta **perfil do utilizador**, **adição de amigos por telefone**, e **rankings mensais** (entre amigos e global) para motivação, incluindo **notificações quando o utilizador é ultrapassado por um amigo no ranking**. 

---

# ÍNDICE

- [Cap I Contextualização e Motivação](#cap-i-contextualização-e-motivação)  
  - [1. Introdução](#1-introdução)  
    - [1.1 Objetivos](#11-objetivos)  
    - [1.2 Resultados](#12-resultados)  
    - [1.3 Organização do documento](#13-organização-do-documento)  
  - [2. Fundamentação teórica](#2-fundamentação-teórica)  
- [Cap II Conceptualização do Problema](#cap-ii-conceptualização-do-problema)  
  - [3. Requisitos](#3-requisitos)  
  - [4. Arquitetura conceptual](#4-arquitetura-conceptual)  
- [Cap III Metodologia de Operacionalização do Trabalho](#cap-iii-metodologia-de-operacionalização-do-trabalho)  
  - [5. Processo e metodologia de trabalho](#5-processo-e-metodologia-de-trabalho)  
  - [6. Desenvolvimento da solução](#6-desenvolvimento-da-solução)  
- [Cap IV Discussão dos Resultados](#cap-iv-discussão-dos-resultados)  
  - [7. Apresentação e discussão dos resultados](#7-apresentação-e-discussão-dos-resultados)  
  - [8. Apresentação e discussão dos impedimentos](#8-apresentação-e-discussão-dos-impedimentos)  
- [Cap V Conclusão](#cap-v-conclusão)  
  - [9. Reflexão crítica dos resultados](#9-reflexão-crítica-dos-resultados)  
  - [10. Conclusão e trabalho futuro](#10-conclusão-e-trabalho-futuro)  
- [Bibliografia](#bibliografia)

---

# ÍNDICE DE FIGURAS

- **Figura 1** — Fluxo entre as screens (Auth → Home → Registo → Histórico → Detalhe → Social/Rankings)  
- **Figura 2** — Login Screen  
- **Figura 3** — Register Screen  
- **Figura 4** — Home / Início de Atividade (Manual)  
- **Figura 5** — Histórico (Lista)  
- **Figura 6** — Histórico (Mapa + cartões)  
- **Figura 7** — Detalhe de sessão (estatísticas + mapa)  
- **Figura 8** — Social (Amigos + Rankings)  
- **Figura 9** — Estrutura Firestore (users / sessions / points / leaderboards)

---

# Cap I Contextualização e Motivação

## 1. Introdução

O trabalho prático proposto na UC de CMU consiste no desenvolvimento de uma aplicação Android que suporte uma das resoluções de ano novo mais comuns: **tornar-se mais ativo e saudável**, registando atividades como **passos, caminhadas, corridas e outros**. :contentReference[oaicite:5]{index=5}

A principal motivação do FitTrack é entregar uma solução **simples**, **offline-first** e **orientada a dados**, onde o utilizador consegue:
- registar sessões com trajeto (GPS) e métricas;
- manter histórico acessível offline;
- sincronizar/guardar no Firebase como backup;
- competir em rankings mensais (amigos e global).

### 1.1 Objetivos

Os objetivos do trabalho são alinhados com os objetivos definidos para o projeto na UC: dominar o desenvolvimento Android, aplicar pesquisa autónoma e coordenar um projeto com práticas de versionamento. :contentReference[oaicite:6]{index=6}

Objetivos específicos do FitTrack:
- Implementar uma aplicação Android moderna com **Jetpack Compose** e **Material Design**.
- Persistência local com **Room**, suportando utilização **offline**.
- Sincronização e backup com **Firebase (Firestore + Auth)**.
- Recolha e apresentação de dados de **localização e mapa**.
- Componentes de motivação: **amigos + rankings + notificações**. :contentReference[oaicite:7]{index=7}

### 1.2 Resultados

Como resultado, é apresentada uma aplicação que permite:
- autenticação e perfil;
- registo/consulta de atividades com estatísticas;
- visualização em lista e mapa;
- sincronização com Firebase;
- rankings mensais com notificação de ultrapassagem. 

### 1.3 Organização do documento

O relatório segue a organização típica: contextualização, requisitos e arquitetura, metodologia e desenvolvimento, discussão e conclusão. :contentReference[oaicite:9]{index=9}

---

## 2. Fundamentação teórica

O FitTrack combina vários pilares de CMU: **persistência local**, **cloud**, **sensores**, **serviços/workers**, **notificações**, **UI moderna** e **integração com API externa**. :contentReference[oaicite:10]{index=10}

Conceitos principais:
- **Jetpack Compose + Material Design**: construção declarativa da UI e consistência visual.
- **Room (SQLite)**: cache local para histórico e operação offline.
- **Firebase Auth + Firestore**: autenticação e armazenamento cloud (backup/sincronização e ranking público).
- **Localização (GPS) + Mapas**: recolha de pontos e visualização do trajeto.
- **WorkManager / tarefas em background**: sincronização e vigilância de rankings.
- **Notificações**: alerta quando um amigo ultrapassa o utilizador no ranking.
- **Retrofit (REST)**: consulta de meteorologia para enriquecer o registo da sessão. :contentReference[oaicite:11]{index=11}

---

# Cap II Conceptualização do Problema

## 3. Requisitos

A aplicação foi construída para cumprir o enunciado: registo de atividades com dados de trajeto e métricas, histórico offline com backup Firebase, amigos por telefone, e leaderboards mensais, com listagens em lista e mapa e cartões de detalhe. :contentReference[oaicite:12]{index=12}

### 3.1 Requisitos obrigatórios (síntese)

Com base no enunciado, foram considerados como requisitos obrigatórios (entre outros): Git/GitLab, multi-idioma, Compose+Material, Navigation Drawer/Scaffold/Navigation, integração com elementos Android (contactos/dialer), imagens locais, Room+Firebase offline, notificações, service/monitorização, mapas, sensores adicionais e Retrofit. 

---

## 4. Arquitetura conceptual

**Figura 1 — Fluxo entre as screens** (inserir screenshot do diagrama)

Arquitetura por camadas (visão conceptual):
- **UI (Compose)**: AuthGate, Home, Histórico (lista/mapa), Detalhe de sessão, Social/Rankings.
- **Core/Domain**: repositórios e casos de uso (ProfileRepository, FirebaseSync, LeaderboardCacheRepository).
- **Data Local (Room)**: `activity_sessions`, `track_points`, `friends`, `leaderboard_snapshot`.
- **Data Remota (Firestore)**:
  - `/users/{uid}`: perfil (privado na escrita; leitura autenticada para procurar amigos)
  - `/users/{uid}/sessions/{sid}`: sessões e dados principais
  - `/users/{uid}/sessions/{sid}/points/{pid}`: pontos GPS
  - `/leaderboards/{month}/users/{uid}`: ranking mensal (público para leitura autenticada)
- **Background**: WorkManager para refresh de rankings e sync quando necessário.

Regras de segurança no Firestore (resumo):
- perfis: leitura autenticada; escrita apenas do dono.
- sessões/pontos: leitura/escrita apenas do dono.
- leaderboards: leitura autenticada; escrita apenas do próprio documento. (baseado no conjunto de regras configurado).  

---

# Cap III Metodologia de Operacionalização do Trabalho

## 5. Processo e metodologia de trabalho

A implementação seguiu uma abordagem incremental:
1. **Base do projeto**: Compose, Navigation, Scaffold/Drawer.
2. **Autenticação e perfil**: Firebase Auth + coleção `users`.
3. **Persistência local**: Room (entidades/DAOs/migrations).
4. **Registo de atividade**: criação/finalização de sessões e pontos.
5. **Sincronização**: upload/download e reconciliação (offline-first).
6. **Rankings + notificações**: cache local (`leaderboard_snapshot`) e worker de vigilância.
7. **Polimento**: estados de loading, consistência do histórico, detalhe e mapa.

Critério chave: **o utilizador nunca perde acesso ao histórico**, mesmo sem internet (Room), mantendo **backup** no Firebase para continuidade entre dispositivos. :contentReference[oaicite:14]{index=14}

---

## 6. Desenvolvimento da solução

### 6.1 Persistência local (Room)

Tabelas principais:
- `activity_sessions`: sessão com dados finais (distância, duração, modo, métricas e meteorologia).
- `track_points`: pontos GPS (timestamp, lat, lon, accuracy, speed, altitude) ligados por FK a `activity_sessions`.
- `friends`: lista de amigos (ownerUid + phone) e opcionalmente uid resolvido.
- `leaderboard_snapshot`: cache mensal (month + uid + name + métricas) para ranking offline.

### 6.2 Firestore (backup/sincronização)

Estruturas (sugeridas e usadas no projeto):
- `users/{uid}`: `uid`, `name`, `email`, `phone`, `createdAt`
- `users/{uid}/sessions/{sid}`: dados principais da sessão
- `users/{uid}/sessions/{sid}/points/{pid}`: pontos da sessão
- `leaderboards/{yyyy-MM}/users/{uid}`: agregados mensais (`distanceKm`, `steps`, `sessions`, `name`)

### 6.3 Sincronização (offline-first)

Estratégia típica implementada:
- **Local é a fonte de operação**: criar/editar/apagar afeta Room.
- **Sync para cloud**:
  - upload de sessões e pontos criados/alterados;
  - remoção remota quando o utilizador apaga localmente;
  - download/reconcile para preencher dispositivo “novo” e garantir consistência.

### 6.4 Rankings e notificações

- `LeaderboardCacheRepository` atualiza a cache local:
  - **Global Top N** (consulta remota → upsert no `leaderboard_snapshot`)
  - **Friends + Eu** (por conjunto de uids)
- Worker (WorkManager) verifica:
  - se algum amigo ultrapassou o utilizador no mês corrente;
  - se sim, dispara uma **notificação** e guarda flag local para evitar spam.

### 6.5 UI e navegação

- Compose com componentes reutilizáveis (ex.: cartão de detalhe).
- Histórico com dois formatos exigidos:
  - **lista + cartão de detalhes**
  - **mapa + marcadores + cartão de detalhes** :contentReference[oaicite:15]{index=15}

---

# Cap IV Discussão dos Resultados

## 7. Apresentação e discussão dos resultados

Funcionalidades entregues no FitTrack (resumo):
- **Autenticação e perfil**: login/registo e gestão de dados do utilizador (nome/telefone).
- **Registo de sessões**: criação e finalização com estatísticas finais.
- **Pontos e mapa**: registo de pontos intermédios e visualização do trajeto.
- **Histórico offline**: listagem e detalhe sempre disponíveis localmente (Room).
- **Backup Firebase**: sessões e pontos persistidos no Firestore.
- **Amigos por telefone**: lista local de amigos (com normalização de contacto).
- **Rankings mensais**: global e amigos, com nomes de utilizador na apresentação.
- **Notificações**: aviso quando ultrapassado no ranking. :contentReference[oaicite:16]{index=16}

## 8. Apresentação e discussão dos impedimentos

Impedimentos típicos do desenvolvimento:
- Complexidade de **sincronização bidirecional** (conflitos, apagamentos e reconciliação).
- Gestão de **migrações Room** ao evoluir o schema.
- Estados de UI (loading) e garantias de não entrar em ciclo de recomposição.
- Dependência de internet apenas para sync/leaderboard, mantendo offline-first.

---

# Cap V Conclusão

## 9. Reflexão crítica dos resultados

A tabela seguinte resume o cumprimento dos requisitos do enunciado (ajusta “Cumprido/Parcial/Não cumprido” conforme a tua versão final):

### Requisitos Obrigatórios (RO)

- **RO-01 GitLab e Git para gestão e versionamento do projeto** — **Cumprido** :contentReference[oaicite:17]{index=17}  
- **RO-02 Localização da aplicação em vários idiomas** — **Cumprido / Parcial** :contentReference[oaicite:18]{index=18}  
- **RO-03 Material Design + Jetpack Compose** — **Cumprido** :contentReference[oaicite:19]{index=19}  
- **RO-04 Navigation Drawer + Scaffold + Navigation** — **Cumprido** :contentReference[oaicite:20]{index=20}  
- **RO-05 Interação com elementos Android (Contactos/Dialer/Mensagens, etc.)** — **Cumprido / Parcial** :contentReference[oaicite:21]{index=21}  
- **RO-06 Imagens locais** — **Cumprido / Parcial** (ex.: fotos antes/depois via URI local) :contentReference[oaicite:22]{index=22}  
- **RO-07 Base de dados online com cache local (Room + Firebase)** — **Cumprido** :contentReference[oaicite:23]{index=23}  
- **RO-08 Integração Retrofit (REST)** — **Cumprido / Parcial** (meteorologia) :contentReference[oaicite:24]{index=24}  
- **RO-09 Estratégias adaptabilidade (bateria/luz, etc.)** — **Parcial / Não cumprido** :contentReference[oaicite:25]{index=25}  
- **RO-10 Uso de serviços Android para monitorização/atualização** — **Parcial** (WorkManager; serviço dedicado se existir) :contentReference[oaicite:26]{index=26}  
- **RO-11 Localização + mapas e listas separadas** — **Cumprido** :contentReference[oaicite:27]{index=27}  
- **RO-12 Sensores adicionais (não GPS)** — **Cumprido / Parcial** (ex.: passos) :contentReference[oaicite:28]{index=28}  
- **RO-13 Notificações quando ultrapassado por amigo** — **Cumprido** :contentReference[oaicite:29]{index=29}  

## 10. Conclusão e trabalho futuro

O FitTrack cumpre o objetivo de apoiar hábitos saudáveis com registo de atividade física, histórico offline e mecanismos de motivação (amigos e rankings). A arquitetura offline-first com sincronização no Firebase reduz o risco de perda de dados e melhora a experiência do utilizador em mobilidade. :contentReference[oaicite:30]{index=30}

Trabalho futuro (melhorias):
- deteção automática de tipo de atividade com maior robustez (sensores + Activity Recognition);
- tratamento avançado de conflitos de sync (merge por timestamps);
- regras de conservação (limpeza de pontos antigos, compressão do trajeto);
- melhorias de UX (filtros no histórico, exportação GPX/CSV);
- eventos do sistema (bateria fraca / modo poupança) para adaptar tracking e sync. :contentReference[oaicite:31]{index=31}

---

# Bibliografia

[1] **Enunciado do Trabalho Prático — Computação Móvel e Ubíqua (2025/2026)**. :contentReference[oaicite:32]{index=32}  
[2] Documentação Android Developers: Jetpack Compose, Room, WorkManager, Location.  
[3] Documentação Firebase: Authentication e Cloud Firestore.  
[4] Documentação Retrofit e API de meteorologia utilizada no projeto.
