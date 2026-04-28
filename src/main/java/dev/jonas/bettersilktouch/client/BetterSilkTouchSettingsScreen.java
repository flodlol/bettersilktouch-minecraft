package dev.jonas.bettersilktouch.client;

import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

public final class BetterSilkTouchSettingsScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int LIST_WIDTH = 410;
    private static final int SCROLLBAR_WIDTH = 8;
    private static final int SCROLLBAR_GAP = 2;

    private final Screen parent;
    private final List<Identifier> allBlockIds = Registries.BLOCK.getIds().stream()
        .filter(id -> !"minecraft:air".equals(id.toString()))
        .sorted(Comparator.comparing(Identifier::toString))
        .toList();
    private final List<Identifier> filteredResults = new ArrayList<>();
    private final LinkedHashSet<Identifier> protectedBlockIds = new LinkedHashSet<>();

    private TextFieldWidget blockInput;
    private String errorMessage = "";
    private String searchQuery = "";
    private int scrollOffset = 0;

    public BetterSilkTouchSettingsScreen(Screen parent) {
        super(Text.literal("Better Silk Touch"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (protectedBlockIds.isEmpty()) {
            for (String blockId : BetterSilkTouchConfig.INSTANCE.getBlockIds()) {
                Identifier id = Identifier.tryParse(blockId);
                if (id != null && Registries.BLOCK.containsId(id)) {
                    protectedBlockIds.add(id);
                }
            }
        }

        refreshFiltered();
        rebuildWidgets();
    }

    private int listX() {
        return this.width / 2 - (LIST_WIDTH / 2);
    }

    private int listY() {
        return 100;
    }

    private int listHeight() {
        return rowsPerPage() * ROW_HEIGHT;
    }

    private int rowsPerPage() {
        int availableHeight = this.height - listY() - 60;
        return Math.max(1, availableHeight / ROW_HEIGHT);
    }

    private int listBottom() {
        return listY() + listHeight();
    }

    private int maxScrollOffset() {
        return Math.max(0, filteredResults.size() - rowsPerPage());
    }

    private int rowsAreaWidth() {
        return LIST_WIDTH - SCROLLBAR_WIDTH - SCROLLBAR_GAP;
    }

    private int scrollbarX() {
        return listX() + rowsAreaWidth() + SCROLLBAR_GAP;
    }

    private void refreshFiltered() {
        filteredResults.clear();
        if (searchQuery.isBlank()) {
            filteredResults.addAll(allBlockIds);
        } else {
            String query = searchQuery.toLowerCase();
            for (Identifier id : allBlockIds) {
                Block block = Registries.BLOCK.get(id);
                String name = block.getName().getString().toLowerCase();
                String rawId = id.toString();
                if (rawId.contains(query) || name.contains(query)) {
                    filteredResults.add(id);
                }
            }
        }

        filteredResults.sort(
            Comparator.<Identifier>comparingInt(id -> protectedBlockIds.contains(id) ? 0 : 1)
                .thenComparing(id -> Registries.BLOCK.get(id).getName().getString(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Identifier::toString)
        );

        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset()));
    }

    private void rebuildWidgets() {
        clearChildren();

        int centerX = this.width / 2;
        blockInput = new TextFieldWidget(textRenderer, centerX - 205, 40, 310, 20, Text.literal("Search blocks"));
        blockInput.setMaxLength(128);
        blockInput.setText(searchQuery);
        blockInput.setChangedListener(value -> {
            searchQuery = value;
            refreshFiltered();
            rebuildWidgets();
        });
        addDrawableChild(blockInput);
        setInitialFocus(blockInput);

        List<Identifier> pageRows = filteredResults.subList(scrollOffset, Math.min(filteredResults.size(), scrollOffset + rowsPerPage()));
        for (int index = 0; index < pageRows.size(); index++) {
            Identifier id = pageRows.get(index);
            int y = listY() + (index * ROW_HEIGHT);
            addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
                toggleProtected(id);
                rebuildWidgets();
            }).dimensions(listX(), y, rowsAreaWidth(), ROW_HEIGHT).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Clear Protected"), button -> {
            protectedBlockIds.clear();
            persistProtectedBlocks();
            refreshFiltered();
            rebuildWidgets();
        }).dimensions(centerX - 205, this.height - 28, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("text.bettersilktouch.done"), button -> close())
            .dimensions(centerX + 5, this.height - 28, 200, 20)
            .build());
    }

    private void toggleProtected(Identifier id) {
        if (!protectedBlockIds.remove(id)) {
            protectedBlockIds.add(id);
        }
        errorMessage = "";
        persistProtectedBlocks();
        refreshFiltered();
    }

    private void persistProtectedBlocks() {
        BetterSilkTouchConfig.INSTANCE.replaceAll(protectedBlockIds.stream().map(Identifier::toString).toList());
        BetterSilkTouchConfig.save();
    }

    private int getScrollbarThumbHeight() {
        if (filteredResults.isEmpty()) {
            return listHeight();
        }
        float visibleRatio = (float) rowsPerPage() / (float) filteredResults.size();
        return Math.max(18, Math.min(listHeight(), (int) (listHeight() * visibleRatio)));
    }

    private int getScrollbarThumbY() {
        int maxOffset = maxScrollOffset();
        if (maxOffset == 0) {
            return listY();
        }

        int thumbHeight = getScrollbarThumbHeight();
        int travel = listHeight() - thumbHeight;
        float progress = (float) scrollOffset / (float) maxOffset;
        return listY() + (int) (travel * progress);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX < listX() || mouseX > listX() + LIST_WIDTH || mouseY < listY() || mouseY > listBottom()) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        int maxOffset = maxScrollOffset();
        if (maxOffset == 0 || verticalAmount == 0.0D) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        int delta = verticalAmount < 0.0D ? 1 : -1;
        int newOffset = Math.max(0, Math.min(scrollOffset + delta, maxOffset));
        if (newOffset != scrollOffset) {
            scrollOffset = newOffset;
            rebuildWidgets();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);

        int centerX = this.width / 2;
        context.drawCenteredTextWithShadow(textRenderer, this.title, centerX, 16, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("Protected blocks"), listX(), 78, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("Click a row to toggle protection"), listX() + 116, 78, 0xA8A8A8);

        if (!errorMessage.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(errorMessage), centerX, this.height - 52, 0xFF5555);
        }

        if (filteredResults.isEmpty()) {
            context.fill(listX(), listY(), listX() + rowsAreaWidth(), listY() + 24, 0x66222222);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("No matching blocks"), centerX, listY() + 8, 0xCFCFCF);
        } else {
            List<Identifier> pageRows = filteredResults.subList(scrollOffset, Math.min(filteredResults.size(), scrollOffset + rowsPerPage()));
            for (int index = 0; index < pageRows.size(); index++) {
                Identifier id = pageRows.get(index);
                Block block = Registries.BLOCK.get(id);
                ItemStack stack = new ItemStack(block);
                boolean isProtected = protectedBlockIds.contains(id);
                int y = listY() + (index * ROW_HEIGHT);
                int background = isProtected ? 0xAA235C2E : 0x88303030;

                context.fill(listX(), y, listX() + rowsAreaWidth(), y + ROW_HEIGHT - 1, background);
                if (!stack.isEmpty()) {
                    context.drawItem(stack, listX() + 4, y + 3);
                }

                context.drawTextWithShadow(textRenderer, block.getName(), listX() + 24, y + 4, 0xFFFFFFFF);
                context.drawTextWithShadow(textRenderer, Text.literal(id.toString()), listX() + 24, y + 14, 0xFFA8A8A8);
                context.drawText(
                    textRenderer,
                    Text.literal(isProtected ? "PROTECTED" : "AVAILABLE"),
                    listX() + rowsAreaWidth() - 72,
                    y + 8,
                    isProtected ? 0xFFA7FFA7 : 0xFFCFCFCF,
                    false
                );
            }
        }

        context.fill(scrollbarX(), listY(), scrollbarX() + SCROLLBAR_WIDTH, listBottom(), 0x6A111111);
        int thumbHeight = getScrollbarThumbHeight();
        int thumbY = getScrollbarThumbY();
        boolean hoveringThumb = mouseX >= scrollbarX() && mouseX <= scrollbarX() + SCROLLBAR_WIDTH
            && mouseY >= thumbY && mouseY <= thumbY + thumbHeight;
        int thumbColor = hoveringThumb ? 0xC8CCCCCC : 0xA8A0A0A0;
        context.fill(scrollbarX() + 1, thumbY + 1, scrollbarX() + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight - 1, thumbColor);

        context.drawCenteredTextWithShadow(
            textRenderer,
            Text.literal("Protected: " + protectedBlockIds.size()),
            centerX,
            this.height - 42,
            0xFFB0F0B0
        );
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}
