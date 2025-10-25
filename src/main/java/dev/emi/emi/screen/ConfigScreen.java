package dev.emi.emi.screen;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.rewindmc.retroemi.REMIScreen;
import com.rewindmc.retroemi.RetroEMI;
import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.api.render.EmiTooltipComponents;
import dev.emi.emi.com.unascribed.qdcss.QDCSS;
import dev.emi.emi.config.*;
import dev.emi.emi.config.EmiConfig.Comment;
import dev.emi.emi.config.EmiConfig.ConfigGroup;
import dev.emi.emi.config.EmiConfig.ConfigGroupEnd;
import dev.emi.emi.config.EmiConfig.ConfigValue;
import dev.emi.emi.input.EmiBind;
import dev.emi.emi.input.EmiBind.ModifiedKey;
import dev.emi.emi.input.EmiInput;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.screen.widget.SizedButtonWidget;
import dev.emi.emi.screen.widget.config.*;
import dev.emi.emi.search.EmiSearch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ConfigScreen extends REMIScreen {
    private static final int maxWidth = 240;
    private GuiScreen last;
    private ConfigSearch search;
    public ListWidget list;
    public EmiBind activeBind;
    public int activeBindOffset;
    public int activeModifiers;
    public int lastModifier;
    public String originalConfig;
    public ButtonWidget resetButton;

    public ConfigScreen(GuiScreen last) {
        super(EmiPort.translatable("screen.emi.config"));
        this.last = last;
        originalConfig = EmiConfig.getSavedConfig();
    }

    public void setActiveBind(EmiBind bind, int offset) {
        activeBind = bind;
        activeBindOffset = offset;
        activeModifiers = 0;
        lastModifier = 0;
    }

    @Override
    public void close() {
        EmiConfig.writeConfig();
        EmiSearch.update();
        Minecraft.getMinecraft().displayGuiScreen(last);
    }

    @SuppressWarnings("unchecked")
    public static List<TooltipComponent> getFieldTooltip(Field field) {
        List<TooltipComponent> text;
        ConfigValue annot = field.getAnnotation(ConfigValue.class);
        String key = "config.emi.tooltip." + annot.value().replace('-', '_');
        Comment comment = field.getAnnotation(Comment.class);
        if (I18n.hasKey(key)) {
            text = Arrays.stream(RetroEMI.translate(key).split("\n"))
                    .map(EmiPort::literal).map(EmiTooltipComponents::of).collect(java.util.stream.Collectors.toList());
        } else if (comment != null) {
            text = Arrays.stream(comment.value().split("\n"))
                    .map(EmiPort::literal).map(EmiTooltipComponents::of).collect(java.util.stream.Collectors.toList());
        } else {
            text = null;
        }
        if (text == null) {
            return com.rewindmc.retroemi.shim.java.List.of();
        }
        return text;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void init() {
        super.init();

        // Persistent-ish state
        int scroll = 0;
        String query = "";
        Set<String> collapsed = Sets.newHashSet();
        if (list != null) {
            scroll = (int) list.getScrollAmount();
            query = search.getSearch();
            for (ListWidget.Entry e : list.children()) {
                if (e instanceof GroupNameWidget g) {
                    if (g.collapsed) {
                        collapsed.add(g.text.getString());
                    }
                }
            }
        }

        list = new ListWidget(client, width, height, 40, height - 60);
        this.addDrawable(new EmiNameWidget(width / 2, 16));
        int w = Math.min(400, width - 40) / 4 * 4;
        int x = (width - w) / 2;
        search = new ConfigSearch(x + 3, height - 51, w / 2 - 4, 18);
        this.addDrawable(search.field);
        this.resetButton = EmiPort.newButton(x + 2, height - 30, w / 2 - 2, 20, EmiPort.translatable("gui.done"), button -> {
            EmiConfig.loadConfig(QDCSS.load("revert", originalConfig));
            Minecraft client = Minecraft.getMinecraft();
            setWorldAndResolution(client, width, height);
        });
        this.addDrawableChild(EmiPort.newButton(x + w / 2 + 2, height - 30, w / 2 - 2, 20, EmiPort.translatable("gui.done"), button -> {
            this.close();
        }));
        this.addDrawableChild(EmiPort.newButton(x + w / 2 + 2, height - 52, w / 2 - 24, 20, EmiPort.translatable("screen.emi.presets"), button -> {
            Minecraft client = Minecraft.getMinecraft();
            client.displayGuiScreen(new ConfigPresetScreen(this));
        }));
        this.addDrawableChild(new SizedButtonWidget(x + w - 20, height - 52, 20, 20, 164, 0, () -> true, widget -> {
            EmiConfig.setGlobalState(!EmiConfig.useGlobalConfig);
            ConfigScreen.this.setWorldAndResolution(client, width, height);
        }, () -> (EmiConfig.useGlobalConfig ? 40 : 0), () -> {
            return (List<Text>) (Object) Arrays.stream(RetroEMI.translate("tooltip.emi.config.global").split("\n"))
                    .flatMap(s -> client.fontRenderer.listFormattedStringToWidth(s, maxWidth).stream().map(str -> Text.literal((String) str)))
                    .collect(Collectors.toList());
        }));
        this.addDrawableChild(resetButton);
        this.addSelectableChild(search.field);

        try {
            String lastGroup = "";
            GroupNameWidget lastGroupWidget = null;
            ConfigGroup currentGroup = null;
            SubGroupNameWidget currentSubGroupWidget = null;
            Supplier<String> searchSupplier = () -> search.getSearch();
            for (Field field : EmiConfig.class.getFields()) {
                ConfigValue annot = field.getAnnotation(ConfigValue.class);
                if (annot != null) {
                    String group = annot.value().split("\\.")[0];
                    if (group.equals("persistent")) {
                        continue;
                    }
                    if (!group.equals(lastGroup)) {
                        lastGroup = group;
                        Text text = EmiPort.translatable("config.emi.group." + group.replace('-', '_'));
                        lastGroupWidget = new GroupNameWidget(group, text);
                        if (collapsed.contains(text.getString())) {
                            lastGroupWidget.collapsed = true;
                        }
                        list.addEntry(lastGroupWidget);
                    }
                    ConfigGroup configGroup = field.getAnnotation(ConfigGroup.class);
                    if (configGroup != null) {
                        currentGroup = configGroup;
                        Text text = EmiPort.translatable("config.emi.group." + configGroup.value().replace('-', '_'));
                        currentSubGroupWidget = new SubGroupNameWidget(configGroup.value(), text);
                        if (collapsed.contains(text.getString())) {
                            currentSubGroupWidget.collapsed = true;
                        }
                        currentSubGroupWidget.parent = lastGroupWidget;
                        list.addEntry(currentSubGroupWidget);
                    }
                    Predicate<?> predicate = EmiConfig.FILTERS.getOrDefault(annot.value(), v -> true);
                    Text translation = EmiPort.translatable("config.emi." + annot.value().replace('-', '_'));
                    ConfigEntryWidget entry = null;
                    if (field.getType() == boolean.class) {
                        entry = new BooleanWidget(translation, getFieldTooltip(field), searchSupplier, new Mutator<Boolean>() {

                            public Boolean getValue() {
                                try {
                                    return field.getBoolean(null);
                                } catch (Exception e) {
                                }
                                return false;
                            }

                            public void setValue(Boolean value) {
                                try {
                                    field.setBoolean(null, value);
                                } catch (Exception e) {
                                }
                            }
                        });
                    } else if (field.getType() == int.class) {
                        entry = new IntWidget(translation, getFieldTooltip(field), searchSupplier, new Mutator<Integer>() {

                            public Integer getValue() {
                                try {
                                    return field.getInt(null);
                                } catch (Exception e) {
                                }
                                return -1;
                            }

                            public void setValue(Integer value) {
                                try {
                                    field.setInt(null, value);
                                } catch (Exception e) {
                                }
                            }
                        });
                    } else if (field.getType() == EmiBind.class) {
                        entry = new EmiBindWidget(this, getFieldTooltip(field), searchSupplier, (EmiBind) field.get(null));
                    } else if (field.getType() == ScreenAlign.class) {
                        entry = new ScreenAlignWidget(translation, getFieldTooltip(field), searchSupplier, objectMutator(field));
                    } else if (field.getType() == SidebarPages.class) {
                        entry = new SidebarPagesWidget(translation, getFieldTooltip(field), searchSupplier, objectMutator(field));
                    } else if (field.getType() == SidebarSubpanels.class) {
                        entry = new SidebarSubpanelsWidget(translation, getFieldTooltip(field), searchSupplier, objectMutator(field));
                    } else if (IntGroup.class.isAssignableFrom(field.getType())) {
                        entry = new IntGroupWidget(translation, getFieldTooltip(field), searchSupplier, objectMutator(field));
                    } else if (ConfigEnum.class.isAssignableFrom(field.getType())) {
                        entry = new EnumWidget(translation, getFieldTooltip(field), searchSupplier, objectMutator(field), (Predicate<ConfigEnum>) predicate);
                    }
                    boolean endGroup = field.getAnnotation(ConfigGroupEnd.class) != null;
                    if (entry != null) {
                        entry.group = currentGroup;
                        entry.endGroup = endGroup;
                        list.addEntry(entry);
                        if (lastGroupWidget != null) {
                            lastGroupWidget.children.add(entry);
                            entry.parentGroups.add(lastGroupWidget);
                        }
                        if (currentSubGroupWidget != null) {
                            currentSubGroupWidget.children.add(entry);
                            entry.parentGroups.add(currentSubGroupWidget);
                        }
                    }
                    if (endGroup) {
                        currentGroup = null;
                        currentSubGroupWidget = null;
                    }
                }
            }
        } catch (Exception e) {
            EmiLog.error("Error initializing config screen", e);
        }

        this.addSelectableChild(list);
        list.setScrollAmount(scroll);
        search.setText(query);
        addJumpButtons();
        updateChanges();
    }

    private void addJumpButtons() {
        List<String> jumps = Lists.newArrayList(
                "general", "general.search",
                "ui", "ui.left-sidebar", "ui.right-sidebar", "ui.top-sidebar", "ui.bottom-sidebar",
                "binds", "binds.crafts", "binds.cheats",
                "dev"
        );
        List<List<String>> removes = com.rewindmc.retroemi.shim.java.List.of(
                com.rewindmc.retroemi.shim.java.List.of("binds.cheats"),
                com.rewindmc.retroemi.shim.java.List.of("general.search"),
                com.rewindmc.retroemi.shim.java.List.of("ui.top-sidebar", "ui.bottom-sidebar"),
                com.rewindmc.retroemi.shim.java.List.of("binds.crafts"),
                com.rewindmc.retroemi.shim.java.List.of("ui.left-sidebar", "ui.right-sidebar")
        );
        int space = list.getLogicalHeight() - 10;
        for (List<String> r : removes) {
            if (jumps.size() * 16 > space) {
                jumps.removeAll(r);
            }
        }
        int y = 40 + (list.getLogicalHeight() - jumps.size() * 16) / 2;
        int u = 0, v = -16;
        for (String s : jumps) {
            boolean newGroup = !s.contains(".");
            if (newGroup) {
                v += 16;
                u = 0;
            } else {
                u += 16;
            }
            this.addDrawableChild(new ConfigJumpButton(
                    2 + (newGroup ? 0 : 8), y, u, v, w -> jump(s),
                    com.rewindmc.retroemi.shim.java.List.of(EmiPort.translatable("config.emi.group." + s.replace('-', '_')))));
            y += 16;
        }
    }

    public void jump(String jump) {
        for (ListWidget.Entry e : list.children()) {
            if (e instanceof ConfigEntryWidget c) {
                for (GroupNameWidget p : c.parentGroups) {
                    if (p.id.equals(jump)) {
                        list.centerScrollOn(e);
                        return;
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Mutator<T> objectMutator(Field field) {
        return new Mutator<T>() {
            public T getValue() {
                try {
                    return (T) field.get(null);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            public void setValue(T en) {
                try {
                    field.set(null, en);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public void updateChanges() {
        // Split on the blank lines between config options
        String[] oLines = originalConfig.split("\n\n");
        String[] cLines = EmiConfig.getSavedConfig().split("\n\n");
        int different = 0;
        for (int i = 0; i < oLines.length; i++) {
            if (i >= cLines.length) {
                break;
            }
            if (!oLines[i].equals(cLines[i])) {
                different++;
            }
        }
        this.resetButton.active = different > 0;
        this.resetButton.setMessage(EmiPort.translatable("screen.emi.config.reset", different));
    }

    @Override
    public void render(DrawContext raw, int mouseX, int mouseY, float delta) {
        EmiDrawContext context = EmiDrawContext.wrap(raw);
        list.setScrollAmount(list.getScrollAmount());
        this.renderBackgroundTexture(context.raw());
        list.render(context.raw(), mouseX, mouseY, delta);
        super.render(context.raw(), mouseX, mouseY, delta);
        if (list.getHoveredEntry() != null) {
            EmiRenderHelper.drawTooltip(this, context, list.getHoveredEntry().getTooltip(mouseX, mouseY), mouseX, mouseY, Math.min(width / 2 - 16, maxWidth));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (activeBind != null) {
            pushModifier(0);
            activeBind.setBind(activeBindOffset, new ModifiedKey(InputUtil.Type.MOUSE.createFromCode(button), activeModifiers));
            activeBind = null;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void pushModifier(int lastModifier) {
        activeModifiers |= EmiInput.maskFromCode(this.lastModifier);
        this.lastModifier = lastModifier;
        activeModifiers &= ~EmiInput.maskFromCode(lastModifier);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (activeBind != null) {
            if (EmiInput.maskFromCode(keyCode) != 0) {
                pushModifier(keyCode);
            } else {
                pushModifier(0);
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    activeBind.setBind(activeBindOffset, new ModifiedKey(InputUtil.UNKNOWN_KEY, 0));
                } else {
                    activeBind.setBind(activeBindOffset, new ModifiedKey(InputUtil.Type.KEYSYM.createFromCode(keyCode), activeModifiers));
                }
                activeBind = null;
                updateChanges();
            }
            return true;
        } else {
            // Element nesting causes crashing for cycling, for some reason
            if (keyCode == GLFW.GLFW_KEY_TAB) {
                return false;
            }
            if (super.keyPressed(keyCode, scanCode, modifiers)) {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
            if (this.getFocused() instanceof TextFieldWidget tfw && tfw.isFocused()) {
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    EmiPort.focus(tfw, false);
                    return true;
                }
            } else {
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    this.close();
                    return true;
                } else if (this.client.gameSettings.keyBindInventory.getKeyCode() == (keyCode)) {
                    this.close();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (activeBind != null) {
            activeModifiers &= ~EmiInput.maskFromCode(keyCode);
            if (keyCode == lastModifier) {
                activeBind.setBind(activeBindOffset, new ModifiedKey(InputUtil.Type.KEYSYM.createFromCode(keyCode), activeModifiers));
                activeBind = null;
            }
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    public abstract class Mutator<T> {
        protected abstract T getValue();

        protected abstract void setValue(T value);

        public T get() {
            return getValue();
        }

        public void set(T value) {
            setValue(value);
            updateChanges();
        }
    }
}
