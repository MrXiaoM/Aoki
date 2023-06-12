package top.mrxiaom.mirai.aoki.util

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

class Version(
    val main: Int,
    val minor: Int,
    val patch: Int,
    val suffix: String
) {
    override operator fun equals(other: Any?): Boolean {
        return other is Version && equals(other, true)
    }
    fun equals(other: Version, checkSuffix: Boolean): Boolean {
        return this.main == other.main
                && this.minor == other.minor
                && this.patch == other.patch
                && (!checkSuffix || suffix == other.suffix)
    }
    operator fun compareTo(that: Version): Int {
        if (equals(that, false)) return 0
        if (this.main > that.main) return 1
        if (this.main < that.main) return -1
        if (this.minor > that.minor) return 1
        if (this.minor < that.minor) return -1
        if (this.patch > that.patch) return 1
        if (this.patch < that.patch) return -1
        return 0
    }

    override fun hashCode(): Int {
        var result = main
        result = 31 * result + minor
        result = 31 * result + patch
        result = 31 * result + suffix.hashCode()
        return result
    }

    val pre: Int
        get() {
            if (!suffix.startsWith("pre")) return -1
            return suffix.substring(3).toIntOrNull() ?: 0
        }
}

fun parseVersion(s: CharSequence): Version {
    val ver = s.splitFrom(".", true, 3)
    val main = ver.getOrNull(0)?.toIntOrNull() ?: 0
    val minor = ver.getOrNull(1)?.toIntOrNull() ?: 0
    val patch: Int
    val suffix: String
    val last = ver.getOrNull(2) ?: ""
    if (!last.contains('-')){
        patch = last.toIntOrNull() ?: 0
        suffix = ""
    }
    else {
        patch = last.substringBefore('-').toIntOrNull() ?: 0
        suffix = last.substringAfter('-')
    }
    return Version(main, minor, patch, suffix)
}
fun CharSequence.splitFrom(delimiter: String, ignoreCase: Boolean, limit: Int): List<String> {
    var currentOffset = 0
    var nextIndex = indexOf(delimiter, currentOffset, ignoreCase)
    if (nextIndex == -1 || limit == 1) {
        return listOf(this.toString())
    }

    val isLimited = limit > 0
    val result = ArrayList<String>(if (isLimited) limit.coerceAtMost(10) else 10)
    do {
        result.add(substring(currentOffset, nextIndex))
        currentOffset = nextIndex + delimiter.length
        // Do not search for next occurrence if we're reaching limit
        if (isLimited && result.size == limit - 1) break
        nextIndex = indexOf(delimiter, currentOffset, ignoreCase)
    } while (nextIndex != -1)

    result.add(substring(currentOffset, length))
    return result
}

val JsonElement.jsonObjectOrNull: JsonObject?
    get() = this as? JsonObject
val JsonElement.jsonArrayOrNull: JsonArray?
    get() = this as? JsonArray