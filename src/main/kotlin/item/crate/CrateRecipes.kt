package item.crate

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ShapedRecipe
import plugin

object CrateRecipes {
    private val allRecipeKeys: List<NamespacedKey>
        get() = CrateType.entries.map { NamespacedKey(plugin, it.recipeKey) }

    fun registerAll() {
        CrateType.entries.forEach(::register)
    }

    fun discoverAll(player: Player) {
        player.discoverRecipes(allRecipeKeys)
    }

    private fun register(type: CrateType) {
        val key = NamespacedKey(plugin, type.recipeKey)
        Bukkit.removeRecipe(key)

        val recipe = ShapedRecipe(key, Crate.create(type)).apply {
            shape(
                "PPP",
                "PCP",
                "PPP",
            )
            setIngredient('P', org.bukkit.Material.PAPER)
            setIngredient('C', type.recipeIngredient)
        }

        Bukkit.addRecipe(recipe)
    }
}

