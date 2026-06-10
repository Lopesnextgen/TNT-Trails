package davi.lopes.update

object VersionComparator {
    fun isNewer(candidate: String, current: String): Boolean {
        val candidateParts = parts(candidate)
        val currentParts = parts(current)
        val size = maxOf(candidateParts.size, currentParts.size)

        repeat(size) { index ->
            val candidatePart = candidateParts.getOrElse(index) { 0 }
            val currentPart = currentParts.getOrElse(index) { 0 }
            if (candidatePart != currentPart) {
                return candidatePart > currentPart
            }
        }

        return false
    }

    private fun parts(version: String): List<Int> {
        return version
            .trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore('-')
            .split('.')
            .map { part -> part.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
    }
}
