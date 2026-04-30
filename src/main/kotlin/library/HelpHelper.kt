package library

object HelpHelper {
    private data class CommandHelp(
        val usage: List<String>,
        val description: List<String>,
        val shortDescription: String = description.firstOrNull() ?: "No short description available."
        )

    private val commands = mapOf(
        "help" to CommandHelp(
            listOf("/help <command>"),
            listOf("Get help for a specific command."),
            "Get help for a specific command."
        ),
        "report" to CommandHelp(
            listOf("/report <reason>"),
            listOf("Report anything to Staff like bugs, issues, feedback or players."),
            "Report anything to Staff."
        ),
        "afk" to CommandHelp(
            listOf("/afk"),
            listOf("Toggle your AFK status."),
            "Toggle AFK status."
        ),
        "nosleep" to CommandHelp(
            listOf("/nosleep"),
            listOf("Toggle the NoSleep tag on your name in the tab list. While enabled, other players cannot skip the night by sleeping."),
            "Toggle the NoSleep tab tag."
        ),
        "book" to CommandHelp(
            listOf("/book author <name>", "/book title <name>"),
            listOf("Set the author of the book you're holding in your main hand.", "Set the title of the book you're holding in your main hand."),
            "Set author or title of a book."
        ),
        "crates" to CommandHelp(
            listOf("/crates"),
            listOf("View the available crates.")
        ),
        "boosters" to CommandHelp(
            listOf("/boosters"),
            listOf("View the available booster packs and preview card odds.")
        ),
        "cratestats" to CommandHelp(
            listOf("/cratestats", "/cratestats me <crate>", "/cratestats global"),
            listOf("Show your overall crate roll stats.", "Show your collectible stats for a specific crate.", "Show global crate roll stats across all players."),
            "View crate roll statistics."
        ),
        "echo" to CommandHelp(
            listOf("/echo <message>"),
            listOf("Echo a message back to you.")
        ),
        "flex" to CommandHelp(
            listOf("/flex"),
            listOf("Flex the item in your hand to the whole server!")
        ),
        "hat" to CommandHelp(
            listOf("/hat"),
            listOf("Wear the item in your main hand as a hat.")
        ),
        "height" to CommandHelp(
            usage = listOf("/height <cm>", "/height reset"),
            description = listOf("Set your in-game scale based on your real-life height in cm.", "Reset your in-game height to the default."),
            "Change your in-game height."
        ),
        "home" to CommandHelp(
            listOf("/homes", "/sethome <name>", "/delhome <name>", "/home <name>"),
            listOf("List all of your homes.", "Sets a home with the given name (if already exists, overwrites it).", "Deletes the home.", "Teleport to the home with the given name."),
            "Manage your homes."
        ),
        "mail" to CommandHelp(
            listOf("/mail send <player> <message>", "/mail inbox [page]", "/mail read <id>", "/mail delete <id>", "/mail clearread"),
            listOf("Send mail to a player.", "Open your mailbox inbox.", "Read a mailbox entry by id.", "Delete a mailbox entry by id.", "Delete all read mailbox entries."),
            "Send and manage mailbox messages."
        ),
        "msg" to CommandHelp(
            listOf("/msg <player> <message>", "/reply <message>"),
            listOf("Send a private message to another player.", "Reply to the last person who messaged you."),
            "Message people privately."
        ),
        "renameitem" to CommandHelp(
            listOf("/renameitem <name>"),
            listOf("Rename the item in your main hand to the given name. Costs 1 lapis lazuli.")
        ),
        "resetitemname" to CommandHelp(
            listOf("/resetitemname"),
            listOf("Reset the name of the item in your main hand."),
            "Reset your held item's custom name."
        ),
        "spawn" to CommandHelp(
            listOf("/spawn"),
            listOf("Teleport to the server spawn.")
        ),
        "streamermode" to CommandHelp(
            listOf("/streamermode"),
            listOf("Toggle streamer mode. Adds a live indicator to your name and hides your coordinates from others."),
            "Toggle streamer mode."
        ),
        "stripcosmetic" to CommandHelp(
            listOf("/stripcosmetic"),
            listOf("Remove a cosmetic overlay from a helmet and return the cosmetic item to your inventory."),
            "Remove a cosmetic from a helmet."
        ),
        "tpa" to CommandHelp(
            listOf("/tpa <player>", "/tpahere <player>", "/tpaccept [player]", "/tpdeny [player]", "/tpacancel"),
            listOf("Send a TPA request to a player.", "Request another player to teleport to you.", "Accept a pending TPA request.", "Deny a pending TPA request.", "Cancel all TPA request you sent."),
            "TPA to players."
        ),
        "ping" to CommandHelp(
            listOf("/ping"),
            listOf("Show your current ping in milliseconds."),
            "Show your current ping."
        )
    )

    private val staffCommands = mapOf(
        "staffhelp" to CommandHelp(
            listOf("/staffhelp"),
            listOf("Get help for staff commands.")
        ),
        "ac" to CommandHelp(
            listOf("/ac <message>"),
            listOf("Send a message to admin chat."),
            "Send a message to admin chat."
        ),
        "dc" to CommandHelp(
            listOf("/dc <message>"),
            listOf("Send a message to developer chat."),
            "Send a message to developer chat."
        ),
        "announce" to CommandHelp(
            listOf("/announce <text>", "/announcesmall <text>"),
            listOf("Broadcast a server-wide announcement in chat.", "Broadcast a short announcement in the action bar."),
            "Broadcast a server-wide announcement."
        ),
        "announcerestart" to CommandHelp(
            listOf("/announcerestart <time>", "/announcerestart <time> funny"),
            listOf("Announce an upcoming restart.", "Announce an upcoming restart with the funny format."),
            "Announce an upcoming restart."
        ),
        "timer" to CommandHelp(
            listOf("/timer start <time> [countdown]"),
            listOf("Start a server timer."),
            "Start a server timer."
        ),
        "vanish" to CommandHelp(
            listOf("/vanish"),
            listOf("Toggle vanish mode."),
            "Toggle vanish mode."
        ),
        "pack" to CommandHelp(
            listOf("/pack status", "/pack refresh [player]", "/pack push [player]", "/pack pop [player]", "/pack export cardmodels", "/pack export cardmodels noplacers"),
            listOf("Show resource pack cache status.", "Refresh resource packs from configured URLs.", "Push active resource packs to players.", "Remove resource packs from players.", "Export card model definitions and placeholder textures.", "Export card model definitions without generating placeholders."),
            "Manage resource pack operations."
        ),
        "showstat" to CommandHelp(
            listOf("/showstat <stat> [--material <material>] [--entity <entity>] [--online]"),
            listOf("Broadcast a rotating statistic scoreboard to online players."),
            "Broadcast statistic scoreboards."
        ),
        "cloudie" to CommandHelp(
            listOf("/cloudie reload"),
            listOf("Reload the plugin configuration and systems."),
            "Reload the plugin."
        ),
        "height" to CommandHelp(
            listOf("/height <cm> <player>", "/height reset <player>"),
            listOf("Set another player's height.", "Reset another player's height."),
            "Manage other players' heights."
        ),
        "trash" to CommandHelp(
            listOf("/trash"),
            listOf("Open a trash inventory — items left inside are deleted on close."),
            "Open a trash inventory."
        ),
        "debug" to CommandHelp(
            listOf("/debug"),
            listOf("Please talk to Seb before using this."),
            "Debug utilities."
        )
    )

    val featuredCommands = commands.keys.toList()
    val featuredStaffCommands = staffCommands.keys.toList()

    fun getCommandShortHelp(command: String, isStaff: Boolean): String {
        val cmd = command.lowercase()
        val help = if (isStaff) {
            staffCommands[cmd] ?: return ""
        } else {
            commands[cmd] ?: return ""
        }
        return help.shortDescription
    }

    fun getCommandHelp(command: String, isStaff: Boolean): String {
        val cmd = command.lowercase()
        val help = if (isStaff) {
            staffCommands[cmd] ?: return "<red>No help available for that staff command."
        } else {
            commands[cmd] ?: return "<red>No help available for that command."
        }
        val title = "<yellow>--------- <white>Help: /$cmd</white> ---------<reset>\n"
        val pairs = help.usage.zip(help.description).mapIndexed { i, (usage, desc) ->
            "<gold>Usage: <white>$usage\n<gold>Description: <white>$desc${if (i < help.usage.size - 1) "\n\n" else ""}"
        }.joinToString("")

        return "$title$pairs"
    }
}