package event.player

import chat.Formatting.allTags
import library.AfkHelper
import library.TagHelper
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent

class TagListener : Listener {

    @EventHandler
    fun onTag(event: EntityDamageByEntityEvent) {
        val tagger = event.damager as? Player ?: return
        val taggee = event.entity as? Player ?: return

        if (!TagHelper.isCurrentlyTagged(tagger) || !TagHelper.isHoldingTag(tagger)) return

        // Cancel the hit entirely so knockback can't clear the taggee's AFK state
        if (AfkHelper.isAfk(taggee)) {
            event.isCancelled = true
            tagger.sendMessage(allTags.deserialize("<red>${taggee.name} is AFK and cannot be tagged!"))
            return
        }

        if (!TagHelper.isCurrentlyTagged(taggee)) {
            TagHelper.startTagging(tagger, taggee)
        }
    }
}