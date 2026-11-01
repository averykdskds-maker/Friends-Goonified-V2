package com.rotbound.dailychaos;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

@Mod(RotboundDailyChaos.MOD_ID)
public final class RotboundDailyChaos {
    public static final String MOD_ID = "rotbound_daily_chaos";
    private static final Logger LOGGER = LogUtils.getLogger();

    private final DailyState state = new DailyState();
    private static volatile DailyState activeState;

    public RotboundDailyChaos() {
        activeState = state;
        NeoForge.EVENT_BUS.register(this);
        LOGGER.info("Rotbound Daily Chaos initialised (server-side). One setting will advance on each new UTC day.");
    }

    public static int fireTickDelayMode() {
        DailyState current = activeState;
        return current == null ? 0 : current.fireTickDelayMode();
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        state.start(event.getServer());
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        state.tick(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        state.stop();
    }

    // Player Interaction is a server-side lock covering block/item/entity use,
    // player attacks, block placement, and ordinary block mining attempts.
    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!state.isLoaded()) return;
        if (!state.isEnabled(DailySetting.PLAYER_INTERACTION)) {
            event.setCanceled(true);
            return;
        }
        if (!state.isEnabled(DailySetting.ENCHANTMENTS)
                && event.getLevel().getBlockState(event.getPos()).is(Blocks.ENCHANTING_TABLE)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (state.isLoaded() && !state.isEnabled(DailySetting.PLAYER_INTERACTION)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (state.isLoaded() && !state.isEnabled(DailySetting.PLAYER_INTERACTION)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (state.isLoaded() && !state.isEnabled(DailySetting.PLAYER_INTERACTION)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (state.isLoaded() && !state.isEnabled(DailySetting.PLAYER_INTERACTION)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (state.isLoaded() && !state.isEnabled(DailySetting.PLAYER_INTERACTION)
                && event.getEntity() instanceof net.minecraft.world.entity.player.Player) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onBlockToolModification(BlockEvent.BlockToolModificationEvent event) {
        if (state.isLoaded() && !state.isEnabled(DailySetting.PLAYER_INTERACTION)
                && event.getPlayer() != null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onAnvilUpdate(AnvilUpdateEvent event) {
        if (!state.isLoaded() || state.isEnabled(DailySetting.ENCHANTMENTS)) {
            return;
        }
        var left = event.getLeft();
        var right = event.getRight();
        // Allow repairing/renaming an already-enchanted item with a plain material.
        // Cancel only when the operation supplies enchantments or creates an enchanted output.
        boolean addsEnchantments = right.isEnchanted()
                || right.is(Items.ENCHANTED_BOOK)
                || (!left.isEnchanted() && event.getOutput().isEnchanted());
        if (addsEnchantments) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (state.isLoaded() && !state.isEnabled(DailySetting.EXPLOSION_BLOCK_BREAKING)) {
            event.getAffectedBlocks().clear();
        }
    }
}
