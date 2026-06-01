package com.example.tntmacro;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerTryUseItemOnBlockPacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod("tntmacro")
public class TntMacro {
    private final Minecraft mc = Minecraft.getInstance();
    private boolean isRunning = false;
    private int tickCounter = 0;
    private int originalSlot = 0;
    public TntMacro() { MinecraftForge.EVENT_BUS.register(this); }
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.player == null || mc.world == null) return;
        if (GLFW.glfwGetMouseButton(mc.getMainWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_4) == GLFW.GLFW_PRESS && !isRunning) {
            if (hasRequiredItems()) { isRunning = true; tickCounter = 0; originalSlot = mc.player.inventory.currentItem; }
        }
        if (isRunning) {
            tickCounter++;
            BlockRayTraceResult lookTarget = getLookTarget();
            if (lookTarget == null && tickCounter <= 2) { resetMacro(); return; }
            if (tickCounter == 1) { 
                sendPacketUseItem(findItemSlot(Items.SHIELD), Hand.OFF_HAND);
                sendPacketUseItemOnBlock(findItemSlot(Items.RAIL), lookTarget); 
            }
            else if (tickCounter == 2) { sendPacketUseItemOnBlock(findItemSlot(Items.TNT_MINECART), lookTarget); }
            else if (tickCounter == 3) { sendPacketUseItem(findBowSlot(), Hand.MAIN_HAND); }
            else if (tickCounter >= 13) { resetMacro(); }
        }
    }
    private void sendPacketUseItemOnBlock(int slot, BlockRayTraceResult target) {
        if (slot == -1) return;
        if (slot >= 9) {
            ItemStack stackInInv = mc.player.inventory.getStackInSlot(slot);
            ItemStack stackOnHotbar = mc.player.inventory.getStackInSlot(0);
            mc.player.inventory.setInventorySlotContents(0, stackInInv);
            mc.player.connection.sendPacket(new CHeldItemChangePacket(0));
            mc.player.connection.sendPacket(new CPlayerTryUseItemOnBlockPacket(Hand.MAIN_HAND, target));
            mc.player.inventory.setInventorySlotContents(0, stackOnHotbar);
        } else {
            mc.player.connection.sendPacket(new CHeldItemChangePacket(slot));
            mc.player.connection.sendPacket(new CPlayerTryUseItemOnBlockPacket(Hand.MAIN_HAND, target));
        }
    }
    private void sendPacketUseItem(int slot, Hand hand) {
        if (slot == -1) return;
        if (slot >= 9) {
            ItemStack stackInInv = mc.player.inventory.getStackInSlot(slot);
            ItemStack stackOnHotbar = mc.player.inventory.getStackInSlot(1);
            mc.player.inventory.setInventorySlotContents(1, stackInInv);
            mc.player.connection.sendPacket(new CHeldItemChangePacket(1));
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(hand));
            mc.player.inventory.setInventorySlotContents(1, stackOnHotbar);
        } else {
            mc.player.connection.sendPacket(new CHeldItemChangePacket(slot));
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(hand));
        }
    }
    private boolean hasRequiredItems() { return findBowSlot() != -1 && findItemSlot(Items.ARROW) != -1 && findItemSlot(Items.RAIL) != -1 && findItemSlot(Items.TNT_MINECART) != -1 && findItemSlot(Items.SHIELD) != -1; }
    private int findBowSlot() {
        PlayerInventory inv = mc.player.inventory;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.getItem() == Items.BOW && EnchantmentHelper.getEnchantmentLevel(Enchantments.FLAME, stack) > 0) return i;
        }
        return -1;
    }
    private int findItemSlot(net.minecraft.item.Item item) {
        PlayerInventory inv = mc.player.inventory;
        for (int i = 0; i < 36; i++) { if (inv.getStackInSlot(i).getItem() == item) return i; }
        return -1;
    }
    private BlockRayTraceResult getLookTarget() {
        if (mc.objectMouseOver != null && mc.objectMouseOver.getType() == RayTraceResult.Type.BLOCK) return (BlockRayTraceResult) mc.objectMouseOver;
        return null;
    }
    private void resetMacro() { isRunning = false; tickCounter = 0; if (mc.player != null) mc.player.connection.sendPacket(new CHeldItemChangePacket(originalSlot)); }
}
