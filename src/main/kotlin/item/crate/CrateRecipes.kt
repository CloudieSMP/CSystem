package item.crate

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ShapedRecipe
import plugin

object CrateRecipes {
    fun registerAll() {
        CrateType.entries.forEach(::register)
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

