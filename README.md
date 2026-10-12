# Enchanting Plus (26.1.2)

Enchanting Plus gives you full control over enchanting. Instead of the random enchanting table, the
**Advanced Enchantment Table** lets you pick exactly which enchantments go on an item, and at which level,
and you pay for it with experience.

Enchanting Plus is for **Minecraft 26.1.2 on NeoForge** and has no dependencies.

## Download / install

1. Grab the jar from the [Releases](https://github.com/magicbanana666/EnchantingPlus/releases) page.
2. Put it in the `mods` folder of a Minecraft 26.1.2 NeoForge instance (NeoForge 26.1.2.115 or newer).
3. The mod is needed on both the client and the server.

## Getting the Advanced Enchantment Table

The table is meant to be an end-game block, so the recipe is expensive:

```
 Lapis Block     Dragon Egg        Lapis Block
 Nether Star     Enchanting Table  Nether Star
 Netherite Block Netherite Block   Netherite Block
```

## Using the table

1. Right click the table to open it.
2. Put the item you want to enchant in the slot at the top left. Tools, weapons, armor, books and already
   enchanted items all work.
3. The list on the right shows the enchantments you can use:
   - first the enchantments that are **meant for the item** (EG Sharpness on a sword),
   - then, under **- Other enchantments -**, every other enchantment in the game, including the ones from
     other mods. These can be applied too, even if they normally can't go on that item.
4. Drag (or click on) the slider of an enchantment to choose its level. Moving it all the way to the left
   removes the enchantment.
5. The box on the left shows your experience, the cost and your bookshelf discount.
6. Click the enchanted book button to apply. The cost is paid in experience points.

Tips:

- Hold **Shift** while hovering over an enchantment to see a description of what it does.
- Enchantments that conflict with one you already chose (EG Sharpness and Smite) are shown in red and are
  locked until you remove the other one.
- Enchantments above their normal maximum level (from commands or other mods) are locked so you don't lose them.
- Damaged items can't have enchantments removed.
- Enchanting a plain book turns it into an enchanted book; removing everything from an enchanted book
  turns it back into a plain book.

### Cost and discount

The cost of each new enchantment level depends on how rare the enchantment is: common enchantments like
Sharpness are cheap, rare ones like Thorns are expensive. Treasure enchantments cost 4x more and curses 3x more.
Removing enchantments is free (but gives no experience back), except removing curses, which costs as much as
adding them.

Bookshelves around the table work just like with the vanilla enchanting table (two blocks away, with air
in between). Every bookshelf gives 1% discount, up to 30%.

### Treasure enchantments and curses

- **Treasure enchantments** (Mending, Frost Walker, Soul Speed, Swift Sneak, Wind Burst, ...) only show up
  during the **day**, when the table stands on a **3x3 floor of beacon base blocks** (iron, gold, emerald,
  diamond or netherite blocks). Golden particles around the table show that the floor is right.
- **Curses** can only be added or removed around **midnight on a full moon**. Portal particles swirl around
  the table when that time has come.

### Your own slot

Every player has their own item slot in the table, so you can leave an item in it like in a chest and other
players won't see it. Breaking the table drops the items of all players. The armor slots on the left side of
the table are your normal armor slots.

## Configuration

The settings are in `<world>/serverconfig/eplus-server.toml` (also editable in game through the mod list):

| Setting | Default | What it does |
| --- | --- | --- |
| `baseCost` | 45 | Base cost in experience points of one enchantment level |
| `costFactor` | 1.0 | Multiplier for all costs (0.5 = half price) |
| `treasureFactor` | 4.0 | Extra multiplier for treasure enchantments |
| `curseFactor` | 3.0 | Extra multiplier for curses |
| `allowOtherEnchantments` | true | Show the "Other enchantments" category |
| `blacklistedItems` | [] | Items that can't be enchanted, EG `"minecraft:elytra"` |
| `blacklistedEnchantments` | [] | Enchantments that are never offered, EG `"minecraft:mending"` |

Modpacks can also use the item tag `eplus:blacklisted` and the enchantment tag `eplus:blacklisted`.

## For mod developers

Two events are posted on `NeoForge.EVENT_BUS` (both sides):

- `com.magicbanana666.eplus.api.event.EnchantmentCostEvent` - change the cost of adding levels of an enchantment.
- `com.magicbanana666.eplus.api.event.AvailableEnchantmentsEvent` - add or remove enchantments offered for an item.

Enchantment descriptions use the translation key `enchantment.<namespace>.<name>.desc`, the same key as the
Enchantment Descriptions mod, so descriptions added for that mod also show up here.

## Building

```
./gradlew build
```

Needs JDK 25. The jar ends up in `build/libs` (this repository builds into `~/.gradle-builds/EnchantingPlus`).

## License

[Don't Be an Asshole License](LICENSE.md): do whatever you want with it, port it, share it, just don't be an asshole.
