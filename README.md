# Evolving Furnaces

Upgrade vanilla furnaces, blast furnaces and smokers through data-driven tiers.
Minecraft **1.21.1** / **NeoForge 21.1.248**.

Vanilla machines stay vanilla. You upgrade one in place into a Copper Furnace (level 1), then keep going. Each tier can change cook time, batch size, the border style, and the upgrade cost.

The GUI is in English and French. Machine names come from the JSON (`display_name`), so a datapack can name tiers in any language without a resource pack.

---

## How it works

1. Place a furnace, blast furnace or smoker.
2. Open it. If you have the items and XP for the next tier, the upgrade button lights up.
3. Click it. The block is replaced in place, inventory included.

From level 1 you also get:

- an **Info** panel: current stats, cooks toward the next tier, next cost
- a side **IO** panel for hoppers and pipes (no auto-eject)

Default IO:

- Top: input
- Bottom: output
- Sides: fuel

The default pack has 10 tiers, copper through netherite. A datapack can ship 3, 12 or 40. There is no hardcoded cap: the last tier is whichever one has nothing after it.

---

## Default tiers

Same numbers for furnace, blast furnace and smoker. Names and files differ.

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

## Datapacks

One file per machine, overlay path:

- `data/evolvingfurnaces/evolving_furnaces/furnace.json`
- `data/evolvingfurnaces/evolving_furnaces/blast_furnace.json`
- `data/evolvingfurnaces/evolving_furnaces/smoker.json`

A datapack **replaces the whole list** for that machine. It does not merge tier by tier.

`/reload` picks the new lists up. Clients get them from the server. After a style or name change, `F3+T` refreshes textures.

Shipped defaults:

- [`furnace.json`](src/main/resources/data/evolvingfurnaces/evolving_furnaces/furnace.json)
- [`blast_furnace.json`](src/main/resources/data/evolvingfurnaces/evolving_furnaces/blast_furnace.json)
- [`smoker.json`](src/main/resources/data/evolvingfurnaces/evolving_furnaces/smoker.json)

Minimal shape:

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

| Field | Notes |
| --- | --- |
| `level` | Integer `>= 1`. Order follows this field, not the array order. |
| `name` | Optional id for logs. |
| `display_name` | Map of locale to name. `en_us` is required. Extra locales are fine. |
| `style.block` | Block whose texture is used on the border. |
| `style.texture` | `top`, `bottom`, `north`, `south`, `east`, `west`, or `side` (default). |
| `required_cooks` | Cooks needed on the previous tier before this one unlocks. Level 1 uses 0. |
| `upgrade_cost.items` | Each entry is either `"item"` or `"tag"`, plus `"count"`. |
| `upgrade_cost.experience_levels` | Optional, default 0. |
| `cook_time` | Ticks per cook cycle, `>= 1`. Vanilla furnace is 200. |
| `batch_size` | Items finished per cycle, `>= 1`. |

Missing file, empty list, or a parse error: that machine falls back to the shipped defaults and a warning is logged. The game does not crash.

---

## Recipe viewers and Jade

JEI, EMI and REI are optional. Each machine type has a category you can step through with the arrows (Shift jumps 10). In JEI, click an upgrade item to see its recipes.

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
