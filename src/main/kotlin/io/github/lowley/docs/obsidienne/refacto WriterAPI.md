### Objectif  
Refactorer WriterAPI en un projet cohérent appelé `periscope`, en conservant l’architecture et les concepts de la « version 2 » (state machines Boat/Submarine) et en éliminant progressivement les restes de la v1 (packages `emitter`/`receiver`), tout en maintenant la compatibilité pour Viewer (desktop “Boat”) et l’app Android “Submarine”.  
  
---  
  
### État actuel (vu depuis Viewer + vos indications)  
- Viewer consomme `io.github.lowley.version2.boat.ISurfaceLogging` et `SurfaceLogging` (ex: `ViewerViewModel.kt`).  
- La DI desktop référence `io.github.lowley.receiver.DeviceAPI` (v1), et un composant `DeviceAPIComponent` local.  
- Côté WriterAPI, la v2 (Boat/Submarine) s’appuie encore sur des briques de la v1 (ex: `emitter` pour envoi/parsing RichLog, `receiver` pour device access).  
- Vous souhaitez à terme ne garder que `boat`, `submarine`, `common` (hérités de v2) et faire disparaître le package `version2` lui‑même, en rehomant les enfants.   
  
---  
  
### Cible proposée (architecture Periscope)  
- Coordonnées Maven (exemple) : `io.github.lowley:periscope-<module>:$version`  
- Modules Gradle (lib JVM/Android selon besoin) :  
  - `periscope-common` (pur Kotlin):  
    - Modèles RichText/RichLog, `CommunicationAPI` générique, codecs (parse/serialize), erreurs, utils communs, abstractions de transport.  
  - `periscope-transport` (JVM + Android):  
    - Adaptateurs concrets de transport (ex: ADB/logcat, TCP, fichiers), remplaçant `emitter`/`receiver`.   
    - Interfaces SPI pour brancher des transports custom.  
  - `periscope-boat` (JVM desktop):  
    - State machine `SurfaceStateMachine`, façades `ISurfaceLogging`/`SurfaceLogging`, log streams (`Flow`), mapping RichLog → UI, stratégies de buffer.  
  - `periscope-submarine` (Android):  
    - State machine `DiveStateMachine`, `IDiveLogging`/`DiveLogging`, sources d’événements (logcat/app), pipeline vers codecs/transport.  
  - `periscope-di` (optionnel, multi‑platform variants):  
    - Modules Koin/Hilt pré‑câblés pour `common`, `transport`, `boat`, `submarine` (pour vous éviter de dupliquer l’assemblage dans Viewer/Android).  
  - `periscope-legacy` (facultatif, temporaire):  
    - Shims de compat (typealiases et façades forwarders) exposant les anciens packages `io.github.lowley.version2.*`, `io.github.lowley.emitter`, `io.github.lowley.receiver` en `@Deprecated(message = "Use periscope-*", level = WARNING)`.  
  
Arborescence cible (packages Kotlin):  
- `periscope.common.*`  
- `periscope.transport.*`  
- `periscope.boat.*`  
- `periscope.submarine.*`  
- `periscope.di.*` (si retenu)  
- `io.github.lowley.version2.*` (deprecated shims) — transitoire  
- `io.github.lowley.emitter|receiver` (deprecated shims) — transitoire  
  
---  
  
### Plan de migration par étapes  
1) Cartographie & gel des API  
- Lister les APIs réellement consommées par:  
  - Viewer: `ISurfaceLogging`, `SurfaceLogging`, flux/logs, toute dépendance à `receiver.DeviceAPI`.  
  - App Android: `IDiveLogging`, `DiveLogging`, et toutes dépendances `emitter`.  
- Décider les signatures « stables » à conserver dans Periscope 1.x.  
- Geler ces APIs dans des interfaces côté `periscope-boat` et `periscope-submarine`.  
  
2) Extraction des communs  
- Déplacer `CommunicationAPI`, modèles RichLog/RichText, codecs, helpers en `periscope-common`.  
- Extraire ce que la v2 pompe encore depuis `emitter/receiver` et le re‑modéliser en abstractions `periscope.common.transport` (interfaces) + codecs communs.  
  
3) Transport unifié  
- Implémenter dans `periscope-transport`:  
  - Adaptateur ADB/Logcat (Android) qui remplace les usages de `receiver`/`emitter` côté Submarine.  
  - Adaptateur Desktop (si besoin) pour lecture de flux distants/fichiers.  
- Injecter ces adaptateurs depuis `periscope-di` (Koin) pour que Boat/Submarine ne dépendent plus de v1.  
  
4) Rehoming des packages v2 → Periscope  
- `io.github.lowley.version2.boat` → `periscope.boat` (mover/rename safe via IDE).  
- `io.github.lowley.version2.submarine` → `periscope.submarine`.  
- `io.github.lowley.common` → `periscope.common` (si tel package existe dans WriterAPI).  
  
5) Shims de compat (transitoires)  
- Créer `periscope-legacy` avec:  
  - `typealias ISurfaceLogging = periscope.boat.ISurfaceLogging`  
  - `object SurfaceLogging : periscope.boat.SurfaceLogging by periscope.boat.SurfaceLoggingImpl` (ou forward simple)  
  - Idem pour `IDiveLogging`, `DiveLogging`.  
  - Re-export minimal de `emitter`/`receiver` si certains appels externes existent encore, mais redirigés vers `periscope.transport`.  
- Annoter tout en `@Deprecated` avec message précis+URL de migration.  
  
6) DI et modules d’application  
- Conserver un module DI minimal nécessaire au fonctionnement:  
  - Dans `periscope-di`, fournir `module { single<ISurfaceLogging> { SurfaceLogging(/* deps */) } … }`.  
- Dans Viewer:  
  - Remplacer les imports `io.github.lowley.version2.boat.*` par `periscope.boat.*` (ou garder shims dans un premier temps).  
  - Remplacer l’usage direct de `io.github.lowley.receiver.DeviceAPI` par un provider `periscope.transport.*` injecté.  
  
7) Build & publication  
- Scinder WriterAPI en sous‑modules Gradle ou Multi‑module dans un seul repo.  
- Publier: `periscope-common`, `periscope-transport`, `periscope-boat`, `periscope-submarine`, `periscope-di`.  
- Versionning: `1.0.0` pour Periscope, `periscope-legacy` en `0.x` (ou placé dans le même versionnement mais marqué deprecated).  
  
8) Tests & vérifs  
- Unit tests:  
  - Codecs RichLog (parse/serialize, round‑trip).  
  - State machines: transitions nominales/erreurs.  
- Integration tests:  
  - Pipeline Submarine: source → codec → transport.  
  - Pipeline Boat: transport → codec → flux UI.  
- Manuels:  
  - Viewer affiche les RichLogs sans régression.  
  - Android Submarine envoie/trace correctement sur le canal choisi.  
  
9) Nettoyage progressif  
- Phase 1: livrer avec shims; communiquer la migration.  
- Phase 2: retirer shims dans la prochaine major (2.0.0), supprimer dossiers v1.  
  
---  
  
### Checklist de tâches concrètes  
- Inventaire rapide (1 jour):  
  - [ ] Lister classes/paquets réellement utilisés par Viewer et par l’app Android.  
  - [ ] Identifier ce qui, dans `emitter/receiver`, est purement « transport » et ce qui est codec.  
- Découpage code (2–4 jours):  
  - [ ] Créer modules Gradle `periscope-common`, `periscope-transport`, `periscope-boat`, `periscope-submarine`, `periscope-di`.  
  - [ ] Déplacer `CommunicationAPI`, modèles/codec RichLog → `periscope-common`.  
  - [ ] Déplacer state machines/logging facades Boat/Submarine → nouveaux packages `periscope.boat|submarine`.  
  - [ ] Implémenter adaptateurs transport (ADB/logcat etc.) dans `periscope-transport`.  
  - [ ] Fournir modules Koin dans `periscope-di`.  
- Compat (1 jour):  
  - [ ] Ajouter `periscope-legacy` avec typealiases/forwarders `@Deprecated` pour anciens namespaces.  
  - [ ] Documenter la table de migration imports→nouveaux packages.  
- Intégration apps (1–2 jours):  
  - [ ] Migrer Viewer aux nouveaux imports; vérifier `ViewerViewModel` compile et reçoit le flux.  
  - [ ] Migrer l’app Android; vérifier envoi/parse OK.  
- Qualité (en parallèle):  
  - [ ] Tests unitaires codecs et machines d’état.  
  - [ ] Tests d’intégration transport.  
  - [ ] CI: build multi-modules, publication locale (mavenLocal) puis remote (JitPack/MavenCentral).  
- Cleanup (post‑release):  
  - [ ] Marquer v1 comme deprecated dans README/CHANGELOG.  
  - [ ] Planifier suppression shims à la prochaine major.  
  
---  
  
### Mapping proposé (préliminaire)  
- `io.github.lowley.version2.boat.*` → `periscope.boat.*`  
- `io.github.lowley.version2.submarine.*` → `periscope.submarine.*`  
- `io.github.lowley.common.*` → `periscope.common.*`  
- `io.github.lowley.emitter.*` → `periscope.transport.*` (ou `periscope.common.codec` si c’est du parsing)  
- `io.github.lowley.receiver.*` → `periscope.transport.*`  
  
Exemples concrets à migrer:  
- `ISurfaceLogging`, `SurfaceLogging` → `periscope.boat.ISurfaceLogging`, `periscope.boat.SurfaceLogging`.  
- `IDiveLogging`, `DiveLogging` → `periscope.submarine.IDiveLogging`, `periscope.submarine.DiveLogging`.  
- `CommunicationAPI` → `periscope.common.CommunicationAPI`.  
  
---  
  
### Stratégie de compatibilité minimale  
- Dans `periscope-legacy`, fournir:  
```kotlin  
@file:Suppress("DEPRECATION")  
package io.github.lowley.version2.boat  
@Deprecated("Use periscope.boat.ISurfaceLogging")  
typealias ISurfaceLogging = periscope.boat.ISurfaceLogging@Deprecated("Use periscope.boat.SurfaceLogging")  
object SurfaceLogging : periscope.boat.SurfaceLogging by periscope.boat.SurfaceLoggingImpl```  
- Idem pour `submarine`, et pour `emitter/receiver` exposer soit des `typealias` de DTO/codec, soit des façades qui délèguent vers `periscope.transport`.  
  
---  
  
### Impacts côté Viewer (projet que vous avez partagé)  
- `ViewerViewModel` : remplacer imports `io.github.lowley.version2.boat.*` par `periscope.boat.*` (ou s’appuyer sur shims pour une première release sans changements).  
- `basics/appModule.kt` :  
  - Remplacer `io.github.lowley.receiver.DeviceAPI` par un binding `periscope.transport.*` (ex: `DeviceTransport`), injecté dans `SurfaceLogging` via `periscope-di`.  
  - Garder le module `appModule` (Koin) — critique pour l’injection.  
  
---  
  
### Questions de clarification  
1) Quels transports devez‑vous supporter à court terme ? (ADB/logcat uniquement ? TCP ? fichiers ?)  
2) Souhaitez‑vous livrer Periscope en plusieurs artefacts (common/boat/submarine/transport/di) ou en un seul JAR monolithique au début ?  
3) Avez‑vous des consommateurs externes de `emitter/receiver` hors Viewer/Submarine ? Si oui, on priorise des shims plus complets.  
4) Acceptez‑vous une phase transitoire où `Viewer` continue d’importer les anciens packages avec `@Deprecated` pendant une release ?  
5) Vos contraintes de versioning/publication (JitPack actuel `io.github.lowley:writer-api:1.0.4`) — conservez‑vous le groupId `io.github.lowley` et passez‑vous à `periscope-*` ?  
  
---  
  
### Prochaine étape proposée  
- Je commence par l’inventaire des usages réels (Viewer + Android) et je rédige la table de migration détaillée classe‑par‑classe. Donnez‑moi confirmation sur les points de clarification ci‑dessus et je vous fournis ensuite un plan d’exécution daté + la structure exacte des modules Gradle et packages, prête pour les refactors IDE sans casse.