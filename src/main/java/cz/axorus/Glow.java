package cz.axorus;

import net.minecraft.client.gui.DrawContext;

public class Glow {
    /** Volitelný glow (průhledné vrstvy okolo) a volitelný pevný 1px okraj. */
    public static void box(DrawContext ctx, int x1, int y1, int x2, int y2, int color, boolean border, boolean glow) {
        int rgb = color & 0xFFFFFF;
        if (glow) {
            int[] alphas = {0x14, 0x24, 0x3C, 0x66};   // zvenku dovnitř
            for (int i = 0; i < alphas.length; i++) {
                int d = alphas.length - i;
                frame(ctx, x1 - d, y1 - d, x2 + d, y2 + d, (alphas[i] << 24) | rgb);
            }
        }
        if (border) frame(ctx, x1, y1, x2, y2, 0xFF000000 | rgb);
    }

    private static void frame(DrawContext ctx, int x1, int y1, int x2, int y2, int argb) {
        ctx.fill(x1, y1, x2, y1 + 1, argb);
        ctx.fill(x1, y2 - 1, x2, y2, argb);
        ctx.fill(x1, y1 + 1, x1 + 1, y2 - 1, argb);
        ctx.fill(x2 - 1, y1 + 1, x2, y2 - 1, argb);
    }
}
