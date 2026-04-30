import chat.Formatting.allTags
import chat.VisualChat
import com.noxcrew.interfaces.InterfacesListeners
import event.ServerListEvent
import event.block.AnvilListener
import event.block.CauldronListener
import event.block.RainCropGrowthListener
import event.player.*
import io.papermc.paper.command.brigadier.CommandSourceStack
import command.ShowStat
import event.block.StonecutterDamageListener
import library.LiveHelper
import item.crate.CrateRecipes
import item.plushiebox.PlushieBox
import library.CardPullCounterStorage
import library.CrateRollStatsStorage
import library.HomeStorage
import library.MailStorage
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.paper.PaperCommandManager
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import util.ui.GamblingWindow
import util.ui.TrashWindow
import library.AfkHelper
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.description.CommandDescription
import org.incendo.cloud.processors.cache.SimpleCache
import org.incendo.cloud.processors.confirmation.ConfirmationContext
import org.incendo.cloud.processors.confirmation.ConfirmationConfiguration
import org.incendo.cloud.processors.confirmation.ConfirmationManager
import org.incendo.cloud.processors.confirmation.annotation.ConfirmationBuilderModifier
import java.io.File
import java.time.Duration
import util.ResourcePacker

@Suppress( "unstableApiUsage")
class CSystem : JavaPlugin() {
    private lateinit var commandManager: PaperCommandManager<CommandSourceStack>
    private lateinit var annotationParser: AnnotationParser<CommandSourceStack>
    lateinit var config: Config
        private set

    override fun onEnable() {
        this.logger.info("Starting the Cloudie System plugin!")
        InterfacesListeners.install(this)
        reloadConfig()
        CardPullCounterStorage.loadSync()
        CrateRollStatsStorage.loadSync()
        ShowStat.loadSync()
        registerRecipes()
        if (ResourcePacker.refreshFromUrl()) {
            logger.info("Resource pack cache populated on startup.")
        } else {
            logger.warning("Resource pack cache could not be populated on startup. Use /pack refresh after fixing the resource pack URLs.")
        }
        setupEvents()
        registerCommands()
        AfkHelper.startIdleChecker()
        VisualChat.clearChatEntities()
    }

    override fun onDisable() {
        this.logger.info("Stopping the Cloudie System plugin!")
        LiveHelper.shutdown()
        HomeStorage.flushAllSync()
        MailStorage.flushAllSync()
        CardPullCounterStorage.flushAllSync()
        CrateRollStatsStorage.flushAllSync()
        ShowStat.saveSync()
        AfkHelper.resetAll()
        VisualChat.clearChatEntities()
    }

    private fun registerCommands() {
        commandManager = PaperCommandManager.builder()
            .executionCoordinator(ExecutionCoordinator.simpleCoordinator())
            .buildOnEnable(this)

        annotationParser = AnnotationParser(commandManager, CommandSourceStack::class.java)
        setupCommandConfirmation()
        annotationParser.parseContainers()
    }

    private fun setupCommandConfirmation() {
        logger.info("Setting up command confirmation.")
        ConfirmationBuilderModifier.install(annotationParser)

        val confirmationCache = SimpleCache
            .of<String, ConfirmationContext<CommandSourceStack>>()
            .keyExtractingView<CommandSourceStack> { css -> confirmationKey(css.sender) }

        val confirmationConfig = ConfirmationConfiguration.builder<CommandSourceStack>()
            .cache(confirmationCache)
            .noPendingCommandNotifier { css ->
                css.sender.sendMessage(
                    Component.text(
                        "You do not have any pending commands.",
                        NamedTextColor.RED
                    )
                ) }
            .confirmationRequiredNotifier { css, ctx ->
                val commandText = ctx.commandContext().rawInput().input().trim().ifEmpty { "<unknown>" }
                css.sender.sendMessage(
                    Component.text("Confirm command ", NamedTextColor.RED).append(
                        Component.text("'/$commandText' ", NamedTextColor.GREEN)
                    ).append(Component.text("by running ", NamedTextColor.RED)).append(
                        Component.text("'/confirm' ", NamedTextColor.YELLOW)
                    ).append(Component.text("to execute.", NamedTextColor.RED))
                ) }
            .expiration(Duration.ofSeconds(30))
            .build()

        val confirmationManager = ConfirmationManager.confirmationManager(confirmationConfig)
        commandManager.registerCommandPostProcessor(confirmationManager.createPostprocessor())

        commandManager.command(
            commandManager.commandBuilder("confirm")
                .handler(confirmationManager.createExecutionHandler())
                .commandDescription(CommandDescription.commandDescription("Confirm a pending command."))
                .build()
        )
    }

    private fun confirmationKey(sender: CommandSender): String {
        return if (sender is Player) {
            "player:${sender.uniqueId}"
        } else {
            "sender:${sender.name.lowercase()}"
        }
    }

    private fun registerRecipes() {
        CrateRecipes.registerAll()
        PlushieBox.registerRecipe()
        server.onlinePlayers.forEach(CrateRecipes::discoverAll)
    }

    private fun setupEvents() {
        server.pluginManager.registerEvents(ServerListEvent(), this)
        server.pluginManager.registerEvents(PlayerJoin(), this)
        server.pluginManager.registerEvents(PlayerQuit(), this)
        server.pluginManager.registerEvents(InventoryOpenRefresh(), this)
        server.pluginManager.registerEvents(ChatEvent(), this)
        server.pluginManager.registerEvents(PlayerInteractEntity(), this)
        server.pluginManager.registerEvents(PlayerItemConsume(), this)
        server.pluginManager.registerEvents(CauldronListener(), this)
        server.pluginManager.registerEvents(PlayerInteract(), this)
        server.pluginManager.registerEvents(RainCropGrowthListener(), this)
        server.pluginManager.registerEvents(AnvilListener(), this)
        server.pluginManager.registerEvents(GamblingWindow, this)
        server.pluginManager.registerEvents(TrashWindow, this)
        server.pluginManager.registerEvents(BinderInteract(), this)
        server.pluginManager.registerEvents(PlushieBoxInteract(), this)
        server.pluginManager.registerEvents(CommandVisibilityListener(), this)
        server.pluginManager.registerEvents(AfkListener(), this)
        server.pluginManager.registerEvents(StonecutterDamageListener(), this)
    }

    private fun applyConfig(config: Config) {

        val serverLinks = Bukkit.getServerLinks()
        serverLinks.links.toList().forEach(serverLinks::removeLink)
        config.links.sortedBy { it.order }.forEach {
            serverLinks.addLink(allTags.deserialize(it.component), it.uri)
        }
        server.motd(allTags.deserialize(config.motd))
    }

    override fun reloadConfig() {
        if (!dataFolder.exists()) dataFolder.mkdir()
        val configFile = File(dataFolder, "config.yml")

        if (!configFile.exists()) {
            getResource("config.yml").use { inputStream ->
                configFile.outputStream().use { outputStream ->
                    inputStream!!.copyTo(outputStream)
                }
            }
        }

        val loader = YamlConfigurationLoader.builder()
            .file(configFile)
            .defaultOptions { options ->
                options.serializers { builder ->
                    builder.registerAnnotatedObjects(objectMapperFactory())
                }
            }
            .build()

        val node = loader.load()
        config = node.get(Config::class)!!
        applyConfig(config)
        logger.info("Loaded configuration.")
    }
}

val plugin: CSystem get() = JavaPlugin.getPlugin(CSystem::class.java)
val logger get() = plugin.logger