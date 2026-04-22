package command

import chat.Formatting.allTags
import io.papermc.paper.command.brigadier.CommandSourceStack
import item.booster.BoosterPack
import item.booster.BoosterType
import item.booster.Cards
import item.booster.CardCatalog
import item.binder.BinderItem
import item.SubRarity
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.processing.CommandContainer
import item.crate.Crate
import item.crate.CrateItem
import item.crate.CrateType
import item.treasurebag.BagItem
import item.treasurebag.BagType
import item.treasurebag.TreasureBag
import org.bukkit.entity.EntityType
import org.incendo.cloud.annotations.Argument
import java.util.UUID
import util.requirePlayer
import util.setIsDebug

@Suppress("unused", "unstableApiUsage")
@CommandContainer
class Debug {
    @Command("debug crate <type> [isDebug]")
    @Permission("cloudie.cmd.debug")
    fun debugCrate(css: CommandSourceStack, @Argument("type") type: CrateType, @Argument("isDebug") isDebug: Boolean = true) {
        val player = css.requirePlayer() ?: return

        if (isDebug) {
            player.inventory.addItem(Crate.create(type, isDebug = true))
        } else {
            player.inventory.addItem(Crate.create(type))
        }
        player.sendMessage(allTags.deserialize("<cloudiecolor>Given a crate!"))
    }

    @Command("debug crate item <type>")
    @Permission("cloudie.cmd.debug")
    fun debugCrateItem(css: CommandSourceStack, @Argument("type") crateItem: CrateItem) {
        val player = css.requirePlayer() ?: return

        val item = crateItem.createItemStack().apply {
            editMeta { meta ->
                meta.persistentDataContainer.setIsDebug(true)
            }
        }
        player.inventory.addItem(item)
        player.sendMessage(allTags.deserialize("<cloudiecolor>Given a crate item!"))
    }

    @Command("debug booster <type>")
    @Permission("cloudie.cmd.debug")
    fun debugBooster(css: CommandSourceStack, @Argument("type") type: BoosterType) {
        val player = css.requirePlayer() ?: return

        player.inventory.addItem(BoosterPack.create(type))
        player.sendMessage(allTags.deserialize("<cloudiecolor>Given a Booster Pack!"))
    }

    @Command("debug pull <type>")
    @Permission("cloudie.cmd.debug")
    fun debugPull(css: CommandSourceStack, @Argument("type") type: BoosterType) {
        val player = css.requirePlayer() ?: return

        val pull = Cards.openBooster(player, type, useDebugSubRarityOverride = true)
        if (pull == null) {
            player.sendMessage(allTags.deserialize("<red>No eligible cards were found for this booster."))
        }
    }

    @Command("debug subrarity set <noneWeight> <shinyWeight> <shadowWeight> <obfuscatedWeight>")
    @Permission("cloudie.cmd.debug")
    fun debugSubRaritySet(
        css: CommandSourceStack,
        @Argument("noneWeight") noneWeight: Double,
        @Argument("shinyWeight") shinyWeight: Double,
        @Argument("shadowWeight") shadowWeight: Double,
        @Argument("obfuscatedWeight") obfuscatedWeight: Double,
    ) {
        val player = css.requirePlayer() ?: return
        if (noneWeight < 0 || shinyWeight < 0 || shadowWeight < 0 || obfuscatedWeight < 0) {
            player.sendMessage(allTags.deserialize("<red>Weights must be 0 or higher."))
            return
        }

        val applied = SubRarity.setDebugWeights(noneWeight, shinyWeight, shadowWeight, obfuscatedWeight)
        if (!applied) {
            player.sendMessage(allTags.deserialize("<red>At least one weight must be greater than 0."))
            return
        }

        player.sendMessage(
            allTags.deserialize(
                "<cloudiecolor>Debug subrarity weights set: <white>NONE=$noneWeight<gray>, <white>SHINY=$shinyWeight<gray>, <white>SHADOW=$shadowWeight<gray>, <white>OBFUSCATED=$obfuscatedWeight"
            )
        )
    }

    @Command("debug subrarity show")
    @Permission("cloudie.cmd.debug")
    fun debugSubRarityShow(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        val weights = SubRarity.debugWeights()
        if (weights == null) {
            player.sendMessage(allTags.deserialize("<gray>No debug subrarity override is active. Using normal rates."))
            return
        }

        player.sendMessage(
            allTags.deserialize(
                "<cloudiecolor>Active debug weights: <white>NONE=${weights[SubRarity.NONE]}<gray>, <white>SHINY=${weights[SubRarity.SHINY]}<gray>, <white>SHADOW=${weights[SubRarity.SHADOW]}<gray>, <white>OBFUSCATED=${weights[SubRarity.OBFUSCATED]}"
            )
        )
    }

    @Command("debug subrarity clear")
    @Permission("cloudie.cmd.debug")
    fun debugSubRarityClear(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        SubRarity.clearDebugWeights()
        player.sendMessage(allTags.deserialize("<cloudiecolor>Cleared debug subrarity weights. Normal rates restored."))
    }

    @Command("debug binder")
    @Permission("cloudie.cmd.debug")
    fun debugBinder(css: CommandSourceStack) {
        val player = css.requirePlayer() ?: return
        player.inventory.addItem(BinderItem.create())
        player.sendMessage(allTags.deserialize("<cloudiecolor>Given a Card Binder!"))
    }

    @Command("debug card <booster> <mob>")
    @Permission("cloudie.cmd.debug")
    fun debugCard(
        css: CommandSourceStack,
        @Argument("booster") boosterType: BoosterType,
        @Argument("mob") mobType: EntityType,
    ) {
        val player = css.requirePlayer() ?: return
        val card = CardCatalog.findByEntityType(mobType)
        if (card == null) {
            player.sendMessage(allTags.deserialize("<red>No card is configured for <white>${mobType.name}<red>."))
            return
        }

        val validationError = Cards.validationErrorFor(boosterType, card)
        if (validationError != null) {
            player.sendMessage(allTags.deserialize("<red>$validationError"))
            return
        }

        val result = Cards.openBoosterForced(player, boosterType, card, useDebugSubRarityOverride = true)
        if (result == null) {
            player.sendMessage(allTags.deserialize("<red>Could not open forced debug pull."))
            return
        }

        player.sendMessage(
            allTags.deserialize("<gray>Debug pull -> <white>${result.definition.id}<gray>, rarity: <white>${result.rarity.name}<gray>, sub: <white>${result.subRarity.name}<gray>, global: <white>${result.globalPullCount}")
        )
    }

    @Command("debug treasure_bag <type>")
    @Permission("cloudie.cmd.debug")
    fun debugTreasureBag(css: CommandSourceStack, @Argument("type") type: BagType) {
        val player = css.requirePlayer() ?: return
        player.inventory.addItem(TreasureBag.create(type, useDebugSubRarityOverride = true))
    }
    @Command("debug treasure_bag item <loot>")
    @Permission("cloudie.cmd.debug")
    fun debugTreasureBagItem(css: CommandSourceStack, @Argument("loot") loot: BagItem) {
        val player = css.requirePlayer() ?: return
        player.inventory.addItem(loot.createItemStack(useDebugSubRarityOverride = true))
    }

    @Command("debug uuid <x> <y> <z> <a>")
    @Permission("cloudie.cmd.debug")
    fun debugUuid(
        css: CommandSourceStack,
        @Argument("x") x: Int,
        @Argument("y") y: Int,
        @Argument("z") z: Int,
        @Argument("a") a: Int,
    ) {
        val mostSignificantBits = ((x.toLong() and 0xffffffffL) shl 32) or (y.toLong() and 0xffffffffL)
        val leastSignificantBits = ((z.toLong() and 0xffffffffL) shl 32) or (a.toLong() and 0xffffffffL)
        val uuidText = UUID(mostSignificantBits, leastSignificantBits).toString()

        css.sender.sendMessage(
            allTags.deserialize(
                "<click:copy_to_clipboard:'$uuidText'><hover:show_text:'<gray>Click to copy UUID</gray>'><cloudiecolor>$uuidText</cloudiecolor></hover></click>"
            )
        )
    }
}