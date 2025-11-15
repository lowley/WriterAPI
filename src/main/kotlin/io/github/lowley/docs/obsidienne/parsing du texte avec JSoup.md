
Pour mon projet de log, j'ai une chaîne de caractère qui devrait contenir un log, un log enrichi, c'est-à-dire que par exemple, pour des fonctions simples, je voudrais pouvoir ajouter de la mise en texte, de la mise en page, de la mise en forme de texte, par exemple avec « grâce », « souligné », « taille variable », « couleur de texte variable », etc. J'avais pensé à mettre en forme avec de l'HTML, qui est connu et standard, tout sur une seule ligne, puisqu'à priori, il ne s'agit pas de gestion de paragraphe. Mais le problème, c'est que moi, ce que j'enverrais au receveur, c'est-à-dire au viewer, c'est un objet « rich log event », et donc j'avais pensé, mais ce n'est pas forcément obligatoire, à parser ce HTML. Est-ce que tu connais des parseurs HTML ? Et si oui, est-ce que je peux facilement utiliser le résultat du parsing pour construire mon objet ? Par exemple, que le texte serait transformé en un objet, en une classe « texte » qui comprendrait une liste de classes « subtexts » et « subtexts » serait une data class qui pourrait, par exemple, contenir la valeur « gras » qui serait un boolean. Et à l'endroit, si on écrit « bonjour, monde » avec « monde » en « gras », alors il y aurait deux subclasses par exemple « bonjour, espace » qui serait la première subclass et « monde » qui serait la deuxième subclass, la deuxième instance de subclass qui, elle, aurait un indicateur de « gras » qui serait « à vrai ». Est-ce que tu comprends l'idée ? Et est-ce que pour le parsing, c'est possible de faire quelque chose de simple ?

---

Oui, je comprends très bien l’idée 😊  
Tu veux en gros :

- côté **émetteur** (sur le téléphone) : produire une **chaîne unique** qui encode du texte riche (gras, souligné, couleur, taille…),
    
- côté **viewer** : la transformer en un **objet structuré** du genre
    

```kotlin
data class RichText(
    val segments: List<RichSegment>
)

data class RichSegment(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val color: String? = null,   // par ex. "#ff0000"
    val size: Float? = null      // taille relative
)
```

où ton exemple  
« bonjour␣monde » avec **monde** en gras  
devient :

- segment 1 : `"bonjour "` (bold = false)
    
- segment 2 : `"monde"` (bold = true)
    

## 1. Est-ce que HTML est une bonne idée pour ça ?

Oui, **HTML comme format intermédiaire** est tout à fait défendable :

- c’est **standard**, bien documenté ;
    
- tu peux te limiter à un **sous-ensemble minuscule** : `<b>`, `<i>`, `<u>`, `<span style="color:…">`, etc. ;
    
- c’est facile à générer côté émetteur.
    

Exemple de ligne de log :

```html
bonjour <b>monde</b>
```

Tu peux décider que ton viewer **ne gère que** :

- `<b>`, `<strong>` → `bold = true`
    
- `<i>`, `<em>` → `italic = true`
    
- `<u>` → `underline = true`
    
- `<span style="color:#ff0000">` → `color = "#ff0000"`
    

Tout le reste est ignoré.

Donc tu n’es pas obligé de respecter _tout_ HTML, tu l’utilises juste comme un **mini marquage de style**.

## 2. Comment parser facilement ce HTML ?

Sur JVM / Android, le plus simple, c’est d’utiliser un parseur existant, typiquement **jsoup** (c’est la référence côté Java/Kotlin).

Le schéma serait :

1. `val doc = Jsoup.parseBodyFragment(htmlString)`
    
2. Tu récupères `doc.body()`
    
3. Tu parcours récursivement les nœuds, en transportant un **style courant** (bold, italique, couleur…)
    
4. À chaque texte brut rencontré (`TextNode`), tu crées un `RichSegment` avec le style courant.
    

### Exemple de logique (pseudo-Kotlin)

Je ne te colle pas 200 lignes, juste l’idée :

```kotlin
data class Style(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val color: String? = null
)

fun parseHtmlToRichText(html: String): RichText {
    val doc = Jsoup.parseBodyFragment(html)
    val segments = mutableListOf<RichSegment>()

    fun walk(node: Node, style: Style) {
        when (node) {
            is TextNode -> {
                val text = node.text()
                if (text.isNotBlank()) {
                    segments += RichSegment(
                        text = text,
                        bold = style.bold,
                        italic = style.italic,
                        underline = style.underline,
                        color = style.color
                    )
                }
            }
            is Element -> {
                val newStyle = style.enrichWith(node)
                node.childNodes().forEach { child ->
                    walk(child, newStyle)
                }
            }
        }
    }

    walk(doc.body(), Style())
    return RichText(segments)
}

// Exemple d’enrichissement de style
fun Style.enrichWith(element: Element): Style {
    var s = this
    when (element.tagName().lowercase()) {
        "b", "strong" -> s = s.copy(bold = true)
        "i", "em"     -> s = s.copy(italic = true)
        "u"           -> s = s.copy(underline = true)
        "span"        -> {
            val styleAttr = element.attr("style")
            // ici tu peux parser styleAttr pour trouver "color: #xxxxxx"
            // et mettre s = s.copy(color = "#xxxxxx")
        }
    }
    return s
}
```

Le point important :  
tu **propages** un `Style` au fur et à mesure de la descente dans l’arbre HTML.  
Cela gère naturellement les cas imbriqués du type :

```html
bonjour <b>monde <span style="color:#ff0000">cruel</span></b>
```

- `monde` : `bold = true`, `color = null`
    
- `cruel` : `bold = true`, `color = "#ff0000"`
    

## 3. Est-ce que c’est “simple” ?

Il y a deux niveaux :

1. **Avec HTML “pur” et jsoup**  
    → oui, c’est raisonnablement simple pour ton besoin **si tu te limites** à quelques balises.  
    Le parseur s’occupe de toutes les bizarreries (balises mal fermées, etc.).
    
2. **Sans lib, avec un parseur maison** basé sur un mini-langage  
    (ex. `[b]monde[/b]` au lieu de `<b>monde</b>`)  
    → c’est encore plus simple, mais tu perds l’avantage de la “standardisation” HTML.
    

À mon avis :

- Si ton viewer tourne en **JVM/Android** et que la taille de dépendance ne te dérange pas :  
    👉 **jsoup + sous-ensemble HTML** = très bon compromis.
    
- Si tu veux quelque chose de **très léger** ou multi-plateforme pur Kotlin (KMP) :  
    👉 inventer un **mini-markup** (ex. `**gras**`, `_italique_`, `[color=#ff0000]texte`) sera encore plus simple à parser avec quelques regex + un petit automaton.
    

## 4. Alternative : ne pas envoyer de HTML du tout

Juste pour que tu aies le tableau complet :  
tu pourrais aussi faire :

- côté émetteur : déjà construire un `RichText` structuré (liste de segments) ;
    
- côté transport : sérialiser en JSON (par ex. `kotlinx.serialization`);
    
- côté viewer : désérialiser directement en `RichText`.
    

Exemple de JSON :

```json
{
  "segments": [
    { "text": "bonjour ", "bold": false },
    { "text": "monde", "bold": true }
  ]
}
```

Avantage :

- plus besoin de parser HTML, c’est **typé** de bout en bout.
    

Inconvénient :

- moins lisible à l’œil nu qu’une ligne de log façon `"bonjour <b>monde</b>"`.
    

## 5. Conclusion (et réponse directe à tes questions)

> Est-ce que tu comprends l’idée ?

Oui, parfaitement : tu veux convertir une chaîne "markup" (HTML ou autre) en un objet `Texte` / `SubText` structurés, pour pouvoir afficher proprement dans ton viewer.

> Est-ce que pour le parsing, c’est possible de faire quelque chose de simple ?

👉 Oui :

- soit tu utilises **jsoup** et tu te limites à quelques balises HTML → parsing assez simple avec un parcours récursif + propagation de style,
    
- soit tu inventes un **mini-langage de balises** (type `[b]…[/b]`) et tu fais un parseur maison très minimaliste.
    

Si tu veux, dans un prochain message, je peux te proposer :

- soit un **exemple complet** (Kotlin + jsoup) qui part d’un `String html` et donne un `RichText`,
    
- soit un petit **mini-markup maison** adapté à tes logs, avec un parseur 100 % Kotlin sans librairie externe.