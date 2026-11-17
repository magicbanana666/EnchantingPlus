package com.magicbanana666.eplus.client.fx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.magicbanana666.eplus.block.entity.BookBlockEntity;
import com.magicbanana666.eplus.client.AdvancedTableScreen;
import com.magicbanana666.eplus.client.EPlusClientConfig;
import com.magicbanana666.eplus.network.EnchantFxPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The enchanting animation, played at a table after an item was enchanted:
 * the item appears over the book and spins up while magic is drawn in from the bookshelves,
 * then bursts into light with a beam going up, and fades away.
 * <p>
 * Timing, colors and effects follow the enchanting ritual of
 * <a href="https://modrinth.com/mod/cinematic-interactions">Cinematic Interactions</a> by Southware (MIT).
 */
public final class EnchantRitual {

    /** Seconds the ritual (spin up, burst, beam) takes. */
    public static final double RITUAL = 1.5;
    /** Seconds after the start when the item bursts. */
    public static final double BURST = 0.5;
    /** Seconds after the start when the item is gone again. */
    public static final double END = 2.6;
    /** Seconds the item takes to fade away at the end. */
    public static final double FADE = 0.45;

    private static final Map<BlockPos, EnchantRitual> ACTIVE = new HashMap<>();
    private static final RandomSource RANDOM = RandomSource.create();
    private static final float[][] PALETTE = {{0.70F, 0.50F, 0.85F}, {0.61F, 0.58F, 0.76F}, {0.95F, 0.86F, 1.0F}};
    private static final int MAX_SPARKS = 400;

    public final BlockPos pos;
    public final ItemStack item;
    public final double start;
    private final List<Vec3> shelves;
    private final List<Spark> sparks = new ArrayList<>();
    private boolean burstDone;
    private int ticks;

    private EnchantRitual(ClientLevel level, BlockPos pos, ItemStack item) {
        this.pos = pos;
        this.item = item;
        this.start = clock();
        this.shelves = new ArrayList<>();
        for (final BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            if (EnchantingTableBlock.isValidBookShelf(level, pos, offset)) {
                this.shelves.add(Vec3.atCenterOf(pos.offset(offset)));
            }
        }
    }

    /** Seconds since some fixed point, smooth between frames. */
    public static double clock() {
        return Util.getNanos() / 1.0E9;
    }

    public static EnchantRitual get(BlockPos pos) {
        return ACTIVE.get(pos);
    }

    public static void handle(EnchantFxPayload payload, IPayloadContext context) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (payload.playerId() == mc.player.getId() && EPlusClientConfig.closeScreenOnEnchant() && EPlusClientConfig.enchantAnimation()
                && mc.gui.screen() instanceof AdvancedTableScreen screen && screen.getMenu().logic.getPos().equals(payload.pos())) {
            mc.player.closeContainer();
        }
        if (EPlusClientConfig.enchantAnimation()) {
            final EnchantRitual ritual = new EnchantRitual(mc.level, payload.pos(), payload.item());
            ACTIVE.put(payload.pos(), ritual);
            ritual.begin(mc.level);
        }
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            ACTIVE.clear();
            return;
        }
        final Iterator<EnchantRitual> it = ACTIVE.values().iterator();
        while (it.hasNext()) {
            if (!it.next().tick(level)) {
                it.remove();
            }
        }
    }

    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            ACTIVE.clear();
        }
    }

    /** Time since the start, in seconds. */
    public double age(double clock) {
        return clock - this.start;
    }

    /** Progress of the ritual from 0 to 1, or -1 when it is over. */
    public static double ritualProgress(double age) {
        final double t = age / RITUAL;
        return t >= 0 && t <= 1 ? t : -1;
    }

    /** Where the item floats (in world coordinates). */
    public Vec3 itemPos(double age) {
        return top().add(0, 0.62 + Math.sin(age * 1.6) * 0.025, 0);
    }

    public Vec3 top() {
        return new Vec3(this.pos.getX() + 0.5, this.pos.getY() + 0.75, this.pos.getZ() + 0.5);
    }

    /** A snapshot of the sparks that are alive. */
    public List<Spark> sparks() {
        return List.copyOf(this.sparks);
    }

    private void begin(ClientLevel level) {
        sound(level, SoundEvents.BEACON_ACTIVATE, 0.45F, 1.7F);
        sound(level, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8F, 0.9F);
        final Vec3 at = top().add(0, 0.35, 0);
        for (int i = 0; i < 40; i++) {
            level.addParticle(ParticleTypes.ENCHANT, at.x + RANDOM.nextGaussian() * 0.3, at.y + RANDOM.nextGaussian() * 0.3, at.z + RANDOM.nextGaussian() * 0.3,
                    RANDOM.nextGaussian() * 0.6, RANDOM.nextGaussian() * 0.6, RANDOM.nextGaussian() * 0.6);
        }
    }

    /** @return false when the ritual is over */
    private boolean tick(ClientLevel level) {
        final double now = clock();
        final double age = age(now);
        this.ticks++;
        this.sparks.removeIf(s -> now - s.born >= s.life);
        final Vec3 itemPos = itemPos(age);

        // The magic is drawn in from the bookshelves while the item spins up.
        if (age < BURST) {
            for (int i = 0; i < 3; i++) {
                stream(level, source(), itemPos);
                gather(now, itemPos, 0.55);
            }
        }
        if (!this.burstDone && age >= BURST) {
            this.burstDone = true;
            burst(level, now, itemPos, 1.4);
            sound(level, SoundEvents.BEACON_POWER_SELECT, 0.7F, 1.3F);
            sound(level, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 0.7F);
        }
        if (!this.item.isEmpty() && age < END - FADE && this.ticks % 3 == 0) {
            sparkle(now, itemPos, 0.2);
        }
        // The book turns its pages during the ritual.
        if (age < RITUAL && this.ticks % 3 == 0 && level.getBlockEntity(this.pos) instanceof BookBlockEntity book) {
            book.flipT += (RANDOM.nextBoolean() ? 1 : -1) * (1 + RANDOM.nextInt(2));
        }
        return age < END || !this.sparks.isEmpty();
    }

    private void sound(ClientLevel level, SoundEvent sound, float volume, float pitch) {
        level.playLocalSound(this.pos, sound, SoundSource.BLOCKS, volume, pitch, false);
    }

    private Vec3 source() {
        if (!this.shelves.isEmpty()) {
            return this.shelves.get(RANDOM.nextInt(this.shelves.size())).add(0, 0.3, 0);
        }
        final double a = RANDOM.nextDouble() * Math.PI * 2;
        return top().add(Math.cos(a) * 1.6, 0.6 + RANDOM.nextDouble() * 0.8, Math.sin(a) * 1.6);
    }

    /** An enchanting glyph flying from {@code from} to {@code to}. */
    private static void stream(ClientLevel level, Vec3 from, Vec3 to) {
        final double jx = (RANDOM.nextDouble() - 0.5) * 0.5;
        final double jy = (RANDOM.nextDouble() - 0.5) * 0.4;
        final double jz = (RANDOM.nextDouble() - 0.5) * 0.5;
        level.addParticle(ParticleTypes.ENCHANT, to.x, to.y, to.z, from.x - to.x + jx, from.y - to.y + jy, from.z - to.z + jz);
    }

    private void spark(double now, Vec3 at, Vec3 velocity, double life, float size) {
        if (this.sparks.size() < MAX_SPARKS) {
            final float[] c = PALETTE[RANDOM.nextInt(PALETTE.length)];
            this.sparks.add(new Spark(at.x, at.y, at.z, velocity.x, velocity.y, velocity.z, now, life, size, c[0], c[1], c[2], this.sparks.size()));
        }
    }

    private void burst(ClientLevel level, double now, Vec3 at, double strength) {
        for (int i = 0; i < (int) (70 * strength); i++) {
            final Vec3 dir = new Vec3(RANDOM.nextGaussian(), RANDOM.nextGaussian() * 0.7 + 0.25, RANDOM.nextGaussian()).normalize();
            spark(now, at, dir.scale(0.35 + RANDOM.nextDouble()), 0.6 + RANDOM.nextDouble() * 0.8, 0.018F + RANDOM.nextFloat() * 0.03F);
        }
        for (int i = 0; i < (int) (8 * strength); i++) {
            level.addParticle(ParticleTypes.REVERSE_PORTAL,
                    at.x + (RANDOM.nextDouble() - 0.5) * 0.3, at.y + (RANDOM.nextDouble() - 0.5) * 0.3, at.z + (RANDOM.nextDouble() - 0.5) * 0.3,
                    (RANDOM.nextDouble() - 0.5) * 0.05, RANDOM.nextDouble() * 0.04, (RANDOM.nextDouble() - 0.5) * 0.05);
        }
        for (int i = 0; i < (int) (30 * strength); i++) {
            final Vec3 dir = new Vec3(RANDOM.nextGaussian(), RANDOM.nextGaussian(), RANDOM.nextGaussian()).normalize().scale(1.1 + RANDOM.nextDouble() * 0.6);
            level.addParticle(ParticleTypes.ENCHANT, at.x, at.y, at.z, dir.x, dir.y, dir.z);
        }
    }

    private void sparkle(double now, Vec3 at, double radius) {
        final double a = RANDOM.nextDouble() * Math.PI * 2;
        final double r = radius * (0.6 + RANDOM.nextDouble() * 0.4);
        spark(now, at.add(Math.cos(a) * r, (RANDOM.nextDouble() - 0.5) * 0.2, Math.sin(a) * r),
                new Vec3(0, 0.05 + RANDOM.nextDouble() * 0.05, 0), 1.0 + RANDOM.nextDouble() * 0.6, 0.012F + RANDOM.nextFloat() * 0.012F);
    }

    private void gather(double now, Vec3 at, double radius) {
        final Vec3 dir = new Vec3(RANDOM.nextGaussian(), RANDOM.nextGaussian(), RANDOM.nextGaussian()).normalize();
        final double life = 0.45 + RANDOM.nextDouble() * 0.2;
        spark(now, at.add(dir.scale(radius)), dir.scale(-radius / life * 1.6), life, 0.014F + RANDOM.nextFloat() * 0.012F);
    }

    /**
     * A small glowing mote. Its position is computed from its age (drag slows it down, it slowly floats up),
     * so it moves smoothly no matter the frame rate.
     */
    public record Spark(double x, double y, double z, double vx, double vy, double vz, double born, double life, float size, float r, float g, float b, int phase) {

        private static final double DRAG = 2.2;
        /** Upward acceleration. */
        private static final double LIFT = 0.04;

        public Vec3 position(double age) {
            final double slow = (1 - Math.exp(-DRAG * age)) / DRAG;
            final double vy0 = this.vy - LIFT / DRAG;
            return new Vec3(this.x + this.vx * slow, this.y + vy0 * slow + LIFT * age / DRAG, this.z + this.vz * slow);
        }

        /** Brightness from 0 to 1: fades in and out and twinkles. */
        public float brightness(double age) {
            final float fade = (float) Math.sin(Math.PI * age / this.life);
            final float twinkle = 0.75F + 0.25F * (float) Math.sin(age * 23.0 + this.phase);
            return Math.max(0, fade * twinkle);
        }

        public float radius(double age) {
            final float fade = (float) Math.sin(Math.PI * age / this.life);
            return this.size * (0.6F + 0.4F * Math.max(0, fade));
        }
    }
}
