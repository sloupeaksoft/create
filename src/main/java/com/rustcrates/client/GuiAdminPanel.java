package com.rustcrates.client;

import com.rustcrates.CrateTier;
import com.rustcrates.network.MsgPanelAction;
import com.rustcrates.network.MsgPanelData;
import com.rustcrates.network.PacketHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import org.lwjgl.input.Mouse;

import java.io.IOException;

/** Панель управления ADMIN-ящика (Shift + ПКМ). */
public class GuiAdminPanel extends GuiScreen {
    private static final int W = 340, H = 232, ROWS = 6, ROW_H = 22;
    private static final int[] CHANCE_DELTA = {-10, -1, 1, 10};
    private static final String[] CHANCE_LABEL = {"-10", "-1", "+1", "+10"};
    private static final int[] INTERVAL_DELTA = {-60, -10, 10, 60};
    private static final String[] INTERVAL_LABEL = {"-60", "-10", "+10", "+60"};

    private MsgPanelData data;
    private int tab = 0;
    private int scroll = 0;
    private int left, top;
    private int refreshTicks = 0;

    public GuiAdminPanel(MsgPanelData data) {
        this.data = data;
    }

    public int tier() {
        return data.tier;
    }

    public void setData(MsgPanelData d) {
        this.data = d;
        clampScroll();
        rebuild();
    }

    @Override
    public void initGui() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        rebuild();
    }

    private int listSize() {
        return tab == 0 ? data.loot.size() : tab == 1 ? data.crates.size() : 0;
    }

    private int maxScroll() {
        return Math.max(0, listSize() - ROWS);
    }

    private void clampScroll() {
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    private int rowY(int r) {
        return top + 66 + r * ROW_H;
    }

    private static String yesNo(boolean b) {
        return b ? "§aДа" : "§cНет";
    }

    private static String time(int s) {
        if (s >= 60) return (s / 60) + " мин " + (s % 60) + " сек";
        return s + " сек";
    }

    private void rebuild() {
        buttonList.clear();
        GuiButton t0 = new GuiButton(0, left + 10, top + 18, 104, 20, "Лут (" + data.loot.size() + ")");
        GuiButton t1 = new GuiButton(1, left + 118, top + 18, 104, 20, "Ящики (" + data.crates.size() + ")");
        GuiButton t2 = new GuiButton(2, left + 226, top + 18, 104, 20, "Настройки");
        t0.enabled = tab != 0;
        t1.enabled = tab != 1;
        t2.enabled = tab != 2;
        buttonList.add(t0);
        buttonList.add(t1);
        buttonList.add(t2);
        buttonList.add(new GuiButton(3, left + W - 70, top + H - 26, 60, 20, "Закрыть"));

        if (tab == 0) {
            buttonList.add(new GuiButton(10, left + 10, top + 42, 140, 20, "Изменить предметы"));
            for (int r = 0; r < ROWS; r++) {
                int idx = scroll + r;
                if (idx >= data.loot.size()) break;
                int bx = left + W - 10 - 4 * 24;
                for (int k = 0; k < 4; k++) {
                    buttonList.add(new GuiButton(100 + r * 10 + k, bx + k * 24, rowY(r), 22, 20, CHANCE_LABEL[k]));
                }
            }
        } else if (tab == 1) {
            buttonList.add(new GuiButton(20, left + 10, top + 42, 100, 20, "Выбрать все"));
            buttonList.add(new GuiButton(21, left + 114, top + 42, 100, 20, "Снять все"));
            for (int r = 0; r < ROWS; r++) {
                int idx = scroll + r;
                if (idx >= data.crates.size()) break;
                MsgPanelData.CrateInfo ci = data.crates.get(idx);
                buttonList.add(new GuiButton(100 + r * 10, left + W - 10 - 34 - 84, rowY(r), 80, 20,
                        ci.selected ? "§aСпавн: Да" : "§cСпавн: Нет"));
                buttonList.add(new GuiButton(100 + r * 10 + 1, left + W - 10 - 30, rowY(r), 30, 20, "ТП"));
            }
        } else {
            buttonList.add(new GuiButton(30, left + 10, top + 42, 200, 20, "Автоспавн: " + (data.enabled ? "§aВкл" : "§cВыкл")));
            for (int k = 0; k < 4; k++) {
                buttonList.add(new GuiButton(31 + k, left + W - 10 - 4 * 30 + k * 30, top + 66, 28, 20, INTERVAL_LABEL[k]));
            }
            buttonList.add(new GuiButton(35, left + 10, top + 90, 240, 20, "Очищать ящик перед спавном: " + yesNo(data.clearBefore)));
            buttonList.add(new GuiButton(36, left + 10, top + 114, 240, 20, "Случайное кол-во (от 1 до N): " + yesNo(data.randomCount)));
            buttonList.add(new GuiButton(37, left + W - 10 - 2 * 30, top + 138, 28, 20, "-1"));
            buttonList.add(new GuiButton(38, left + W - 10 - 30, top + 138, 28, 20, "+1"));
            buttonList.add(new GuiButton(39, left + 10, top + 164, 200, 20, "§eЗаспавнить лут сейчас"));
        }

        if (tab < 2 && maxScroll() > 0) {
            GuiButton up = new GuiButton(4, left + 10, top + H - 26, 50, 20, "Вверх");
            GuiButton down = new GuiButton(5, left + 64, top + H - 26, 50, 20, "Вниз");
            up.enabled = scroll > 0;
            down.enabled = scroll < maxScroll();
            buttonList.add(up);
            buttonList.add(down);
        }
    }

    private void send(int action, int a, int b, long c) {
        PacketHandler.NET.sendToServer(new MsgPanelAction(data.tier, action, a, b, c));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        int id = button.id;
        if (id <= 2) {
            tab = id;
            scroll = 0;
            rebuild();
            return;
        }
        switch (id) {
            case 3: mc.displayGuiScreen(null); return;
            case 4: scroll--; clampScroll(); rebuild(); return;
            case 5: scroll++; clampScroll(); rebuild(); return;
            case 10: send(MsgPanelAction.OPEN_POOL, 0, 0, 0); return;
            case 20: send(MsgPanelAction.SELECT_ALL, 0, 0, 0); return;
            case 21: send(MsgPanelAction.DESELECT_ALL, 0, 0, 0); return;
            case 30: send(MsgPanelAction.TOGGLE_ENABLED, 0, 0, 0); return;
            case 31: case 32: case 33: case 34:
                send(MsgPanelAction.SET_INTERVAL, 0, data.interval + INTERVAL_DELTA[id - 31], 0); return;
            case 35: send(MsgPanelAction.TOGGLE_CLEAR, 0, 0, 0); return;
            case 36: send(MsgPanelAction.TOGGLE_RANDOM, 0, 0, 0); return;
            case 37: send(MsgPanelAction.SET_MAX, 0, data.maxItems - 1, 0); return;
            case 38: send(MsgPanelAction.SET_MAX, 0, data.maxItems + 1, 0); return;
            case 39: send(MsgPanelAction.SPAWN_NOW, 0, 0, 0); return;
            default: break;
        }
        if (id >= 100) {
            int r = (id - 100) / 10;
            int k = (id - 100) % 10;
            int idx = scroll + r;
            if (tab == 0 && idx < data.loot.size()) {
                MsgPanelData.LootInfo li = data.loot.get(idx);
                li.chance = Math.max(0, Math.min(100, li.chance + CHANCE_DELTA[k]));
                send(MsgPanelAction.SET_CHANCE, li.slot, li.chance, 0);
            } else if (tab == 1 && idx < data.crates.size()) {
                MsgPanelData.CrateInfo ci = data.crates.get(idx);
                if (k == 0) {
                    send(MsgPanelAction.TOGGLE_CRATE, ci.dim, 0, ci.pos.toLong());
                } else {
                    send(MsgPanelAction.TELEPORT, ci.dim, 0, ci.pos.toLong());
                    mc.displayGuiScreen(null);
                }
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dw = Mouse.getEventDWheel();
        if (dw != 0 && tab < 2) {
            scroll += dw > 0 ? -1 : 1;
            clampScroll();
            rebuild();
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (++refreshTicks % 40 == 0) send(MsgPanelAction.REFRESH, 0, 0, 0);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawRect(left, top, left + W, top + H, 0xE0101010);
        drawRect(left, top, left + W, top + 1, 0xFFCC0000);
        drawRect(left, top + H - 1, left + W, top + H, 0xFFCC0000);
        drawRect(left, top, left + 1, top + H, 0xFFCC0000);
        drawRect(left + W - 1, top, left + W, top + H, 0xFFCC0000);
        CrateTier t = CrateTier.byId(data.tier);
        drawCenteredString(fontRenderer, "§c§lADMIN§r  §f" + t.title + " — панель управления", left + W / 2, top + 6, 0xFFFFFF);

        if (tab == 0) {
            drawString(fontRenderer, "§7Шанс выпадения каждого предмета", left + 158, top + 48, 0xFFFFFF);
            if (data.loot.isEmpty()) {
                drawString(fontRenderer, "Пул лута пуст.", left + 12, rowY(0) + 4, 0xFFFFFF);
                drawString(fontRenderer, "§7Нажмите «Изменить предметы» и положите туда", left + 12, rowY(0) + 18, 0xFFFFFF);
                drawString(fontRenderer, "§7предметы, которые могут выпасть из ящиков.", left + 12, rowY(0) + 30, 0xFFFFFF);
            }
            for (int r = 0; r < ROWS; r++) {
                int idx = scroll + r;
                if (idx >= data.loot.size()) break;
                MsgPanelData.LootInfo li = data.loot.get(idx);
                int y = rowY(r);
                drawRect(left + 8, y - 1, left + W - 8, y + 21, r % 2 == 0 ? 0x30FFFFFF : 0x18FFFFFF);
                String name = fontRenderer.trimStringToWidth(li.stack.getDisplayName(), 140);
                drawString(fontRenderer, name + " §7x" + li.stack.getCount(), left + 32, y + 6, 0xFFFFFF);
                drawString(fontRenderer, "§e" + li.chance + "%", left + W - 10 - 4 * 24 - 34, y + 6, 0xFFFFFF);
            }
        } else if (tab == 1) {
            int sel = 0;
            for (MsgPanelData.CrateInfo ci : data.crates) if (ci.selected) sel++;
            drawString(fontRenderer, "§7Выбрано: §f" + sel + " / " + data.crates.size(), left + 222, top + 48, 0xFFFFFF);
            if (data.crates.isEmpty()) {
                drawString(fontRenderer, "Нет поставленных ящиков этого типа.", left + 12, rowY(0) + 4, 0xFFFFFF);
                drawString(fontRenderer, "§7Поставьте обычные (не ADMIN) ящики в мире —", left + 12, rowY(0) + 18, 0xFFFFFF);
                drawString(fontRenderer, "§7они появятся здесь автоматически.", left + 12, rowY(0) + 30, 0xFFFFFF);
            }
            for (int r = 0; r < ROWS; r++) {
                int idx = scroll + r;
                if (idx >= data.crates.size()) break;
                MsgPanelData.CrateInfo ci = data.crates.get(idx);
                int y = rowY(r);
                drawRect(left + 8, y - 1, left + W - 8, y + 21, r % 2 == 0 ? 0x30FFFFFF : 0x18FFFFFF);
                drawString(fontRenderer, "§f#" + (idx + 1) + "  §bX: " + ci.pos.getX() + "  Y: " + ci.pos.getY() + "  Z: " + ci.pos.getZ(), left + 12, y + 1, 0xFFFFFF);
                drawString(fontRenderer, "§7Измерение " + ci.dim + (ci.loaded ? "" : "  §8(чанк не загружен)"), left + 12, y + 11, 0xFFFFFF);
            }
        } else {
            drawString(fontRenderer, "Интервал спавна: §e" + time(data.interval), left + 12, top + 72, 0xFFFFFF);
            drawString(fontRenderer, "Макс. предметов в ящике: §e" + (data.maxItems == 0 ? "без лимита" : String.valueOf(data.maxItems)), left + 12, top + 144, 0xFFFFFF);
            drawString(fontRenderer, "До следующего спавна: " + (data.enabled ? "§e" + time(data.secondsLeft) : "§cавтоспавн выключен"), left + 12, top + 192, 0xFFFFFF);
        }

        if (tab < 2 && maxScroll() > 0) {
            int from = scroll + 1;
            int to = Math.min(listSize(), scroll + ROWS);
            drawString(fontRenderer, "§7" + from + "–" + to + " из " + listSize(), left + 122, top + H - 20, 0xFFFFFF);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (tab == 0) {
            RenderHelper.enableGUIStandardItemLighting();
            for (int r = 0; r < ROWS; r++) {
                int idx = scroll + r;
                if (idx >= data.loot.size()) break;
                itemRender.renderItemAndEffectIntoGUI(data.loot.get(idx).stack, left + 12, rowY(r) + 2);
            }
            RenderHelper.disableStandardItemLighting();
            for (int r = 0; r < ROWS; r++) {
                int idx = scroll + r;
                if (idx >= data.loot.size()) break;
                int x = left + 12, y = rowY(r) + 2;
                if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                    renderToolTip(data.loot.get(idx).stack, mouseX, mouseY);
                }
            }
        }
    }
}
