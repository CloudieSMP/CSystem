package chat

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags
import net.kyori.adventure.text.`object`.ObjectContents

object Formatting {
    /** Prefix enum for allowing MiniMessage usage of the <prefix:NAME> tag in messages. **/
    enum class Prefix(val prefixName: String, val value: String) {
        NO_PREFIX("", ""),
        DEV_PREFIX("dev", "\uE002"),
        ADMIN_PREFIX("admin", "\uE001"),
        LIVE_PREFIX("live", "\uE010"),
        NOSLEEP_PREFIX("nosleep", "\uE011"),
        WARNING_PREFIX("warning", "⚠"),
        SKULL_PREFIX("skull", "☠");

        companion object {
            fun ofName(str : String): Prefix {
                for(p in entries) {
                    if (p.prefixName == str) return p
                }
                return NO_PREFIX
            }
        }
    }

    private val Cloudie_Color = TagResolver.resolver("cloudiecolor", Tag.styling(TextColor.color(196, 88, 137)))
    private val NOTIFICATION_COLOR = TagResolver.resolver("notifcolor", Tag.styling(TextColor.color(219, 0, 96)))

    val allTags = MiniMessage.builder()
        .tags(
            TagResolver.builder()
                .resolver(StandardTags.defaults())
                .resolver(skullResolver())
                .resolver(Cloudie_Color)
                .resolver(NOTIFICATION_COLOR)
                .resolver(prefix())
                .build()
        )
        .build()

    val restrictedTags = MiniMessage.builder()
        .tags(
            TagResolver.builder()
                .resolver(StandardTags.color())
                .resolver(StandardTags.decorations())
                .resolver(StandardTags.reset())
                .resolver(StandardTags.rainbow())
                .resolver(StandardTags.pride())
                .resolver(StandardTags.gradient())
                .resolver(StandardTags.shadowColor())
                .resolver(skullResolver())
                .resolver(Cloudie_Color)
                .build()
        )
        .build()

    val restrictedNoSkullTags = MiniMessage.builder()
        .tags(
            TagResolver.builder()
                .resolver(StandardTags.color())
                .resolver(StandardTags.decorations())
                .resolver(StandardTags.reset())
                .resolver(StandardTags.rainbow())
                .resolver(StandardTags.pride())
                .resolver(StandardTags.gradient())
                .resolver(StandardTags.shadowColor())
                .resolver(Cloudie_Color)
                .build()
        )
        .build()

    /** Builds a prefix tag. **/
    private fun prefix() : TagResolver {
        return TagResolver.resolver("prefix") { args, _ ->
            val prefixName = args.popOr("Name not supplied.")
            Tag.inserting(
                Component.text(Prefix.ofName(prefixName.toString()).value)
            )
        }
    }

    /** Resolves any MiniMessage <skull:NAME> tags used in messages. **/
    fun skullResolver() : TagResolver {
        return TagResolver.resolver("skull") { args, _ ->
            val rawName = args.popOr("Name not supplied.")
            Tag.inserting(Component.`object`(ObjectContents.playerHead(rawName.toString())))
        }
    }
}