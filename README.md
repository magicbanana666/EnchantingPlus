# Enchanting Plus (26.1.2)

Enchanting Plus gives you full control over enchanting. Instead of the random enchanting table, the
**Advanced Enchantment Table** lets you pick exactly which enchantments go on an item, and at which level,
and you pay for it with experience.

Enchanting Plus is for **Minecraft 26.1.2 on NeoForge** and has no dependencies. It also adds the
[Lucky Mining](#lucky-mining) and [Flim Flam](#flim-flam) enchantments.

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

### Enchanting animation

When you enchant an item, the table screen closes and the item rises over the book. Rune circles light up on
the table, glyphs fly in from the bookshelves while the item spins up, and it ends in a burst of sparkles with
a beam of light. Other players nearby see it too. You can turn it off, or keep the screen open, in
`config/eplus-client.toml` (`enchantAnimation`, `closeScreenOnEnchant`).

The animation is based on [Cinematic Interactions](https://modrinth.com/mod/cinematic-interactions) by Southware (MIT).

### Your own slot

Every player has their own item slot in the table, so you can leave an item in it like in a chest and other
players won't see it. Breaking the table drops the items of all players. The armor slots on the left side of
the table are your normal armor slots.

## Lucky Mining

Enchanting Plus adds one enchantment of its own: **Lucky Mining** (I-III, pickaxes). When you mine an ore,
there is a chance that a stone block next to it turns into the same ore, with green and gold particles and
a chime. The base chance is 10% / 15% / 20%; mining the same ore again within 3 seconds raises it, up to
twice the base chance.

It is a normal (non-treasure) enchantment, so it is also available from the vanilla enchanting table,
villagers and loot. Which blocks spread and which blocks they can replace are set by the block tags
`eplus:lucky_mining/ores` and `eplus:lucky_mining/stones`.

Lucky Mining is a port of [Lucky Mining](https://github.com/Buuz135/Lucky-Mining-MC) by Buuz135 (MIT).

## Flim Flam

**Flim Flam** (I-IV, armor and swords) is a PvP prank enchantment. When a player hurts another player, the one
with less Flim Flam loses luck: the attacker counts the level on their weapon, the victim counts the total on
their armor divided by three. Players with bad luck get pranked every now and then, and each prank brings
their luck back up a bit. The worse the luck, the worse the pranks:

| Prank | What happens |
| --- | --- |
| `sound` | A scary sound only the victim hears |
| `sheep-dye` | A nearby sheep gets a random color |
| `epic-lore` | A held or worn item gets a silly history in its tooltip |
| `living-rename` | A nearby mob gets a silly name |
| `invisible-mobs` | About a third of the nearby mobs turn invisible for 10 minutes |
| `inventory-shuffle` | The inventory gets shuffled |
| `squid` | A squid hat |
| `useless-tool` | A completely broken tool or armor piece with a random enchantment drops |
| `bane` | A bucket, saddle or similar item gets Bane of Arthropods V |
| `snowballs` | 200 snowballs shoot up (and come back down) |
| `disarm` | Some armor and the held item are dropped |
| `creepers` | 15 charged creepers fall from the sky (their explosions do no damage) |
| `effect` | Two random, very strong potion effects (EG Speed 50, Levitation) |
| `teleport` | An ender pearl is thrown in a random direction |
| `mount` | The victim rides a random mob nearby |
| `encase` | A dirt cage with a torch around the victim |
| `skyblock` | Teleported 150 blocks up, onto a tiny ice platform |

The first nine are "safe" (they don't kill). Server admins can allow only those with `flimFlam.safeOnly`, or
turn off single pranks with `flimFlam.blacklist`.

Commands (needs cheats / op):

- `/flimflam <players> [<prank>]` - prank players right away (a random prank if none is given).
- `/flimflam luck <player> [<amount>]` - show the luck of a player, or add to it (EG `-500` for very bad luck).

Flim Flam is a port of the enchantment from [OpenBlocks](https://github.com/OpenMods/OpenBlocks) by the OpenMods
Team (MIT). The custom OpenBlocks sounds (mosquito, alarm clock, ...) were replaced by vanilla sounds.

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
| `luckyMining.tier1Chance` | 0.10 | Base chance of Lucky Mining I to spread an ore |
| `luckyMining.tier2Chance` | 0.15 | Same, for Lucky Mining II |
| `luckyMining.tier3Chance` | 0.20 | Same, for Lucky Mining III |
| `flimFlam.safeOnly` | false | Only allow Flim Flam pranks that don't kill |
| `flimFlam.blacklist` | [] | Flim Flam pranks that never happen, EG `"skyblock"` |

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
