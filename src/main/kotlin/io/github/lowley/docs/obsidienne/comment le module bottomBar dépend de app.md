> J'ai le module app, le module principal de mon application Dossier Sigma qui est avec injection yield. Puis avoir le module bottom bar avec injection coin. Sachant qu'il y a des chances que le module app ait besoin au moins du point d'entrée du module bottom bar, et que le module bottom bar ait besoin aussi de choses qu'il y a dans le module app.

En gros, dans l’état actuel : tel que tu le formules, **ça ne peut pas marcher** tel quel.
Tu as :

- `:app` (module principal, DI avec Hilt — j’imagine que “yield” = Hilt),
    
- `:bottombar` (module secondaire, tu voudrais DI avec Koin),
    
- et tu imagines :
    
    - `:app` a besoin du “point d’entrée” du module `:bottombar`,
        
    - `:bottombar` a besoin de choses qui sont dans `:app`.
        

Ça, c’est **une dépendance circulaire** au niveau Gradle, et Gradle ne l’acceptera jamais.

---

## 1. Règle structurelle : pas de cycle entre modules

En Gradle, les dépendances de modules doivent être **orientées** :

- soit `:app` → `:bottombar`,
    
- soit `:bottombar` → `:app`,
    
- mais **jamais les deux**.
    

Donc tu dois choisir une direction, puis organiser tes “besoins mutuels” autrement (interfaces, module “core”, etc.).

La bonne direction ici, vu que `:app` est l’APK final, c’est :

```text
:app  --->  :bottombar
```

Donc :

- `:app` **dépend de** `:bottombar` (il peut utiliser les composables de la bottom bar, etc.),
    
- `:bottombar` ne doit **pas** dépendre de `:app`.
    

---

## 2. Que faire si `:bottombar` “a besoin de choses de :app” ?

On applique l’**inversion de dépendance** :

1. Tu identifies ce dont la bottom bar a besoin :
    
    - navigation (par ex. `navigateTo(FolderId)`),
        
    - accès au ViewModel principal,
        
    - actions “globales” (ouvrir paramètres, rafraîchir dossier, etc.).
        
2. Tu mets **uniquement les interfaces** de ces besoins dans un module neutre, par exemple :
    

```text
:core:api
```

Exemples :

```kotlin
// :core:api
interface SigmaNavigator {
    fun openFolder(id: FolderId)
    fun openSettings()
}

interface SigmaStatusProvider {
    val currentFolderName: StateFlow<String>
    val unreadCount: StateFlow<Int>
}
```

3. Ton module `:bottombar` dépend de `:core:api`, pas de `:app` :
    

```gradle
// bottombar/build.gradle.kts
dependencies {
    implementation(project(":core:api"))
    // + libs UI, etc.
}
```

4. Dans `:bottombar`, tu ne connais **que** ces interfaces :
    

```kotlin
@Composable
fun BottomBar(
    navigator: SigmaNavigator,
    statusProvider: SigmaStatusProvider,
) {
    // UI qui appelle navigator.openFolder(...) ou observe statusProvider...
}
```

5. Dans `:app`, tu fournis les implémentations concrètes, avec Hilt :
    

```kotlin
// :app - impl des interfaces
class SigmaNavigatorImpl @Inject constructor(
    private val appNavigator: AppNavigator // ton système de nav interne
) : SigmaNavigator {
    override fun openFolder(id: FolderId) = appNavigator.openFolder(id)
}

class SigmaStatusProviderImpl @Inject constructor(
    private val sigmaViewModel: SigmaViewModel
) : SigmaStatusProvider {
    override val currentFolderName = sigmaViewModel.currentFolderName
    override val unreadCount = sigmaViewModel.unreadCount
}
```

Puis dans ton `Scaffold` de `:app` :

```kotlin
@Composable
fun SigmaMainScreen(
    navigator: SigmaNavigator = hiltViewModel<NavigatorHolder>().navigator,
    statusProvider: SigmaStatusProvider = hiltViewModel<StatusHolder>().statusProvider,
) {
    Scaffold(
        bottomBar = {
            BottomBar(
                navigator = navigator,
                statusProvider = statusProvider
            )
        }
    ) { /* ... */ }
}
```

Résultat :

- `:app` utilise `:bottombar`,
    
- `:bottombar` utilise des **interfaces** définies dans `:core:api`,
    
- les implémentations concrètes vivent dans `:app`, via Hilt,
    
- **aucun cycle de dépendance**.
    

---

## 3. Mélanger Hilt (app) et Koin (bottombar) ?

Techniquement, tu peux :

- `:app` démarre Hilt (normal),
    
- dans `onCreate()` ou via une init, tu démarres Koin pour certaines features,
    
- la bottom bar fait son DI via Koin.
    

Mais pour un **simple module UI de bottom bar**, ça complique beaucoup :

- deux graphes d’injection à maintenir,
    
- deux syntaxes,
    
- des questions de “qui est responsable de quoi ?”.
    

Franchement, pour ce type de module :

- soit tu le rends **agnostique de tout framework DI** (tu passes tout en paramètres ou via CompositionLocals),
    
- soit tu restes **full Hilt** dans tout le projet,
    
- mais avoir `:app` en Hilt et `:bottombar` en Koin ne t’apporte pas grand-chose, à part de la complexité.
    

---

## 4. Schéma de modules suggéré

Par exemple :

```text
:core:api        → interfaces communes (navigator, providers, etc.)
:core:model      → types purs (FolderId, SigmaState, etc.)

:bottombar       → composables de la bottom bar, dépend de :core:api, :core:model
:app             → implémente les interfaces de :core:api avec Hilt
                  et utilise BottomBar(...) dans son UI
```

Dépendances Gradle :

```text
:app        → :bottombar, :core:api, :core:model
:bottombar  → :core:api, :core:model
```

Aucun module ne dépend de `:app`, donc pas de cycle.

---

Si tu veux, au prochain message, tu peux me dire **concrètement** ce que la bottom bar doit faire (actions, états qu’elle affiche), et je te proposerai un jeu d’interfaces `:core:api` taillé pile pour Dossier Sigma, en restant compatible avec ta structure actuelle.

