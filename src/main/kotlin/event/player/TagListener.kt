package event.player

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

        if (TagHelper.isCurrentlyTagged(tagger) && !TagHelper.isCurrentlyTagged(taggee) && TagHelper.isHoldingTag(tagger)) {
            TagHelper.startTagging(tagger, taggee)
        }
    }
}