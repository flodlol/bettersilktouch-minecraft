package dev.jonas.bettersilktouch.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;

public final class BetterSilkTouchClient implements ClientModInitializer {
    private static final int WARNING_DURATION_TICKS = 30;
    private static final int WARNING_COOLDOWN_TICKS = 10;

    private static int warningTicksLeft = 0;
    private static int soundCooldownTicks = 0;

    @Override
    public void onInitializeClient() {
        BetterSilkTouchConfig.load();

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!world.isClient()) {
                return ActionResult.PASS;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.interactionManager == null || player == null) {
                return ActionResult.PASS;
            }
            if (client.interactionManager.getCurrentGameMode() == GameMode.CREATIVE) {
                return ActionResult.PASS;
            }

            BlockState state = world.getBlockState(pos);
            Identifier blockId = Registries.BLOCK.getId(state.getBlock());
            if (!BetterSilkTouchConfig.INSTANCE.contains(blockId)) {
                return ActionResult.PASS;
            }

            ItemStack stack = player.getStackInHand(hand);
            if (hasSilkTouch(stack)) {
                return ActionResult.PASS;
            }

            showBlockedFeedback(client, pos);
            return ActionResult.FAIL;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (warningTicksLeft > 0) {
                warningTicksLeft--;
            }
            if (soundCooldownTicks > 0) {
                soundCooldownTicks--;
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            if (warningTicksLeft <= 0) {
                return;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.options.hudHidden) {
                return;
            }

            Text warningText = Text.translatable("text.bettersilktouch.blocked").formatted(Formatting.RED);
            int screenWidth = client.getWindow().getScaledWidth();
            int screenHeight = client.getWindow().getScaledHeight();
            int textWidth = client.textRenderer.getWidth(warningText);
            int x = screenWidth / 2;
            int y = screenHeight - 86;

            drawContext.fill(x - (textWidth / 2) - 4, y - 2, x + (textWidth / 2) + 4, y + 10, 0x66000000);

            drawContext.drawCenteredTextWithShadow(
                client.textRenderer,
                warningText,
                x,
                y,
                0xFF5555
            );
        });
    }

    private static boolean hasSilkTouch(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.getEnchantments().getEnchantments().stream().anyMatch(enchantment -> enchantment.matchesKey(Enchantments.SILK_TOUCH));
    }

    private static void showBlockedFeedback(MinecraftClient client, BlockPos pos) {
        warningTicksLeft = WARNING_DURATION_TICKS;

        if (soundCooldownTicks > 0 || client.player == null || client.world == null) {
            return;
        }

        soundCooldownTicks = WARNING_COOLDOWN_TICKS;
        client.world.playSound(
            client.player,
            pos,
            SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(),
            SoundCategory.PLAYERS,
            0.35F,
            1.35F
        );
    }
}
