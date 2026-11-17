package com.magicbanana666.eplus.enchantment.flimflam;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The pranks of Flim Flam. Each one returns false when it could not do anything (EG no sheep around),
 * so another one is tried.
 * <p>
 * Ported from the flim flams of <a href="https://github.com/OpenMods/OpenBlocks">OpenBlocks</a> by the OpenMods Team (MIT).
 */
public final class FlimFlamEffects {

    /** A prank. */
    @FunctionalInterface
    public interface Action {
        boolean execute(ServerPlayer target);
    }

    /**
     * @param cost   luck the victim gets back when it happens (negative = the luck goes up)
     * @param weight how often it is picked compared to the others
     * @param maxLuck it only happens when the victim's luck is at or below this
     * @param safe   it (almost) never kills
     * @param silent the victim does not get the "something weird happens" message
     */
    public record Effect(String name, int cost, int weight, int maxLuck, boolean safe, boolean silent, Action action) {
    }

    public static final List<Effect> ALL = List.of(
            new Effect("inventory-shuffle", -50, 100, -50, true, false, FlimFlamEffects::inventoryShuffle),
            new Effect("useless-tool", -125, 50, -125, true, false, FlimFlamEffects::uselessTool),
            new Effect("bane", -125, 100, -125, true, false, FlimFlamEffects::bane),
            new Effect("epic-lore", -10, 100, -10, true, false, FlimFlamEffects::epicLore),
            new Effect("living-rename", -10, 100, -10, true, false, FlimFlamEffects::livingRename),
            new Effect("squid", -75, 50, -75, true, false, FlimFlamEffects::squid),
            new Effect("sheep-dye", -5, 50, -5, true, false, FlimFlamEffects::sheepDye),
            new Effect("invisible-mobs", -25, 10, -25, true, false, FlimFlamEffects::invisibleMobs),
            new Effect("sound", -5, 150, -5, true, true, FlimFlamEffects::sound),
            new Effect("snowballs", -50, 50, -50, false, false, FlimFlamEffects::snowballs),
            new Effect("teleport", -100, 30, -100, false, false, FlimFlamEffects::teleport),
            new Effect("mount", -150, 25, -150, false, false, FlimFlamEffects::mount),
            new Effect("encase", -50, 50, -300, false, false, FlimFlamEffects::encase),
            new Effect("creepers", -60, 50, -60, false, false, FlimFlamEffects::creepers),
            new Effect("disarm", -50, 50, -50, false, false, FlimFlamEffects::disarm),
            new Effect("effect", -75, 75, -75, false, false, FlimFlamEffects::effect),
            new Effect("skyblock", -100, 150, -400, false, false, FlimFlamEffects::skyblock));

    private static final RandomSource RANDOM = RandomSource.create();

    private FlimFlamEffects() {
    }

    public static Effect byName(String name) {
        for (final Effect effect : ALL) {
            if (effect.name().equals(name)) {
                return effect;
            }
        }
        return null;
    }

    private static <T> T pick(List<T> list) {
        return list.get(RANDOM.nextInt(list.size()));
    }

    private static AABB around(ServerPlayer target, double range) {
        return target.getBoundingBox().inflate(range);
    }

    // ---- Items -------------------------------------------------------------------------------------------------

    /** Shuffles the main inventory (only when no other container is open). */
    private static boolean inventoryShuffle(ServerPlayer target) {
        if (target.containerMenu != target.inventoryMenu) {
            return false;
        }
        Collections.shuffle(target.getInventory().getNonEquipmentItems());
        target.getInventory().setChanged();
        return true;
    }

    private static final List<Item> USELESS_TOOLS = List.of(
            Items.DIAMOND_PICKAXE, Items.GOLDEN_PICKAXE, Items.IRON_PICKAXE, Items.STONE_PICKAXE, Items.WOODEN_PICKAXE,
            Items.DIAMOND_SHOVEL, Items.GOLDEN_SHOVEL, Items.IRON_SHOVEL, Items.STONE_SHOVEL, Items.WOODEN_SHOVEL,
            Items.DIAMOND_AXE, Items.GOLDEN_AXE, Items.IRON_AXE, Items.STONE_AXE, Items.WOODEN_AXE,
            Items.SHEARS,
            Items.DIAMOND_LEGGINGS, Items.GOLDEN_LEGGINGS, Items.IRON_LEGGINGS, Items.CHAINMAIL_LEGGINGS, Items.LEATHER_LEGGINGS,
            Items.DIAMOND_BOOTS, Items.GOLDEN_BOOTS, Items.IRON_BOOTS, Items.CHAINMAIL_BOOTS, Items.LEATHER_BOOTS,
            Items.DIAMOND_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.IRON_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.LEATHER_CHESTPLATE,
            Items.DIAMOND_HELMET, Items.GOLDEN_HELMET, Items.IRON_HELMET, Items.CHAINMAIL_HELMET, Items.LEATHER_HELMET);

    /** Drops a fully broken tool or armor piece with a random enchantment at the victim's feet. */
    private static boolean uselessTool(ServerPlayer target) {
        final ItemStack stack = new ItemStack(pick(USELESS_TOOLS));
        final List<EnchantmentInstance> options = EnchantmentHelper.getAvailableEnchantmentResults(30, stack,
                target.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements().map(h -> (Holder<net.minecraft.world.item.enchantment.Enchantment>) h));
        if (options.isEmpty()) {
            return false;
        }
        final EnchantmentInstance chosen = pick(options);
        stack.enchant(chosen.enchantment(), RANDOM.nextInt(chosen.level()) + 1);
        stack.setDamageValue(stack.getMaxDamage());
        target.level().addFreshEntity(new ItemEntity(target.level(), target.getX(), target.getY(), target.getZ(), stack));
        return true;
    }

    /** Puts Bane of Arthropods V on a non-enchantable item that does not stack, like a water bucket or a saddle. */
    private static boolean bane(ServerPlayer target) {
        final Holder<net.minecraft.world.item.enchantment.Enchantment> bane = target.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.BANE_OF_ARTHROPODS);
        for (final ItemStack stack : target.getInventory().getNonEquipmentItems()) {
            if (!stack.isEmpty() && stack.getMaxStackSize() == 1 && !stack.isEnchantable() && !stack.isEnchanted()) {
                stack.enchant(bane, 5);
                return true;
            }
        }
        return false;
    }

    /** Gives a random held or worn item an epic history. */
    private static boolean epicLore(ServerPlayer target) {
        final List<EquipmentSlot> slots = new ArrayList<>(Arrays.asList(EquipmentSlot.values()));
        Collections.shuffle(slots);
        for (final EquipmentSlot slot : slots) {
            final ItemStack stack = target.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            final String lore = LoreGenerator.generateLore(RANDOM, target.getName().getString(), identityType(stack));
            final List<Component> lines = new ArrayList<>();
            for (final String line : splitText(lore, 30)) {
                lines.add(Component.literal(line).withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC));
            }
            stack.set(DataComponents.LORE, new ItemLore(lines));
            return true;
        }
        return false;
    }

    private static List<String> splitText(String text, int maxSize) {
        final List<String> result = new ArrayList<>();
        final StringBuilder line = new StringBuilder();
        for (final String word : text.split(" ")) {
            if (word.isEmpty()) {
                continue;
            }
            if (!line.isEmpty() && line.length() + word.length() > maxSize) {
                result.add(line.toString());
                line.setLength(0);
            }
            if (!line.isEmpty()) {
                line.append(' ');
            }
            line.append(word);
        }
        if (!line.isEmpty()) {
            result.add(line.toString());
        }
        return result;
    }

    private static String identityType(ItemStack stack) {
        final Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable != null) {
            switch (equippable.slot()) {
                case HEAD:
                    return "helmet";
                case CHEST:
                    return "chestplate";
                case LEGS:
                    return "leggings";
                case FEET:
                    return "boots";
                default:
                    break;
            }
        }
        if (stack.is(ItemTags.PICKAXES)) {
            return "pickaxe";
        }
        if (stack.is(Items.SHEARS)) {
            return "shears";
        }
        if (stack.is(ItemTags.AXES)) {
            return "axe";
        }
        if (stack.is(ItemTags.SHOVELS)) {
            return "shovel";
        }
        if (stack.getItem() instanceof BlockItem) {
            return "block";
        }
        if (stack.getItem() instanceof BucketItem) {
            return "bucket";
        }
        return "gizmo";
    }

    /** Makes the victim drop some of their armor and the held item. */
    private static boolean disarm(ServerPlayer target) {
        boolean dropped = false;
        for (final EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD, EquipmentSlot.MAINHAND}) {
            final ItemStack stack = target.getItemBySlot(slot);
            if (!stack.isEmpty() && RANDOM.nextFloat() <= 0.5F) {
                // Scattered around the victim, like an item dropped on death.
                final ItemEntity drop = target.createItemStackToDrop(stack.split(1), true, false);
                if (drop != null) {
                    target.level().addFreshEntity(drop);
                }
                dropped = true;
            }
        }
        return dropped;
    }

    // ---- Mobs --------------------------------------------------------------------------------------------------

    /** Gives a nearby mob a silly name. */
    private static boolean livingRename(ServerPlayer target) {
        final List<Mob> mobs = new ArrayList<>(target.level().getEntitiesOfClass(Mob.class, around(target, 20)));
        Collections.shuffle(mobs);
        for (final Mob mob : mobs) {
            if (!mob.hasCustomName()) {
                String name = LoreGenerator.generateName(RANDOM);
                if (name.length() > 64) {
                    name = name.substring(0, 61) + "...";
                }
                mob.setCustomName(Component.literal(name));
                return true;
            }
        }
        return false;
    }

    private static final List<String> SQUID_NAMES = List.of("Fancy Hat", "Hello there!", "Look at my hat!", "My hat is amazing!", "Ceci n'est pas une pipe", "???");

    /** A squid called "Fancy Hat" (or similar) appears at the victim's feet. (Entities can't ride players in 26.1.) */
    private static boolean squid(ServerPlayer target) {
        if (target.isVehicle()) {
            return false;
        }
        final Squid squid = EntityTypes.SQUID.create(target.level(), EntitySpawnReason.MOB_SUMMONED);
        if (squid == null) {
            return false;
        }
        squid.snapTo(target.getX(), target.getY(), target.getZ(), target.getYRot(), 0);
        squid.setCustomName(Component.literal(pick(SQUID_NAMES)));
        target.level().addFreshEntity(squid);
        return true;
    }

    /** Dyes a nearby sheep a random color. */
    private static boolean sheepDye(ServerPlayer target) {
        final List<Sheep> sheep = target.level().getEntitiesOfClass(Sheep.class, around(target, 20));
        if (sheep.isEmpty()) {
            return false;
        }
        pick(sheep).setColor(DyeColor.values()[RANDOM.nextInt(DyeColor.values().length)]);
        return true;
    }

    /** Makes about a third of the nearby mobs invisible for 10 minutes. */
    private static boolean invisibleMobs(ServerPlayer target) {
        final List<Mob> mobs = target.level().getEntitiesOfClass(Mob.class, around(target, 20));
        if (mobs.isEmpty()) {
            return false;
        }
        for (final Mob mob : mobs) {
            if (RANDOM.nextFloat() < 0.3F) {
                mob.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 10 * 60 * 20, 1));
            }
        }
        return true;
    }

    /** Makes the victim ride a random mob nearby. */
    private static boolean mount(ServerPlayer target) {
        final List<Mob> mobs = new ArrayList<>(target.level().getEntitiesOfClass(Mob.class, around(target, 40), e -> !(e instanceof Creeper) && !(e instanceof Squid)));
        Collections.shuffle(mobs);
        for (final Mob mob : mobs) {
            if (target.startRiding(mob, true, true)) {
                return true;
            }
        }
        return false;
    }

    /** Charged creepers fall from the sky around the victim. They explode without doing any damage. */
    private static boolean creepers(ServerPlayer target) {
        final ServerLevel level = target.level();
        final CompoundTag tag = new CompoundTag();
        tag.putBoolean("powered", true);
        tag.putByte("ExplosionRadius", (byte) 0);
        for (int i = 0; i < 15; i++) {
            final double x = target.getX() + 20 * (RANDOM.nextFloat() - 0.5);
            final double y = target.getY() + 5 * (1 + RANDOM.nextFloat());
            final double z = target.getZ() + 20 * (RANDOM.nextFloat() - 0.5);
            final Entity creeper = EntityType.loadEntityRecursive(EntityTypes.CREEPER, tag.copy(), level, EntitySpawnReason.MOB_SUMMONED, e -> {
                e.snapTo(x, y, z, e.getYRot(), e.getXRot());
                return e;
            });
            if (creeper != null) {
                level.addFreshEntity(creeper);
            }
        }
        return true;
    }

    // ---- The victim --------------------------------------------------------------------------------------------

    private record PotionPrank(Holder<MobEffect> effect, int levelMin, int levelMax, int durationMin, int durationMax) {
    }

    private static final List<PotionPrank> POTIONS = List.of(
            new PotionPrank(MobEffects.BLINDNESS, 1, 1, 15 * 20, 60 * 20),
            new PotionPrank(MobEffects.NAUSEA, 1, 1, 15 * 20, 60 * 20),
            new PotionPrank(MobEffects.MINING_FATIGUE, 50, 100, 15 * 20, 60 * 20),
            new PotionPrank(MobEffects.JUMP_BOOST, 30, 50, 5 * 20, 15 * 20),
            new PotionPrank(MobEffects.SPEED, 50, 100, 5 * 20, 15 * 20),
            new PotionPrank(MobEffects.SLOWNESS, 4, 7, 5 * 20, 15 * 20),
            new PotionPrank(MobEffects.LEVITATION, 1, 1, 5 * 20, 15 * 20));

    /** Two random, very strong potion effects. */
    private static boolean effect(ServerPlayer target) {
        final List<PotionPrank> potions = new ArrayList<>(POTIONS);
        Collections.shuffle(potions);
        for (int i = 0; i < 2; i++) {
            final PotionPrank prank = potions.get(i);
            final int duration = prank.durationMin() + RANDOM.nextInt(prank.durationMax() - prank.durationMin() + 1);
            final int amplifier = prank.levelMin() + RANDOM.nextInt(prank.levelMax() - prank.levelMin() + 1);
            target.addEffect(new MobEffectInstance(prank.effect(), duration, amplifier));
        }
        return true;
    }

    /** Throws an ender pearl in a random direction, as if the victim threw it. */
    private static boolean teleport(ServerPlayer target) {
        final ThrownEnderpearl pearl = new ThrownEnderpearl(target.level(), target, new ItemStack(Items.ENDER_PEARL));
        pearl.setPos(target.getX(), target.getY() + 1, target.getZ());
        pearl.setDeltaMovement(RANDOM.nextGaussian(), 0.5, RANDOM.nextGaussian());
        target.level().addFreshEntity(pearl);
        return true;
    }

    /** 200 snowballs shoot up from above the victim's head (and come back down). */
    private static boolean snowballs(ServerPlayer target) {
        for (int i = 0; i < 200; i++) {
            final Snowball snowball = new Snowball(target.level(), target.getX(), target.getY() + 4, target.getZ(), new ItemStack(Items.SNOWBALL));
            snowball.setDeltaMovement(RANDOM.nextGaussian() * 0.05, 1, RANDOM.nextGaussian() * 0.05);
            target.level().addFreshEntity(snowball);
        }
        return true;
    }

    /** Builds a dirt cage around the victim, with a torch inside. */
    private static boolean encase(ServerPlayer target) {
        final ServerLevel level = target.level();
        final int playerX = (int) Math.floor(target.getX());
        final int playerY = (int) Math.floor(target.getBoundingBox().minY) - 1;
        final int playerZ = (int) Math.floor(target.getZ());
        for (int y = playerY; y <= playerY + 3; y++) {
            for (int x = playerX - 1; x <= playerX + 1; x++) {
                for (int z = playerZ - 1; z <= playerZ + 1; z++) {
                    final boolean isGap = y < playerY + 3 && x == playerX && z == playerZ;
                    final BlockPos pos = new BlockPos(x, y, z);
                    if (!isGap && level.isEmptyBlock(pos)) {
                        level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
                    }
                }
            }
        }
        // A torch at head height, on the floor if possible, else on one of the walls.
        final BlockPos torchPos = new BlockPos(playerX, playerY + 2, playerZ);
        if (level.isEmptyBlock(torchPos)) {
            BlockState torch = Blocks.TORCH.defaultBlockState();
            for (final Direction facing : Direction.Plane.HORIZONTAL) {
                if (torch.canSurvive(level, torchPos)) {
                    break;
                }
                torch = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, facing);
            }
            if (torch.canSurvive(level, torchPos)) {
                level.setBlockAndUpdate(torchPos, torch);
            }
        }
        return true;
    }

    /** Teleports the victim 150 blocks up, onto a tiny ice platform. */
    private static boolean skyblock(ServerPlayer target) {
        final ServerLevel level = target.level();
        if (level.dimensionType().hasCeiling() || level.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, target.blockPosition())) {
            return false;
        }
        final BlockPos center = BlockPos.containing(target.getX(), Math.min(target.getY() + 150, level.getMaxY() - 2), target.getZ());
        final BlockPos[] blocks = {center.relative(Direction.DOWN), center.relative(Direction.EAST), center.relative(Direction.NORTH), center.relative(Direction.SOUTH), center.relative(Direction.WEST)};
        for (final BlockPos pos : blocks) {
            if (!level.isEmptyBlock(pos)) {
                return false;
            }
        }
        for (final BlockPos pos : blocks) {
            if (!level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState())) {
                return false;
            }
        }
        target.teleportTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
        return true;
    }

    // ---- Sounds ------------------------------------------------------------------------------------------------

    private static final List<SoundEvent> SOUNDS = List.of(
            SoundEvents.TNT_PRIMED,
            SoundEvents.GENERIC_EXPLODE.value(),
            SoundEvents.ELDER_GUARDIAN_CURSE,
            SoundEvents.BLAZE_SHOOT,
            SoundEvents.ENDERMAN_STARE,
            SoundEvents.GHAST_SCREAM,
            SoundEvents.GHAST_SHOOT,
            SoundEvents.ZOMBIFIED_PIGLIN_ANGRY,
            SoundEvents.CREEPER_PRIMED,
            SoundEvents.WARDEN_NEARBY_CLOSE,
            SoundEvents.ITEM_BREAK.value(),
            SoundEvents.BEE_LOOP_AGGRESSIVE);

    /** A scary sound only the victim hears. */
    private static boolean sound(ServerPlayer target) {
        final Holder<SoundEvent> sound = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(pick(SOUNDS));
        target.connection.send(new ClientboundSoundPacket(sound, SoundSource.MASTER, target.getX(), target.getY(), target.getZ(), 1, 1, RANDOM.nextLong()));
        return true;
    }
}
