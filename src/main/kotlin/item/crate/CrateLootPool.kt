package item.crate

import item.crate.CrateItem.*

enum class CrateLootPool(val possibleItems: List<CrateItem>) {
    PLUSHIE(listOf(
        PENGUIN,
        MUSHROOM,
        BEE,
        STAR,
        HEART,
        RAMEN_BOWL,
        COFFEE_CUP
    )),
    WEARABLES(listOf(
        CAT_EARS,
        DOG_EARS,
        FOX_EARS,
        COOL_GLASSES,
        HALO,
        HEART_CROWN,
        ORCHID_CROWN,
        HEART_GLASSES
    )),
    PLAYER(listOf(
        SEBIANN,
        SEBIANN_CLASSIC,
        COOKIE,
        BEAUVER
    )),
    CHARACTER(listOf(
        N,
        ASTARION,
        BATMAN,
        DAZAI,
        LEVI,
        SPARROW,
        SPIDERMAN,
        LEONARDO,
        RAPHAEL,
        MICHELANGELO,
        DONATELLO
    ))
}