import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.net.URI

@ConfigSerializable
data class Config(
    val links: List<Link>,
    val resourcePacks: List<ResourcePack>,
    val motd: String = "Bro forgot to set the motd, laugh at this user",
    val tpa: TpaConfig = TpaConfig(),
    val home: HomeConfig = HomeConfig(),
    val discord: DiscordConfig = DiscordConfig(),
    val afk: AfkConfig = AfkConfig()
)

@ConfigSerializable
data class TpaConfig(
    val requestExpireTime: Int = 30,
    val tpaDelay: Int = 2
)

@ConfigSerializable
data class HomeConfig(
    val maxHomes: Int = 5
)

@ConfigSerializable
data class DiscordConfig(
    val reportWebhookUrl: String = ""
)

@ConfigSerializable
data class AfkConfig(
    val idleTimeoutSeconds: Long = 300
)

@ConfigSerializable
data class Link(val component: String, val uri: URI, val order: Int)

@ConfigSerializable
data class ResourcePack(val uri: URI, val priority: Int)
