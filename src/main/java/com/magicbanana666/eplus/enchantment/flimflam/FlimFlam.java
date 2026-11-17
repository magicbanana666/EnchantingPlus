package com.magicbanana666.eplus.enchantment.flimflam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import com.magicbanana666.eplus.EPlusConfig;
import com.magicbanana666.eplus.EnchantingPlus;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Flim Flam: when a player hurts another player, the one with less Flim Flam loses luck.
 * Players with bad luck get pranked every now and then (see {@link FlimFlamEffects}), which brings their luck back up.
 * <p>
 * The attacker's level is the level on their weapon. The victim's level is the total on their armor divided by three
 * (armor has more pieces to put it on).
 * <p>
 * Ported from <a href="https://github.com/OpenMods/OpenBlocks">OpenBlocks</a> by the OpenMods Team (MIT).
 */
public final class FlimFlam {

    public static final ResourceKey<Enchantment> ENCHANTMENT = ResourceKey.create(Registries.ENCHANTMENT, EnchantingPlus.id("flim_flam"));

    /** Pranks start once the luck is below -LUCK_MARGIN... */
    private static final int LUCK_MARGIN = -30;
    /** ...and happen at most every 15 seconds. */
    private static final int EFFECT_DELAY = 20 * 15;
    private static final RandomSource RANDOM = RandomSource.create();

    /** The luck of a player. Only the luck itself is saved. */
    public static final class Luck {
        private static final MapCodec<Luck> CODEC = Codec.INT.fieldOf("luck").xmap(Luck::new, l -> l.luck);

        public int luck;
        private int cooldown;
        private boolean forceNext;

        public Luck() {
            this(0);
        }

        private Luck(int luck) {
            this.luck = luck;
        }
    }

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, EnchantingPlus.MOD_ID);
    public static final Supplier<AttachmentType<Luck>> LUCK = ATTACHMENTS.register("luck", () -> AttachmentType.builder(() -> new Luck()).serialize(Luck.CODEC).build());

    private static final SimpleCommandExceptionType UNKNOWN_EFFECT = new SimpleCommandExceptionType(Component.translatable("commands.eplus.flimflam.unknown"));
    private static final SimpleCommandExceptionType FAILED = new SimpleCommandExceptionType(Component.translatable("commands.eplus.flimflam.failed"));

    private FlimFlam() {
    }

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        NeoForge.EVENT_BUS.addListener(FlimFlam::onDamage);
        NeoForge.EVENT_BUS.addListener(FlimFlam::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(FlimFlam::registerCommands);
    }

    private static Luck luck(Player player) {
        return player.getData(LUCK);
    }

    // ---- Losing luck -------------------------------------------------------------------------------------------

    private static void onDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer targetPlayer) || !(event.getSource().getEntity() instanceof ServerPlayer sourcePlayer) || sourcePlayer == targetPlayer) {
            return;
        }
        final Holder<Enchantment> enchantment = enchantment(targetPlayer);
        if (enchantment == null) {
            return;
        }
        final int sourceFlimFlam = EnchantmentHelper.getItemEnchantmentLevel(enchantment, sourcePlayer.getMainHandItem());
        int targetFlimFlam = 0;
        for (final EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
            targetFlimFlam += EnchantmentHelper.getItemEnchantmentLevel(enchantment, targetPlayer.getItemBySlot(slot));
        }
        // Armor is less effective, since there are more pieces to put it on.
        final int diff = targetFlimFlam / 3 - sourceFlimFlam;
        if (diff == 0) {
            return;
        }
        // The one with less Flim Flam is the victim.
        final ServerPlayer victim = diff > 0 ? sourcePlayer : targetPlayer;
        final Luck luck = luck(victim);
        for (int i = 0; i < Math.abs(diff); i++) {
            final int roll = RANDOM.nextInt(20) + 1;
            if (roll == 20) {
                // Critical!
                luck.forceNext = true;
            }
            luck.luck -= roll;
        }
        if (luck.luck < LUCK_MARGIN) {
            luck.forceNext = true;
        }
    }

    private static Holder<Enchantment> enchantment(Player player) {
        return player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(ENCHANTMENT).orElse(null);
    }

    // ---- Getting pranked ---------------------------------------------------------------------------------------

    private static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player && player.isAlive()) {
            deliverKarma(player);
        }
    }

    private static void deliverKarma(ServerPlayer player) {
        final Luck luck = luck(player);
        if (!canFlimFlam(luck)) {
            return;
        }
        final List<FlimFlamEffects.Effect> options = new ArrayList<>();
        for (final FlimFlamEffects.Effect effect : FlimFlamEffects.ALL) {
            if (luck.luck <= effect.maxLuck() && isAllowed(effect)) {
                options.add(effect);
            }
        }
        // Pick by weight; when a prank can't happen (EG no sheep around), try another one.
        while (!options.isEmpty()) {
            final FlimFlamEffects.Effect effect = pickWeighted(options);
            options.remove(effect);
            if (execute(player, effect)) {
                luck.luck -= effect.cost();
                return;
            }
        }
    }

    private static FlimFlamEffects.Effect pickWeighted(List<FlimFlamEffects.Effect> options) {
        int total = 0;
        for (final FlimFlamEffects.Effect effect : options) {
            total += effect.weight();
        }
        int roll = RANDOM.nextInt(total);
        for (final FlimFlamEffects.Effect effect : options) {
            roll -= effect.weight();
            if (roll < 0) {
                return effect;
            }
        }
        return options.getLast();
    }

    private static boolean execute(ServerPlayer player, FlimFlamEffects.Effect effect) {
        try {
            if (effect.action().execute(player)) {
                if (!effect.silent()) {
                    player.sendSystemMessage(Component.translatable("eplus.flimflam.flimflammed"));
                }
                return true;
            }
        }
        catch (RuntimeException e) {
            EnchantingPlus.LOG.warn("Error during flim flam '{}'", effect.name(), e);
        }
        return false;
    }

    private static boolean isAllowed(FlimFlamEffects.Effect effect) {
        if (EPlusConfig.flimFlamSafeOnly() && !effect.safe()) {
            return false;
        }
        return !EPlusConfig.flimFlamBlacklist().contains(effect.name());
    }

    private static boolean canFlimFlam(Luck luck) {
        if (luck.forceNext) {
            luck.forceNext = false;
            luck.cooldown = EFFECT_DELAY;
            return true;
        }
        if (luck.luck > -LUCK_MARGIN || luck.cooldown-- > 0) {
            return false;
        }
        luck.cooldown = EFFECT_DELAY;
        // The worse the luck, the more likely (up to 75%).
        final double probability = 0.75 * 2.0 * Math.abs(Math.atan(luck.luck / 250.0) / Math.PI);
        return RANDOM.nextDouble() < probability;
    }

    // ---- Commands ----------------------------------------------------------------------------------------------

    /**
     * /flimflam &lt;players&gt; [&lt;prank&gt;] - prank players right away (a random prank if none is given).<br>
     * /flimflam luck &lt;player&gt; [&lt;amount&gt;] - show the luck of a player, or add to it.
     */
    private static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("flimflam")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("luck")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(FlimFlam::showLuck)
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                        .executes(FlimFlam::addLuck))))
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(ctx -> prank(ctx, null))
                        .then(Commands.argument("prank", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(FlimFlamEffects.ALL.stream().map(FlimFlamEffects.Effect::name), builder))
                                .executes(ctx -> prank(ctx, StringArgumentType.getString(ctx, "prank"))))));
    }

    private static int prank(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
        final Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        int done = 0;
        for (final ServerPlayer target : targets) {
            final FlimFlamEffects.Effect effect = name == null ? FlimFlamEffects.ALL.get(RANDOM.nextInt(FlimFlamEffects.ALL.size())) : FlimFlamEffects.byName(name);
            if (effect == null) {
                throw UNKNOWN_EFFECT.create();
            }
            if (execute(target, effect)) {
                done++;
                ctx.getSource().sendSuccess(() -> Component.translatable("commands.eplus.flimflam.success", target.getDisplayName(), effect.name()), true);
            }
        }
        if (done == 0) {
            throw FAILED.create();
        }
        return done;
    }

    private static int showLuck(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        final int value = luck(player).luck;
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.eplus.flimflam.luck", player.getDisplayName(), value), false);
        return value;
    }

    private static int addLuck(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        final Luck luck = luck(player);
        luck.luck += IntegerArgumentType.getInteger(ctx, "amount");
        final int value = luck.luck;
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.eplus.flimflam.luck", player.getDisplayName(), value), true);
        return value;
    }
}
