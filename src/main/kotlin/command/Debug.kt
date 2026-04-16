package command

import chat.Formatting.allTags
import io.papermc.paper.command.brigadier.CommandSourceStack
import item.booster.BoosterPack
import item.booster.BoosterType
import item.booster.Cards
import item.booster.CardCatalog
import item.binder.BinderItem
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.processing.CommandContainer
import item.crate.Crate
import item.crate.CrateItem
import item.crate.CrateType
import item.treasurebag.BagType
import item.treasurebag.TreasureBag
import org.bukkit.entity.EntityType
import org.incendo.cloud.annotations.Argument
import util.requirePlayer

@Suppress("unused", "unstableApiUsage")
@CommandContainer
class Debug {
    @Command("debug crate <type>")
    @Permission("cloudie.cmd.debug")
    fun debugCrate(css: CommandSourceStack, @Argument("type") type: CrateType) {
        val player = css.requirePlayer() ?: return

        player.inventory.addItem(Crate.create(type))
        player.sendMessage(allTags.deserialize("<cloudiecolor>Given a crate!"))
    }

    @Command("debug crate item <type>")
    @Permission("cloudie.cmd.debug")
    fun debugCrateItem(css: CommandSourceStack, @Argument("type") crateItem: CrateItem) {
        val player = css.requirePlayer() ?: return

        player.inventory.addItem(crateItem.createItemStack())
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

        val pull = Cards.openBooster(player, type)
        if (pull == null) {
            player.sendMessage(allTags.deserialize("<red>No eligible cards were found for this booster."))
        }
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

        val result = Cards.openBoosterForced(player, boosterType, card)
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
        player.inventory.addItem(TreasureBag.create(type))
    }
}