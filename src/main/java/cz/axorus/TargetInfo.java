package cz.axorus;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;

public class TargetInfo {
    private static PlayerEntity target;
    private static long lastHit;

    public static void setTarget(PlayerEntity p) {
        target = p;
        lastHit = System.currentTimeMillis();
    }

    public static void render(DrawContext ctx, MinecraftClient mc) {
        Module hud = Modules.TARGET_HUD;
        if (!hud.enabled || target == null || mc.player == null) return;
        long showMs = Long.parseLong(hud.setting("Duration").get().replace("s", "")) * 1000L;
        if (System.currentTimeMillis() - lastHit > showMs || target.isRemoved()) { target = null; return; }

        TextRenderer font = mc.textRenderer;
        float scale = Math.max(50, hud.setting("Scale").index) / 100f;
        int alpha = hud.setting("Opacity").index * 255 / 100;
        int w = 175, h = 78;
        boolean right = hud.setting("Position").get().equals("Right");
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        int sx = right ? sw / 2 + 20 : sw / 2 - 20 - Math.round(w * scale);
        int sy = sh / 2 - 30;

        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(sx, sy);
        m.scale(scale, scale);
        int x = 0, y = 0;   // od teď kreslíme v lokálních souřadnicích

        int edge = Themes.edge();
        ctx.fill(x, y, x + w, y + h, (alpha << 24) | (Themes.main() & 0xFFFFFF));
        ctx.fill(x, y, x + w, y + 2, edge);
        Glow.box(ctx, x, y, x + w, y + h, edge, Themes.border(), Themes.glow());

        int tx = x + 6;
        if (hud.setting("Skin").isOn()) {
            PlayerListEntry entry = mc.getNetworkHandler() == null ? null
                    : mc.getNetworkHandler().getPlayerListEntry(target.getUuid());
            if (entry != null) PlayerSkinDrawer.draw(ctx, entry.getSkinTextures(), x + 6, y + 8, 32);
            tx = x + 44;
        }

        String name = target.getName().getString();
        ctx.drawText(font, name, tx, y + 7, 0xFFFFFFFF, true);

        if (hud.setting("HP").isOn()) {
            float hp = target.getHealth() + target.getAbsorptionAmount();
            float max = Math.max(1f, target.getMaxHealth());
            ctx.drawText(font, String.format("HP: %.1f / %.0f", hp, max), tx, y + 19, 0xFFFF5555, true);
            int bw = w - (tx - x) - 6;
            ctx.fill(tx, y + 31, tx + bw, y + 35, 0xFF333340);
            ctx.fill(tx, y + 31, tx + (int) (bw * Math.min(1f, hp / max)), y + 35, 0xFF55FF55);
        }

        if (hud.setting("Tier").isOn()) {
            ctx.drawText(font, "FlowPvP: " + TierFetcher.get(name), tx, y + 40, 0xFFFFD35A, true);
        }

        if (hud.setting("Gear").isOn()) {
            // Klient zná jen to, co server pošle: ruce + brnění (ne celý inventář).
            ItemStack[] items = {
                    target.getEquippedStack(EquipmentSlot.HEAD),
                    target.getEquippedStack(EquipmentSlot.CHEST),
                    target.getEquippedStack(EquipmentSlot.LEGS),
                    target.getEquippedStack(EquipmentSlot.FEET),
                    target.getMainHandStack(),
                    target.getOffHandStack()
            };
            int ix = x + 6, iy = y + 56;
            for (ItemStack s : items) {
                if (!s.isEmpty()) {
                    ctx.drawItem(s, ix, iy);
                    ctx.drawStackOverlay(font, s, ix, iy);
                }
                ix += 18;
            }
            boolean totem = target.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)
                    || target.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING);
            boolean pot = target.getMainHandStack().getItem() instanceof PotionItem
                    || target.getOffHandStack().getItem() instanceof PotionItem;
            ctx.drawText(font, (totem ? "Totem " : "") + (pot ? "Pot" : ""), ix + 4, iy + 5, 0xFFAAAAAA, true);
        }

        m.popMatrix();
    }
}
