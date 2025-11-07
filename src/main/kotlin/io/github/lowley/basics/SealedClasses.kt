package lorry.basics

import kotlin.reflect.KClass

//usage:  val exchangeModes = getAllOf<ExchangeMode>()

inline fun <reified T : Any> getAllOf(): List<T> = getAll(T::class)

fun <T : Any> getAll(base: KClass<T>): List<T> {
    fun collect(k: KClass<out T>): List<T> {
        // instance si c'est un object (data object inclus)
        val self = k.objectInstance?.let { listOf(it) } ?: emptyList()

        // descendre si c'est scellé (pour couvrir les sous-classes scellées)
        val children = if (k.isSealed) {
            k.sealedSubclasses.flatMap { collect(it) }
        } else emptyList()

        return self + children
    }
    return collect(base)
}