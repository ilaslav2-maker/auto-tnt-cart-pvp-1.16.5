package com.example.tntmacro;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

public class TntMacro implements ClientModInitializer {
    private boolean isRunning = false;
    private int tickCounter = 0;
    private int originalSlot = 0;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            long window = client.getWindow().getHandle();
            if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_4) == GLFW.GLFW_PRESS && !isRunning) {
                if (hasRequiredItems(client)) {
                    isRunning = true;
                    tickCounter = 0;
                    originalSlot = client.player.inventory.selectedSlot;
                }
            }

            if (isRunning) {
                tickCounter++;
                BlockHitResult lookTarget = getLookTarget(client);

                if (lookTarget == null && tickCounter <= 2) {
                    resetMacro(client);
                    return;
                }

                // ТАКТ 1: Скрытно прожимаем щит + ставим рельсы
                if (tickCounter == 1) {
                    sendPacketUseItem(client, findItemSlot(client, Items.SHIELD), Hand.OFF_HAND);
                    sendPacketUseItemOnBlock(client, findItemSlot(client, Items.RAIL), lookTarget);
                }
                // ТАКТ 2: Ставим вагонетку ТНТ
                else if (tickCounter == 2) {
                    sendPacketUseItemOnBlock(client, findItemSlot(client, Items.TNT_MINECART), lookTarget);
                }
                // ТАКТ 3: Выстрел из лука (детонация)
                else if (tickCounter == 3) {
                    sendPacketUseItem(client, findBowSlot(client), Hand.MAIN_HAND);
                }
                // ТАКТ 13: Сброс пакетов (прошло 500мс удержания щита)
                else if (tickCounter >= 13) {
                    resetMacro(client);
                }
            }
        });
    }

    private void sendPacketUseItemOnBlock(MinecraftClient client, int slot, BlockHitResult target) {
        if (slot == -1) return;
        if (slot >= 9) {
            ItemStack stackInInv = client.player.inventory.getStack(slot);
            ItemStack stackOnHotbar = client.player.inventory.getStack(0);
            client.player.inventory.setStack(0, stackInInv);
            client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(0));
            client.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, target));
            client.player.inventory.setStack(0, stackOnHotbar);
        } else {
            client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            client.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, target));
        }
    }

    private void sendPacketUseItem(MinecraftClient client, int slot, Hand hand) {
        if (slot == -1) return;
        if (slot >= 9) {
            ItemStack stackInInv = client.player.inventory.getStack(slot);
            ItemStack stackOnHotbar = client.player.inventory.getStack(1);
            client.player.inventory.setStack(1, stackInInv);
            client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(1));
            client.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(hand));
            client.player.inventory.setStack(1, stackOnHotbar);
        } else {
            client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            client.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(hand));
        }
    }

    private boolean hasRequiredItems(MinecraftClient client) {
        return findBowSlot(client) != -1 && findItemSlot(client, Items.ARROW) != -1 && findItemSlot(client, Items.RAIL) != -1 && findItemSlot(client, Items.TNT_MINECART) != -1 && findItemSlot(client, Items.SHIELD) != -1;
    }

    private int findBowSlot(MinecraftClient client) {
        PlayerInventory inv = client.player.inventory;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.getItem() == Items.BOW && EnchantmentHelper.getLevel(Enchantments.FLAME, stack) > 0) return i;
        }
        return -1;
    }

    private int findItemSlot(MinecraftClient client, net.minecraft.item.Item item) {
        PlayerInventory inv = client.player.inventory;
        for (int i = 0; i < 36; i++) {
            if (inv.getStack(i).getItem() == item) return i;
        }
        return -1;
    }

    private BlockHitResult getLookTarget(MinecraftClient client) {
        if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            return (BlockHitResult) client.crosshairTarget;
        }
        return null;
    }

    private void resetMacro(MinecraftClient client) {
        isRunning = false;
        tickCounter = 0;
        if (client.player != null) client.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(originalSlot));
    }
}
