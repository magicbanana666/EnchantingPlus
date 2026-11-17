package com.magicbanana666.eplus.gametest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import com.magicbanana666.eplus.EnchantingPlus;
import com.magicbanana666.eplus.enchantment.flimflam.FlimFlam;
import com.magicbanana666.eplus.enchantment.flimflam.FlimFlamEffects;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Game tests for Flim Flam. Run them with {@code ./gradlew runGameTestServer}.
 * Every prank is run on a real (fake-connection) server player and its effect is checked.
 */
@EventBusSubscriber(modid = EnchantingPlus.MOD_ID)
public final class FlimFlamTests {

    private static final Map<String, Consumer<GameTestHelper>> TESTS = new LinkedHashMap<>();
    private static int playerCounter;

    static {
        TESTS.put("prank_inventory_shuffle", FlimFlamTests::inventoryShuffle);
        TESTS.put("prank_useless_tool", FlimFlamTests::uselessTool);
        TESTS.put("prank_bane", FlimFlamTests::bane);
        TESTS.put("prank_epic_lore", FlimFlamTests::epicLore);
        TESTS.put("prank_living_rename", FlimFlamTests::livingRename);
        TESTS.put("prank_squid", FlimFlamTests::squid);
        TESTS.put("prank_sheep_dye", FlimFlamTests::sheepDye);
        TESTS.put("prank_invisible_mobs", FlimFlamTests::invisibleMobs);
        TESTS.put("prank_sound", FlimFlamTests::sound);
        TESTS.put("prank_snowballs", FlimFlamTests::snowballs);
        TESTS.put("prank_teleport", FlimFlamTests::teleport);
        TESTS.put("prank_mount", FlimFlamTests::mount);
        TESTS.put("prank_encase", FlimFlamTests::encase);
        TESTS.put("prank_creepers", FlimFlamTests::creepers);
        TESTS.put("prank_disarm", FlimFlamTests::disarm);
        TESTS.put("prank_effect", FlimFlamTests::effect);
        TESTS.put("prank_skyblock", FlimFlamTests::skyblock);
        TESTS.put("luck_weapon_beats_bare_victim", FlimFlamTests::luckWeapon);
        TESTS.put("luck_armor_beats_bare_attacker", FlimFlamTests::luckArmor);
        TESTS.put("luck_equal_does_nothing", FlimFlamTests::luckEqual);
        TESTS.put("command_flimflam", FlimFlamTests::command);
    }

    private FlimFlamTests() {
    }

    // ---- Registration ------------------------------------------------------------------------------------------

    private static ResourceKey<Consumer<GameTestHelper>> functionKey(String name) {
        return ResourceKey.create(Registries.TEST_FUNCTION, EnchantingPlus.id("flimflam/" + name));
    }

    @SubscribeEvent
    public static void registerFunctions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, helper -> TESTS.forEach((name, test) -> helper.register(functionKey(name).identifier(), test)));
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        final Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(EnchantingPlus.id("flimflam"), new TestEnvironmentDefinition.AllOf(List.of()));
        TESTS.keySet().forEach(name -> event.registerTest(EnchantingPlus.id("flimflam/" + name),
                new FunctionGameTestInstance(functionKey(name),
                        // Lots of padding: some pranks look 20-40 blocks around the player.
                        new TestData<>(environment, Identifier.withDefaultNamespace("empty"), 200, 0, true, net.minecraft.world.level.block.Rotation.NONE, false, 1, 1, true, 50))));
    }

    // ---- Helpers -----------------------------------------------------------------------------------------------

    /** A survival server player with a fake connection, standing on a stone floor in the test area. */
    private static ServerPlayer player(GameTestHelper helper, int x, int z) {
        final ServerLevel level = helper.getLevel();
        final String name = "ff_" + (playerCounter++);
        final CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), name), false);
        final ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        // Players can't be hurt until their client says it has loaded; a fake player never does.
        player.connection.markClientLoaded();
        // The test server puts new players in creative, where they can't be hurt.
        player.setGameMode(GameType.SURVIVAL);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                helper.setBlock(new BlockPos(x + dx, 0, z + dz), Blocks.STONE);
            }
        }
        final Vec3 at = helper.absoluteVec(new Vec3(x + 0.5, 1, z + 0.5));
        player.teleportTo(at.x, at.y, at.z);
        return player;
    }

    private static void done(GameTestHelper helper, ServerPlayer... players) {
        for (final ServerPlayer player : players) {
            player.stopRiding();
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
        helper.succeed();
    }

    private static boolean run(ServerPlayer player, String prank) {
        return FlimFlamEffects.byName(prank).action().execute(player);
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        helper.assertTrue(condition, Component.literal(message));
    }

    private static <T extends net.minecraft.world.entity.Entity> List<T> around(ServerPlayer player, Class<T> type, double range) {
        return player.level().getEntitiesOfClass(type, player.getBoundingBox().inflate(range));
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    private static ItemStack withFlimFlam(GameTestHelper helper, ItemStack stack, int level) {
        stack.enchant(enchantment(helper, FlimFlam.ENCHANTMENT), level);
        return stack;
    }

    // ---- Pranks ------------------------------------------------------------------------------------------------

    private static void inventoryShuffle(GameTestHelper helper) {
        final ServerPlayer p = player(helper, 0, 0);
        final List<ItemStack> before = new ArrayList<>();
        final ItemStack[] kinds = {new ItemStack(Items.DIRT), new ItemStack(Items.STONE), new ItemStack(Items.OAK_LOG), new ItemStack(Items.SAND)};
        for (int i = 0; i < 36; i++) {
            final ItemStack stack = kinds[i % kinds.length].copyWithCount(1 + i % 60);
            p.getInventory().setItem(i, stack);
            before.add(stack.copy());
        }
        check(helper, run(p, "inventory-shuffle"), "prank returned false");
        int moved = 0;
        int total = 0;
        for (int i = 0; i < 36; i++) {
            final ItemStack now = p.getInventory().getItem(i);
            total += now.getCount();
            if (!ItemStack.matches(now, before.get(i))) {
                moved++;
            }
        }
        check(helper, moved > 10, "only " + moved + " slots changed");
        check(helper, total == before.stream().mapToInt(ItemStack::getCount).sum(), "items were lost or duplicated");
        done(helper, p);
    }

    private static void uselessTool(GameTestHelper helper) {
        final ServerPlayer p = player(helper, 0, 0);
        check(helper, run(p, "useless-tool"), "prank returned false");
        final List<ItemEntity> drops = around(p, ItemEntity.class, 2);
        check(helper, drops.size() == 1, "expected 1 dropped item, got " + drops.size());
        final ItemStack tool = drops.getFirst().getItem();
        check(helper, tool.isEnchanted(), "dropped item is not enchanted");
        check(helper, tool.getDamageValue() == tool.getMaxDamage(), "dropped item is not fully broken");
        EnchantingPlus.LOG.info("[flimflam test] useless-tool dropped {} with {}", tool.getItem(), EnchantmentHelper.getEnchantmentsForCrafting(tool));
        done(helper, p);
    }

    private static void bane(GameTestHelper helper) {
        final ServerPlayer p = player(helper, 0, 0);
        p.getInventory().setItem(5, new ItemStack(Items.WATER_BUCKET));
        check(helper, run(p, "bane"), "prank returned false");
        check(helper, EnchantmentHelper.getItemEnchantmentLevel(enchantment(helper, Enchantments.BANE_OF_ARTHROPODS), p.getInventory().getItem(5)) == 5, "water bucket has no Bane of Arthropods V");
        done(helper, p);
    }

    private static void epicLore(GameTestHelper helper) {
        final ServerPlayer p = player(helper, 0, 0);
        p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_PICKAXE));
        check(helper, run(p, "epic-lore"), "prank returned false");
        final ItemLore lore = p.getMainHandItem().get(DataComponents.LORE);
        check(helper, lore != null && !lore.lines().isEmpty(), "no lore added");
        for (int i = 0; i < 5; i++) {
            run(p, "epic-lore");
            EnchantingPlus.LOG.info("[flimflam test] epic-lore: {}", String.join(" / ", p.getMainHandItem().get(DataComponents.LORE).lines().stream().map(Component::getString).toList()));
        }
        done(helper, p);
    }

    private static void livingRename(GameTestHelper helper) {
        final ServerPlayer p = player(helper, 0, 0);
        final Cow cow = helper.spawn(EntityTypes.COW, new BlockPos(2, 1, 2));
        check(helper, run(p, "living-rename"), "prank returned false");
        check(helper, cow.hasCustomName(), "cow has no name");
        EnchantingPlus.LOG.info("[flimflam test] living-rename: {}", cow.getCustomName().getString());
        cow.discard();
        done(helper, p);
    }

    private static void squid(GameTestHelper helper) {
        final ServerPlayer p = player(helper, 0, 0);
        check(helper, run(p, "squid"), "prank returned false");
        final List<Squid> squids = around(p, Squid.class, 3);
        check(helper, squids.size() == 1, "expected 1 squid, got " + squids.size());
        check(helper, squids.getFirst().hasCustomName(), "squid has no name");
        EnchantingPlus.LOG.info("[flimflam test] squid: '{}'", squids.getFirst().getCustomName().getString());
        squids.getFirst().discard();
        done(helper, p);
    }

    private static void sheepDye(GameTestHelper helper) {
        final ServerPlayer p = player(helper, 0, 0);
        final Sheep sheep = helper.spawn(EntityTypes.SHEEP, new BlockPos(2, 1, 2));
        sheep.setColor(DyeColor.WHITE);
        boolean changed = false;
        for (int i = 0; i < 10 && !changed; i++) {
            check(helper, run(p, "sheep-dye"), "prank returned false");
            changed = sheep.getColor() != DyeColor.WHITE;
        }
        check(helper, changed, "sheep stayed white");
        sheep.discard();
        done(helper, p);
    }

    private static void invisibleMobs(GameTestHelper helper) {
        final ServerPlayer p = player(helper, 0, 0);
        final List<Cow> cows = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            cows.add(helper.spawn(EntityTypes.COW, new BlockPos(-2 + i % 5, 1, -2 + i / 5 % 5)));
        }
        check(helper, run(p, "invisible-mobs"), "prank returned false");
        final long invisible = cows.stream().filter(c -> c.hasEffect(MobEffects.INVISIBILITY)).count();
        check(helper, invisible > 0 && invisible < 30, "invisible cows: " + invisible + "/30");
        EnchantingPlus.LOG.info("[flimflam test] invisible-mobs: {}/30 cows invisible", invisible);
        cows.forEach(Cow::discard);
        done(helper, p);
    }

    private static void sound(GameTestHelper helper) {
        final ServerPlayer p = player(helper, 0, 0);
        check(helper, run(p, "sound"), "prank returned false");
        done(helper, p);
    }

    private static void snowballs(GameTestHelper helper) {
        final ServerPlayer p = player(helper, 0, 0);
        check(helper, run(p, "snowballs"), "prank returned false");
        final int count = around(p, Snowball.class, 6).size();
        check(helper, count == 200, "expected 200 snowballs, got " + count);
        around(p, Snowball.class, 6).forEach(Snowball::discard);
        done(helper, p);
    }

    private static void teleport(GameTestHelper helper) {
        final ServerPlayer p = player(helper, 0, 0);
        check(helper, run(p, "teleport"), "prank returned false");
        final List<ThrownEnderpearl> pearls = around(p, ThrownEnderpearl.class, 3);
        check(helper, pearls.size() == 1, "expected 1 ender pearl, got " + pearls.size());
        check(helper, pearls.getFirst().getOwner() == p, "pearl is not thrown by the player");
        pearls.getFirst().discard();
        done(helper, p);
    }

    private static void mount(GameTestHelper helper) {
        final ServerPlayer p = player(helper, 0, 0);
        final Cow cow = helper.spawn(EntityTypes.COW, new BlockPos(2, 1, 0));
        check(helper, run(p, "mount"), "prank returned false");
        check(helper, p.getVehicle() == cow, "player is not riding the cow");
        p.stopRiding();
        cow.discard();
        done(helper, p);
    }

    private static void encase(GameTestHelper helper) {
        // Away from the barrier walls of the (1x1) test structure.
        final ServerPlayer p = player(helper, 4, 4);
        final BlockPos feet = p.blockPosition();
        check(helper, run(p, "encase"), "prank returned false");
        check(helper, helper.getLevel().getBlockState(feet.above(2)).is(Blocks.DIRT), "no dirt roof");
        check(helper, helper.getLevel().getBlockState(feet.east()).is(Blocks.DIRT), "no dirt wall");
        check(helper, helper.getLevel().getBlockState(feet).isAir(), "dirt inside the player");
        final var torch = helper.getLevel().getBlockState(feet.above());
        check(helper, torch.is(Blocks.TORCH) || torch.is(Blocks.WALL_TORCH), "no torch, found " + torch);
        done(helper, p);
    }

    private static void creepers(GameTestHelper helper) {
        final ServerPlayer p = player(helper, 0, 0);
        check(helper, run(p, "creepers"), "prank returned false");
        final List<Creeper> creepers = around(p, Creeper.class, 15);
        check(helper, creepers.size() == 15, "expected 15 creepers, got " + creepers.size());
        check(helper, creepers.stream().allMatch(Creeper::isPowered), "not all creepers are charged");
        // Blow one up next to the player: no block damage, no player damage.
        final Creeper creeper = creepers.getFirst();
        creepers.stream().skip(1).forEach(Creeper::discard);
        creeper.teleportTo(p.getX() + 1, p.getY(), p.getZ());
        creeper.setNoAi(false);
        creeper.ignite();
        final float health = p.getHealth();
        helper.runAfterDelay(100, () -> {
            check(helper, creeper.isRemoved(), "the creeper did not explode: ignited " + creeper.isIgnited() + " swell " + creeper.getSwelling(0) + " at " + creeper.blockPosition() + " player " + p.blockPosition() + " alive " + creeper.isAlive() + " ticks " + creeper.tickCount);
            check(helper, helper.getLevel().getBlockState(p.blockPosition().below()).is(Blocks.STONE), "the explosion broke the floor");
            check(helper, p.getHealth() >= health, "the explosion hurt the player: " + health + " -> " + p.getHealth());
            done(helper, p);
        });
    }

    private static void disarm(GameTestHelper helper) {
        final ServerPlayer p = player(helper, 0, 0);
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        boolean dropped = false;
        for (int i = 0; i < 10 && !dropped; i++) {
            dropped = run(p, "disarm");
        }
        check(helper, dropped, "nothing was dropped");
        final int worn = (int) java.util.stream.Stream.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND).filter(s -> !p.getItemBySlot(s).isEmpty()).count();
        final int onGround = around(p, ItemEntity.class, 6).size();
        check(helper, worn + onGround == 5, "items lost: " + worn + " worn + " + onGround + " dropped");
        around(p, ItemEntity.class, 6).forEach(ItemEntity::discard);
        done(helper, p);
    }

    private static void effect(GameTestHelper helper) {
        final ServerPlayer p = player(helper, 0, 0);
        check(helper, run(p, "effect"), "prank returned false");
        check(helper, p.getActiveEffects().size() == 2, "expected 2 effects, got " + p.getActiveEffects().size());
        EnchantingPlus.LOG.info("[flimflam test] effect: {}", p.getActiveEffects());
        done(helper, p);
    }

    private static void skyblock(GameTestHelper helper) {
        final ServerPlayer p = player(helper, 0, 0);
        final double y = p.getY();
        check(helper, run(p, "skyblock"), "prank returned false");
        check(helper, p.getY() > y + 100, "player was not teleported up: " + y + " -> " + p.getY());
        final BlockPos feet = p.blockPosition();
        check(helper, helper.getLevel().getBlockState(feet.below()).is(Blocks.ICE), "no ice under the player");
        check(helper, helper.getLevel().getBlockState(feet.east()).is(Blocks.ICE), "no ice next to the player");
        for (final BlockPos pos : new BlockPos[] {feet.below(), feet.east(), feet.west(), feet.north(), feet.south()}) {
            helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        done(helper, p);
    }

    // ---- Luck --------------------------------------------------------------------------------------------------

    /** Attacker with a Flim Flam IV sword hits a victim without Flim Flam armor: the victim loses luck and gets pranked. */
    private static void luckWeapon(GameTestHelper helper) {
        final ServerPlayer attacker = player(helper, 0, 0);
        final ServerPlayer victim = player(helper, 2, 0);
        attacker.setItemSlot(EquipmentSlot.MAINHAND, withFlimFlam(helper, new ItemStack(Items.DIAMOND_SWORD), 4));
        victim.getData(FlimFlam.LUCK).luck = -100;
        final boolean hurt = victim.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(attacker), 1);
        check(helper, hurt, "the attack did not hurt the victim");
        final int afterHit = victim.getData(FlimFlam.LUCK).luck;
        check(helper, afterHit <= -104, "victim did not lose luck: " + afterHit);
        check(helper, attacker.getData(FlimFlam.LUCK).luck == 0, "attacker lost luck");
        // Luck below -30 forces a prank on the next tick, which brings some luck back.
        victim.doTick();
        final int afterPrank = victim.getData(FlimFlam.LUCK).luck;
        check(helper, afterPrank > afterHit, "no prank happened: luck " + afterHit + " -> " + afterPrank);
        EnchantingPlus.LOG.info("[flimflam test] weapon hit: victim luck -100 -> {} -> {} after a prank", afterHit, afterPrank);
        cleanupPranks(helper, victim);
        done(helper, attacker, victim);
    }

    /** Victim with 3 pieces of Flim Flam IV armor (12 / 3 = 4) is hit by a bare-handed attacker: the attacker loses luck. */
    private static void luckArmor(GameTestHelper helper) {
        final ServerPlayer attacker = player(helper, 0, 0);
        final ServerPlayer victim = player(helper, 2, 0);
        victim.setItemSlot(EquipmentSlot.HEAD, withFlimFlam(helper, new ItemStack(Items.IRON_HELMET), 4));
        victim.setItemSlot(EquipmentSlot.CHEST, withFlimFlam(helper, new ItemStack(Items.IRON_CHESTPLATE), 4));
        victim.setItemSlot(EquipmentSlot.LEGS, withFlimFlam(helper, new ItemStack(Items.IRON_LEGGINGS), 4));
        victim.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(attacker), 1);
        final int attackerLuck = attacker.getData(FlimFlam.LUCK).luck;
        check(helper, attackerLuck <= -4 && attackerLuck >= -80, "attacker luck should drop by 4d20, is " + attackerLuck);
        check(helper, victim.getData(FlimFlam.LUCK).luck == 0, "victim lost luck");
        done(helper, attacker, victim);
    }

    /** Flim Flam I sword against 3 levels of armor (3 / 3 = 1): nothing happens. */
    private static void luckEqual(GameTestHelper helper) {
        final ServerPlayer attacker = player(helper, 0, 0);
        final ServerPlayer victim = player(helper, 2, 0);
        attacker.setItemSlot(EquipmentSlot.MAINHAND, withFlimFlam(helper, new ItemStack(Items.IRON_SWORD), 1));
        victim.setItemSlot(EquipmentSlot.FEET, withFlimFlam(helper, new ItemStack(Items.IRON_BOOTS), 3));
        victim.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(attacker), 1);
        check(helper, attacker.getData(FlimFlam.LUCK).luck == 0 && victim.getData(FlimFlam.LUCK).luck == 0, "luck changed although Flim Flam was equal");
        done(helper, attacker, victim);
    }

    // ---- Command -----------------------------------------------------------------------------------------------

    private static void command(GameTestHelper helper) {
        final ServerPlayer p = player(helper, 0, 0);
        final var source = helper.getLevel().getServer().createCommandSourceStack().withSuppressedOutput();
        final var commands = helper.getLevel().getServer().getCommands();
        commands.performPrefixedCommand(source, "flimflam luck " + p.getScoreboardName() + " -250");
        check(helper, p.getData(FlimFlam.LUCK).luck == -250, "/flimflam luck did not change the luck: " + p.getData(FlimFlam.LUCK).luck);
        commands.performPrefixedCommand(source, "flimflam " + p.getScoreboardName() + " effect");
        check(helper, p.getActiveEffects().size() == 2, "/flimflam <player> effect did not apply 2 effects");
        done(helper, p);
    }

    /** Removes whatever a random prank may have left around the player. */
    private static void cleanupPranks(GameTestHelper helper, ServerPlayer player) {
        player.stopRiding();
        final AABB area = player.getBoundingBox().inflate(25);
        helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Entity.class, area, e -> !(e instanceof ServerPlayer)).forEach(net.minecraft.world.entity.Entity::discard);
    }
}
