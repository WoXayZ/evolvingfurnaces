# Evolving Furnaces

Upgrade vanilla furnaces, blast furnaces, smokers and brewing stands through data-driven tiers.
Minecraft **1.21.1** / **NeoForge 21.1.248**. Version **1.1**.

Vanilla machines stay vanilla. You upgrade one in place into a Copper machine (level 1), then keep going. Each tier can change processing time, batch size, the visual style, and the upgrade cost.

The GUI is in English and French. Machine names come from the JSON (`display_name`), so a datapack can name tiers in any language without a resource pack.

---

## How it works

1. Place a furnace, blast furnace, smoker or brewing stand.
2. Open it. If you have the items and XP for the next tier, the upgrade button lights up.
3. Click it. The block is replaced in place, inventory included.

From level 1 you also get:

- an **Info** panel: current stats, progression toward the next tier, next cost
- a side **IO** panel for hoppers and pipes (no auto-eject)

Default IO:

- Top: input
- Bottom: output
- Sides: fuel (Blaze Powder on the brewing stand)

The default pack has 10 tiers, copper through netherite. A datapack can ship 3, 12 or 40. There is no hardcoded cap: the last tier is whichever one has nothing after it.

---

## Configuration

`config/evolvingfurnaces-common.toml`. One section per machine, independently:

```toml
[furnace]
enabled = true

[blast_furnace]
enabled = true

[smoker]
enabled = true

[brewing_stand]
enabled = true
```

Disabling a machine:

- hides it from creative and JEI / EMI / REI
- removes the upgrade button on the vanilla GUI
- ignores that machine's datapack progression
- converts already placed Evolving blocks back to vanilla on load (contents kept; tier, progression and IO are lost)
- never changes the vanilla block of the same type

A datapack cannot re-enable a machine turned off here. Changing these flags needs a game restart (common config).

---

## Default furnace family

Same cook counts for furnace, blast furnace and smoker. Names and files differ. Blast furnace and smoker cook at half the furnace time.

| Level | Name | Cook time | Batch |
| --- | --- | --- | --- |
| 1 | Copper | 200 ticks (10s) | 1 |
| 2 | Amethyst | 190 | 1 |
| 3 | Iron | 180 | 1 |
| 4 | Lapis Lazuli | 170 | 2 |
| 5 | Redstone | 160 | 2 |
| 6 | Gold | 150 | 2 |
| 7 | Diamond | 140 | 4 |
| 8 | Emerald | 130 | 4 |
| 9 | Obsidian | 120 | 4 |
| 10 | Netherite | 110 | 8 |

Costs, cook counts and display names live in the JSON, not in code.

---

## Brewing Stand

The Evolving Brewing Stand keeps one Blaze Powder charge per cycle (`fuel` 20 uses, same as vanilla). Ingredient cost is the **largest brewable stack** among the three potion slots (8-7-4 consumes 8, 3-2-0 consumes 3). Fuel is not multiplied by batch size.

`batch_size` is **per potion slot**, not for the whole stand. Three slots × 8 = 24 potions in one operation at Netherite.

Internal potion and ingredient slots may hold more than the vanilla item max (turtle helmets stack to `batch_size` in the ingredient slot only). Hoppers, player inventory and stack limits stay vanilla: a max-stack-1 item comes out one at a time.

Progression counts **potions actually produced**. Brewing 24 potions adds 24, not 1.

The base plate uses `brewing_stand_base` fused with the tier material. The stand rods and bottles stay vanilla. Animated style blocks keep their extra frames.

| Level | Name | Brew time | Potions / slot | `required_brews` |
| --- | --- | ---: | ---: | ---: |
| 1 | Copper | 400 (20s) | 1 | 0 |
| 2 | Amethyst | 380 | 1 | 30 |
| 3 | Iron | 360 | 1 | 120 |
| 4 | Lapis Lazuli | 340 | 2 | 480 |
| 5 | Redstone | 320 | 2 | 1920 |
| 6 | Gold | 300 | 2 | 7680 |
| 7 | Diamond | 280 | 4 | 30720 |
| 8 | Emerald | 260 | 4 | 122880 |
| 9 | Obsidian | 240 | 4 | 491520 |
| 10 | Netherite | 220 | 8 | 1966080 |

XP costs match the furnace family (2 / 5 / 8 / 10 / 12 / 15 / 18 / 20 / 25 / 30). Upgrade items are 8 blocks of the target tier style.

IO for brewing:

- **Input**: ingredient and potion bottles (up to `batch_size` per slot)
- **Output**: extract potions
- **Fuel**: Blaze Powder only
- **Disabled**: nothing

---

## Datapacks

One file per machine, overlay path:

- `data/evolvingfurnaces/evolving_furnaces/furnace.json`
- `data/evolvingfurnaces/evolving_furnaces/blast_furnace.json`
- `data/evolvingfurnaces/evolving_furnaces/smoker.json`
- `data/evolvingfurnaces/evolving_furnaces/brewing_stand.json`

A datapack **replaces the whole list** for that machine. It does not merge tier by tier.

`/reload` picks the new lists up. Clients get them from the server. After a style or name change, `F3+T` refreshes textures.

If a reload adds, removes or changes tiers, existing machines keep their current level. Missing levels fall back to the first remaining tier. They are not corrupted.

Shipped defaults:

- [`furnace.json`](src/main/resources/data/evolvingfurnaces/evolving_furnaces/furnace.json)
- [`blast_furnace.json`](src/main/resources/data/evolvingfurnaces/evolving_furnaces/blast_furnace.json)
- [`smoker.json`](src/main/resources/data/evolvingfurnaces/evolving_furnaces/smoker.json)
- [`brewing_stand.json`](src/main/resources/data/evolvingfurnaces/evolving_furnaces/brewing_stand.json)

Furnace-family shape:

```json
{
  "levels": [
    {
      "level": 1,
      "name": "copper",
      "display_name": {
        "en_us": "Copper Furnace",
        "fr_fr": "Fourneau en cuivre"
      },
      "style": {
        "block": "minecraft:copper_block",
        "texture": "side"
      },
      "required_cooks": 0,
      "upgrade_cost": {
        "items": [
          { "count": 8, "item": "minecraft:copper_block" }
        ],
        "experience_levels": 2
      },
      "cook_time": 200,
      "batch_size": 1
    }
  ]
}
```

Brewing stand uses `required_brews` and `brew_time` instead of `required_cooks` and `cook_time`. Example:

```json
{
  "level": 1,
  "name": "copper",
  "display_name": {
    "en_us": "Copper Brewing Stand",
    "fr_fr": "Stand d'alchimie en cuivre"
  },
  "style": {
    "block": "minecraft:copper_block",
    "texture": "side"
  },
  "required_brews": 0,
  "upgrade_cost": {
    "items": [
      { "count": 8, "item": "minecraft:copper_block" }
    ],
    "experience_levels": 2
  },
  "brew_time": 400,
  "batch_size": 1
}
```

| Field | Notes |
| --- | --- |
| `level` | Integer `>= 1`. Order follows this field, not the array order. |
| `name` | Optional id for logs. |
| `display_name` | Map of locale to name. `en_us` is required. Extra locales are fine. |
| `style.block` | Block whose texture is used (furnace border, or fused brewing stand base). |
| `style.texture` | `top`, `bottom`, `north`, `south`, `east`, `west`, or `side` (default). |
| `required_cooks` / `required_brews` | Progression needed on the previous tier before this one unlocks. Level 1 uses 0. |
| `upgrade_cost.items` | Each entry is either `"item"` or `"tag"`, plus `"count"`. |
| `upgrade_cost.experience_levels` | Optional, default 0. |
| `cook_time` / `brew_time` | Ticks per cycle, `>= 1`. Vanilla furnace 200, vanilla brewing 400. |
| `batch_size` | Items or potions per slot per cycle, `>= 1`. |

Missing file, empty list, or a parse error: that machine falls back to the shipped defaults and a warning is logged. The game does not crash.

---

## Recipe viewers and Jade

JEI, EMI and REI are optional. Each **enabled** machine type has a category you can step through with the arrows (Shift jumps 10). In JEI, click an upgrade item to see its recipes. Disabled machines are omitted.

Jade shows the current tier name.

---

## Requirements

| Dependency | Version |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.248+ |
| Java | 21 |

Optional: JEI, EMI, REI, Jade.

---

## Building

```bash
./gradlew jar
```

On Windows: `gradlew.bat jar`.

The jar lands in `build/libs/evolvingfurnaces-neoforge-1.21.1-<version>.jar`.

---

## Links

- CurseForge: https://www.curseforge.com/minecraft/mc-mods/evolvingfurnaces
- Issues: https://github.com/WoXayZ/evolvingfurnaces/issues

## License

MIT.
