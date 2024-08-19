package io.papermc.paper.adventure;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.KeybindComponent;
import net.kyori.adventure.text.ScoreComponent;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.ScoreContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.intellij.lang.annotations.Subst;

import static java.util.Objects.requireNonNull;
import static net.kyori.adventure.text.Component.text;
import static net.minecraft.util.ExtraCodecs.strictOptionalField;

@DefaultQualifier(NonNull.class)
public final class AdventureCodecs {

    public static final Codec<Component> COMPONENT_CODEC = ExtraCodecs.recursive("adventure Component", AdventureCodecs::createCodec);

    private static final Codec<TextColor> TEXT_COLOR_CODEC = Codec.STRING.comapFlatMap(s -> {
        if (s.startsWith("#")) {
            @Nullable TextColor value = TextColor.fromHexString(s);
            return value != null ? DataResult.success(value) : DataResult.error(() -> "Cannot convert " + s + " to adventure TextColor");
        } else {
            final @Nullable NamedTextColor value = NamedTextColor.NAMES.value(s);
            return value != null ? DataResult.success(value) : DataResult.error(() -> "Cannot convert " + s + " to adventure NamedTextColor");
        }
    }, textColor -> {
        if (textColor instanceof NamedTextColor named) {
            return NamedTextColor.NAMES.keyOrThrow(named);
        } else {
            return textColor.asHexString();
        }
    });

    private static final Codec<Key> KEY_CODEC = Codec.STRING.comapFlatMap(s -> {
        return Key.parseable(s) ? DataResult.success(Key.key(s)) : DataResult.error(() -> "Cannot convert " + s + " to adventure Key");
    }, Key::asString);

    private static final Codec<ClickEvent.Action> CLICK_EVENT_ACTION_CODEC = Codec.STRING.comapFlatMap(s -> {
        final ClickEvent.@Nullable Action value = ClickEvent.Action.NAMES.value(s);
        return value != null ? DataResult.success(value) : DataResult.error(() -> "Cannot convert " + s + " to adventure ClickEvent$Action");
    }, ClickEvent.Action.NAMES::keyOrThrow);
    private static final Codec<ClickEvent> CLICK_EVENT_CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(
            CLICK_EVENT_ACTION_CODEC.fieldOf("action").forGetter(ClickEvent::action),
            Codec.STRING.fieldOf("value").forGetter(ClickEvent::value)
        ).apply(instance, ClickEvent::clickEvent);
    });

    private static final Codec<HoverEvent.ShowEntity> SHOW_ENTITY_CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(
            KEY_CODEC.fieldOf("type").forGetter(HoverEvent.ShowEntity::type),
            UUIDUtil.LENIENT_CODEC.fieldOf("id").forGetter(HoverEvent.ShowEntity::id),
            strictOptionalField(COMPONENT_CODEC, "name").forGetter(he -> Optional.ofNullable(he.name()))
        ).apply(instance, (key, uuid, component) -> {
            return HoverEvent.ShowEntity.showEntity(key, uuid, component.orElse(null));
        });
    });

    private static final Codec<HoverEvent.ShowItem> SHOW_ITEM_CODEC = net.minecraft.network.chat.HoverEvent.ItemStackInfo.CODEC.xmap(isi -> {
        @Subst("key") final String typeKey = BuiltInRegistries.ITEM.getKey(isi.item).toString();
        return HoverEvent.ShowItem.showItem(Key.key(typeKey), isi.count, PaperAdventure.asBinaryTagHolder(isi.tag.orElse(null)));
    }, si -> {
        final Item itemType = BuiltInRegistries.ITEM.get(PaperAdventure.asVanilla(si.item()));
        final ItemStack stack;
        try {
            final @Nullable CompoundTag tag = si.nbt() != null ? si.nbt().get(PaperAdventure.NBT_CODEC) : null;
            stack = new ItemStack(BuiltInRegistries.ITEM.wrapAsHolder(itemType), si.count(), Optional.ofNullable(tag));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new net.minecraft.network.chat.HoverEvent.ItemStackInfo(stack);
    });

    // TODO legacies
    private static final HoverEventType<HoverEvent.ShowEntity> SHOW_ENTITY_HOVER_EVENT_TYPE = new HoverEventType<>(SHOW_ENTITY_CODEC, HoverEvent.Action.SHOW_ENTITY, "show_entity");
    private static final HoverEventType<HoverEvent.ShowItem> SHOW_ITEM_HOVER_EVENT_TYPE = new HoverEventType<>(SHOW_ITEM_CODEC, HoverEvent.Action.SHOW_ITEM, "show_item");
    private static final HoverEventType<Component> SHOW_TEXT_HOVER_EVENT_TYPE = new HoverEventType<>(COMPONENT_CODEC, HoverEvent.Action.SHOW_TEXT, "show_text");
    private static final Codec<HoverEventType<?>> HOVER_EVENT_TYPE_CODEC = StringRepresentable.fromValues(() -> new HoverEventType<?>[]{ SHOW_ENTITY_HOVER_EVENT_TYPE, SHOW_ITEM_HOVER_EVENT_TYPE, SHOW_TEXT_HOVER_EVENT_TYPE });

    private record HoverEventType<V>(Codec<HoverEvent<V>> codec, String id) implements StringRepresentable {
        private HoverEventType(final Codec<V> contentCodec, final HoverEvent.Action<V> action, final String id) {
            this(contentCodec.xmap(v -> {
                return HoverEvent.hoverEvent(action, v);
            }, HoverEvent::value), id);
        }
        @Override
        public String getSerializedName() {
            return this.id;
        }
    }

    private static final MapCodec<HoverEvent<?>> HOVER_EVENT_MAP_CODEC = HOVER_EVENT_TYPE_CODEC.dispatchMap("action", he -> {
        if (he.action() == HoverEvent.Action.SHOW_ENTITY) {
            return SHOW_ENTITY_HOVER_EVENT_TYPE;
        } else if (he.action() == HoverEvent.Action.SHOW_ITEM) {
            return SHOW_ITEM_HOVER_EVENT_TYPE;
        } else if (he.action() == HoverEvent.Action.SHOW_TEXT) {
            return SHOW_TEXT_HOVER_EVENT_TYPE;
        } else {
            throw new IllegalStateException();
        }
    }, HoverEventType::codec);

    public static final MapCodec<Style> STYLE_CODEC = RecordCodecBuilder.mapCodec((instance) -> {
        return instance.group(
            strictOptionalField(TEXT_COLOR_CODEC, "color").forGetter(styleGetter(Style::color)),
            strictOptionalField(Codec.BOOL, "bold").forGetter(decorationGetter(TextDecoration.BOLD)),
            strictOptionalField(Codec.BOOL, "italic").forGetter(decorationGetter(TextDecoration.ITALIC)),
            strictOptionalField(Codec.BOOL, "underlined").forGetter(decorationGetter(TextDecoration.UNDERLINED)),
            strictOptionalField(Codec.BOOL, "strikethrough").forGetter(decorationGetter(TextDecoration.STRIKETHROUGH)),
            strictOptionalField(Codec.BOOL, "obfuscated").forGetter(decorationGetter(TextDecoration.OBFUSCATED)),
            strictOptionalField(CLICK_EVENT_CODEC, "clickEvent").forGetter(styleGetter(Style::clickEvent)),
            strictOptionalField(HOVER_EVENT_MAP_CODEC.codec(), "hoverEvent").forGetter(styleGetter(Style::hoverEvent)),
            strictOptionalField(Codec.STRING, "insertion").forGetter(styleGetter(Style::insertion)),
            strictOptionalField(KEY_CODEC, "font").forGetter(styleGetter(Style::font))
            ).apply(instance, (textColor, bold, italic, underlined, strikethrough, obfuscated, clickEvent, hoverEvent, insertion, font) -> {
                return Style.style(builder -> {
                    textColor.ifPresent(builder::color);
                    bold.ifPresent(styleBooleanConsumer(builder, TextDecoration.BOLD));
                    italic.ifPresent(styleBooleanConsumer(builder, TextDecoration.ITALIC));
                    underlined.ifPresent(styleBooleanConsumer(builder, TextDecoration.UNDERLINED));
                    strikethrough.ifPresent(styleBooleanConsumer(builder, TextDecoration.STRIKETHROUGH));
                    obfuscated.ifPresent(styleBooleanConsumer(builder, TextDecoration.OBFUSCATED));
                    clickEvent.ifPresent(builder::clickEvent);
                    hoverEvent.ifPresent(builder::hoverEvent);
                    insertion.ifPresent(builder::insertion);
                    font.ifPresent(builder::font);
                });
        });
    });
    private static Consumer<Boolean> styleBooleanConsumer(final Style.Builder builder, final TextDecoration decoration) {
        return b -> builder.decoration(decoration, b);
    }

    private static Function<Style, Optional<Boolean>> decorationGetter(final TextDecoration decoration) {
        return style -> Optional.ofNullable(style.decoration(decoration) == TextDecoration.State.NOT_SET ? null : style.decoration(decoration) == TextDecoration.State.TRUE);
    }

    private static <T> Function<Style, Optional<T>> styleGetter(final Function<Style, @Nullable T> getter) {
        return style -> Optional.ofNullable(getter.apply(style));
    }

    private static final MapCodec<TextComponent> TEXT_COMPONENT_CODEC = RecordCodecBuilder.mapCodec((instance) -> {
        return instance.group(Codec.STRING.fieldOf("text").forGetter(TextComponent::content)).apply(instance, Component::text);
    });
    private static final Codec<Object> PRIMITIVE_ARG_CODEC = ExtraCodecs.validate(ExtraCodecs.JAVA, TranslatableContents::filterAllowedArguments);
    private static final Codec<Component> ARG_CODEC = Codec.either(PRIMITIVE_ARG_CODEC, COMPONENT_CODEC).xmap((either) -> {
        return either.map((object) -> {
            if (object instanceof Integer integer) {
                return text(integer);
            } else if (object instanceof Long l) {
                return text(l);
            } else if (object instanceof String s) {
                return text(s);
            } else if (object instanceof Boolean bool) {
                return text(bool);
            } else if (object instanceof Float f) {
                return text(f);
            } else if (object instanceof Double d) {
                return text(d);
            } else if (object instanceof Short s) {
                return text(s);
            } else {
                throw new IllegalStateException();
            }
        }, (text) -> text);
    }, Either::right);
    private static final MapCodec<TranslatableComponent> TRANSLATABLE_COMPONENT_CODEC = RecordCodecBuilder.mapCodec((instance) -> {
        return instance.group(
            Codec.STRING.fieldOf("translate").forGetter(TranslatableComponent::key),
            Codec.STRING.fieldOf("fallback").forGetter(TranslatableComponent::fallback),
            strictOptionalField(ARG_CODEC.listOf(), "with").forGetter(c -> c.args().isEmpty() ? Optional.empty() : Optional.of(c.args()))
        ).apply(instance, (key, fallback, components) -> {
            return Component.translatable(key, components.orElse(Collections.emptyList())).fallback(fallback);
        });
    });
    private static final MapCodec<KeybindComponent> KEYBIND_COMPONENT_CODEC = KeybindContents.CODEC.xmap(k -> Component.keybind(k.getName()), k -> new KeybindContents(k.keybind()));
    private static final MapCodec<ScoreComponent> SCORE_COMPONENT_CODEC = ScoreContents.INNER_CODEC.xmap(s -> Component.score(s.getName(), s.getObjective()), s -> new ScoreContents(s.name(), s.objective()));

    private record ComponentType<C extends Component>(MapCodec<C> codec, String id) implements StringRepresentable {
        @Override
        public String getSerializedName() {
            return this.id;
        }
    }

    private static final ComponentType<TextComponent> PLAIN = new ComponentType<>(TEXT_COMPONENT_CODEC, "text");
    private static final ComponentType<TranslatableComponent> TRANSLATABLE = new ComponentType<>(TRANSLATABLE_COMPONENT_CODEC, "translatable");
    private static final ComponentType<ScoreComponent> SCORE = new ComponentType<>(SCORE_COMPONENT_CODEC, "score");
    private static final ComponentType<KeybindComponent> KEYBIND = new ComponentType<>(KEYBIND_COMPONENT_CODEC, "keybind");

    private static Codec<Component> createCodec(final Codec<Component> selfCodec) {
        final ComponentType<?>[] types = new ComponentType<?>[]{PLAIN, TRANSLATABLE, SCORE};
        final MapCodec<Component> legacyCodec = ComponentSerialization.createLegacyComponentMatcher(types, ComponentType::codec, component -> {
            if (component instanceof TextComponent) {
                return PLAIN;
            } else if (component instanceof TranslatableComponent) {
                return TRANSLATABLE;
            } else if (component instanceof KeybindComponent) {
                return KEYBIND;
            } else if (component instanceof ScoreComponent) {
                return SCORE;
            } else {
                throw new IllegalStateException();
            }
        }, "type");

        final Codec<Component> codec = RecordCodecBuilder.create((instance) -> {
            return instance.group(legacyCodec.forGetter(Function.identity()), ExtraCodecs.strictOptionalField(ExtraCodecs.nonEmptyList(selfCodec.listOf()), "extra", List.of()).forGetter(Component::children), STYLE_CODEC.forGetter(Component::style)).apply(instance, (component, children, style) -> {
                return component.style(style).children(children);
            });
        });
        return Codec.either(Codec.either(Codec.STRING, ExtraCodecs.nonEmptyList(selfCodec.listOf())), codec).xmap((either) -> {
            return either.map((either2) -> {
                return either2.map(Component::text, AdventureCodecs::createFromList);
            }, (text) -> {
                return text;
            });
        }, (text) -> {
            final @Nullable String string = tryCollapseToString(text);
            return string != null ? Either.left(Either.left(string)) : Either.right(text);
        });
    }

    private static @Nullable String tryCollapseToString(final Component component) {
        if (component instanceof final TextComponent textComponent) {
            if (component.children().isEmpty() && component.style().isEmpty()) {
                return textComponent.content();
            }
        }
        return null;
    }

    private static Component createFromList(final List<Component> components) {
        Component component = components.get(0);
        for (int i = 1; i < components.size(); i++) {
            component = component.append(components.get(i));
        }
        return component;
    }

    private AdventureCodecs() {
    }
}
