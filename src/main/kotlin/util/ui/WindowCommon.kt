package util.ui

import com.noxcrew.interfaces.drawable.Drawable.Companion.drawable
import com.noxcrew.interfaces.element.StaticElement
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * Creates the shared gray stained-glass-pane border element used across all GUI windows.
 * A new instance is returned each call so the library never shares one element across multiple slots.
 */
@Suppress("unused")  // called from CollectionBrowserWindow and StorageWindow
internal fun glassBorderElement(): StaticElement =
    StaticElement(drawable(ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
        itemMeta = itemMeta.apply { isHideTooltip = true }
    }))