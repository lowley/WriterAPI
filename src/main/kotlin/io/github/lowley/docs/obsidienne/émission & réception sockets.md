Oui, **TCP est full-duplex** : on peut **lire** et **écrire** en même temps sans problème réseau.  
La règle d’or côté code est simple :

- **1 lecteur unique** sur `InputStream` (boucle de lecture).
    
- **1 écrivain unique** sur `OutputStream` (boucle d’écriture).
    
- Si plusieurs producteurs veulent envoyer → **une file unique** (ex. `Channel`) alimente **l’unique écrivain**.
    

Ainsi, pas d’écritures entremêlées ni de lectures concurrentes hasardeuses.

---

# Schéma recommandé (2 jobs + 1 file)

```kotlin
class ChatIo(
    private val socket: Socket,
    scope: CoroutineScope
) {
    private val outbox = Channel<String>(capacity = Channel.BUFFERED) // file des messages à envoyer

    private val readerJob = scope.launch(Dispatchers.IO) {
        socket.getInputStream().bufferedReader(Charsets.UTF_8).use { r ->
            while (isActive) {
                val line = r.readLine() ?: break            // EOF -> pair fermé
                onIncoming(line)                            // traite le message entrant (UI, VM, etc.)
            }
        }
    }

    private val writerJob = scope.launch(Dispatchers.IO) {
        socket.getOutputStream().bufferedWriter(Charsets.UTF_8).use { w ->
            for (msg in outbox) {                           // un seul écrivain
                w.write(msg)
                w.write("\n")                               // framing simple par ligne
                w.flush()                                   // flush immédiat (ou batch si tu préfères)
            }
        }
    }

    fun send(msg: String) {
        // Plusieurs producteurs peuvent appeler ceci en parallèle en toute sécurité
        outbox.trySend(msg).getOrElse { /* gérer plein/erreur si besoin */ }
    }

    suspend fun close() {
        // arrêt propre : on termine l'écrivain, puis on ferme la socket
        outbox.close()
        writerJob.cancelAndJoin()
        readerJob.cancelAndJoin()
        try { socket.shutdownOutput() } catch (_: Exception) {}
        try { socket.close() } catch (_: Exception) {}
    }

    private fun onIncoming(line: String) {
        // TODO: dispatcher vers VM/Flow/handlers
    }
}
```

### Pourquoi ça marche bien

- **Full-duplex** : le `readerJob` et le `writerJob` tournent **en parallèle**.
    
- **Sérialisation des envois** : tous les producteurs passent par `outbox` → **un seul écrivain** touche l’`OutputStream`.
    
- **Pas de double lecture** : **un seul lecteur** touche l’`InputStream`.
    

---

## Détails pratiques

- **Framing** : ici, _ligne par message_. Pour du binaire/JSON long → préfère **longueur-préfixée** (int32 + payload).
    
- **Contexte** : `Dispatchers.IO` évite de bloquer le Main.
    
- **Back-pressure** : ajuste la capacité de `Channel` (`BUFFERED`, `UNLIMITED`, ou suspension via `send()`).
    
- **Timeouts** : `socket.soTimeout = 30_000` pour éviter une lecture bloquée éternellement.
    
- **Robustesse** : entoure les boucles d’un `try/catch(IOException)` + stratégie de reconnexion si besoin.
    
- **Half-close** : `shutdownOutput()` signale proprement « j’ai fini d’écrire » (le pair lit EOF), puis `close()`.
    

---

### Réponse directe à ta crainte

> _“Et si j’émets exactement au même moment qu’un message arrive ?”_  
> Aucun souci : **le réseau gère** les deux directions indépendamment.  
> Le seul point à garantir, c’est **ta discipline côté code** : **un** lecteur et **un** écrivain (avec une file d’envoi pour éviter les écritures concurrentes).