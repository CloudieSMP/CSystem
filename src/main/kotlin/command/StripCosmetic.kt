package command

import chat.Formatting
import io.papermc.paper.datacomponent.DataComponentTypes
import item.crate.CrateItem
import org.bukkit.persistence.PersistentDataType.STRING
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.processing.CommandContainer
import io.papermc.paper.command.brigadier.CommandSourceStack
import util.requirePlayer
import util.Keys.CRATE_ITEM

/**
 * /stripcosmetic — Removes a plushie/wearable cosmetic that was applied to a helmet via the anvil.
 *
 * - Takes the item in the player's main hand.
 * - Restores the helmet's original ITEM_MODEL (or unsets it if there was none).
 * - Gives the cosmetic [CrateItem] back to the player (or drops it at their feet if inventory is full).
 * - No data is lost: all original helmet metadata (enchants, name, lore, etc.) is preserved.
 */
@Suppress("unused", "UnstableApiUsage")
@CommandContainer
class StripCosmetic {

    @Command("stripcosmetic")
    @CommandDescription("Removes the cosmetic overlay from a helmet and returns the cosmetic item.")
    @Permission("cloudie.cmd.stripcosmetic")
    fun run(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        val item = player.inventory.itemInMainHand

        if (item.isEmpty) {
            player.sendMessage(Formatting.allTags.deserialize("<red>Hold the cosmetified helmet in your main hand."))
            return
        }

        val meta = item.itemMeta
        val pdc = meta?.persistentDataContainer

        val cosmeticId = pdc?.get(CRATE_ITEM, STRING)
        if (cosmeticId == null) {
            player.sendMessage(Formatting.allTags.deserialize("<red>This item has no cosmetic applied to it."))
            return
        }

        val crateItem = CrateItem.fromStoredId(cosmeticId)
        if (crateItem == null) {
            player.sendMessage(Formatting.allTags.deserialize("<red>The cosmetic stored on this item is invalid. Please contact an admin."))
            return
        }

        // Reset ITEM_MODEL, EQUIPPABLE, and glint override back to vanilla defaults
        item.resetData(DataComponentTypes.ITEM_MODEL)
        item.resetData(DataComponentTypes.EQUIPPABLE)
        item.resetData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE)

        // Build the restored cosmetic first so we know exactly which keys to scrub from the helmet.
        // createItemStack() sets ITEM_MODEL + EQUIPPABLE correctly; copyTo brings back all original
        // cosmetic PDC data; then we remove the two bookkeeping keys that don't belong on the cosmetic.
        val cosmeticStack = crateItem.createItemStack()
        cosmeticStack.editMeta { m ->
            pdc.copyTo(m.persistentDataContainer, true)
        }

        // Remove every key copied from the cosmetic (including CRATE_ITEM)
        item.editMeta { m ->
            cosmeticStack.itemMeta?.persistentDataContainer?.keys?.forEach { key ->
                m.persistentDataContainer.remove(key)
            }
        }

        player.inventory.setItemInMainHand(item)

        val leftover = player.inventory.addItem(cosmeticStack)
        leftover.values.forEach { dropped ->
            player.world.dropItemNaturally(player.location, dropped)
        }

        player.sendMessage(Formatting.allTags.deserialize(
            "<cloudiecolor>Cosmetic removed! <white>The <cloudiecolor>${crateItem.displayNamePlain}<white> has been returned to your inventory."
        ))
    }
}
