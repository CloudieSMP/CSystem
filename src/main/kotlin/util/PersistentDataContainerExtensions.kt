package util

import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType.BYTE

fun PersistentDataContainer.isDebug(): Boolean {
    return this.get(Keys.IS_DEBUG, BYTE)?.toInt() == 1
}

fun PersistentDataContainer.setIsDebug(isDebug: Boolean) {
    if (isDebug) {
        this.set(Keys.IS_DEBUG, BYTE, 1)
    } else {
        this.remove(Keys.IS_DEBUG)
    }
}

