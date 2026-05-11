package item.cosmetic

/**
 * Enum of available player tail cosmetics.
 *
 * Each entry corresponds to a model file at BetterModel/models/tails/<modelId>.bbmodel
 * The root bone of each model should be tagged "phip" in BlockBench so BetterModel
 * parents it to the player's hip bone with correct body-rotation sync.
 */
enum class PlayerTail(
    /** The BetterModel model ID — must match the .bbmodel filename without extension */
    val modelId: String,
    /** Friendly display name shown in messages */
    val displayName: String,
    /** Permission node suffix, used as cloudie.cmd.tail.<permissionKey> */
    val permissionKey: String,
) {
    DRAGON(
        modelId = "tails/dragon_tail",
        displayName = "Dragon Tail",
        permissionKey = "dragon",
    );

    /** Full permission node required to equip this tail */
    val permission: String get() = "cloudie.cmd.tail.$permissionKey"

    companion object {
        private val byPermissionKey = entries.associateBy(PlayerTail::permissionKey)
        fun fromPermissionKey(key: String): PlayerTail? = byPermissionKey[key]
    }
}
