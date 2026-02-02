package de.coldfang.voidpressure.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import de.coldfang.voidpressure.config.ClientConfig;
import de.coldfang.voidpressure.data.VoidPressureSavedData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.Locale;

public class VoidPressureCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("voidpressure")
                        .then(Commands.literal("get")
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    ServerLevel level = source.getLevel();

                                    double value = VoidPressureSavedData.get(level).getPressure();
                                    source.sendSuccess(
                                            () -> Component.literal(String.format(Locale.ROOT, "Void Pressure: %.3f", value)),
                                            false
                                    );
                                    return 1;
                                })
                        )

                        .then(Commands.literal("add")
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.001))
                                        .executes(ctx -> {
                                            CommandSourceStack source = ctx.getSource();
                                            ServerLevel level = source.getLevel();

                                            double amount = DoubleArgumentType.getDouble(ctx, "amount");
                                            var data = VoidPressureSavedData.get(level);

                                            double before = data.getPressure();

                                            // ✅ Command soll Anti-Spam ignorieren
                                            data.addPressureUncapped(level, amount);

                                            double after = data.getPressure();

                                            source.sendSuccess(
                                                    () -> Component.literal(String.format(
                                                            Locale.ROOT,
                                                            "Void Pressure: %.3f -> %.3f (+%.3f)",
                                                            before, after, amount
                                                    )),
                                                    false
                                            );

                                            return 1;
                                        })
                                )
                        )

                        .then(Commands.literal("reset")
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    ServerLevel level = source.getLevel();

                                    var data = VoidPressureSavedData.get(level);
                                    data.setPressure(0.0);
                                    source.sendSuccess(() -> Component.literal("Void Pressure reset to 0.000"), false);
                                    return 1;
                                })
                        )

                        .then(Commands.literal("hud")
                                .executes(ctx -> {
                                    if (!isSingleplayerIntegrated(ctx.getSource())) return 0;

                                    boolean enabled = ClientConfig.HUD_ENABLED.get();
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("HUD: " + (enabled ? "ON" : "OFF")),
                                            false
                                    );
                                    return 1;
                                })
                                .then(Commands.literal("on")
                                        .executes(ctx -> setHud(ctx.getSource(), true))
                                )
                                .then(Commands.literal("off")
                                        .executes(ctx -> setHud(ctx.getSource(), false))
                                )
                                .then(Commands.literal("toggle")
                                        .executes(ctx -> {
                                            if (!isSingleplayerIntegrated(ctx.getSource())) return 0;
                                            return setHud(ctx.getSource(), !ClientConfig.HUD_ENABLED.get());
                                        })
                                )
                        )
        );
    }

    private static int setHud(CommandSourceStack source, boolean enabled) {
        if (!isSingleplayerIntegrated(source)) return 0;

        ClientConfig.HUD_ENABLED.set(enabled);

        source.sendSuccess(
                () -> Component.literal("HUD: " + (enabled ? "ON" : "OFF")),
                false
        );
        return 1;
    }
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isSingleplayerIntegrated(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        if (server.isDedicatedServer()) {
            source.sendFailure(Component.literal("HUD is client-side. Use this command in singleplayer."));
            return false;
        }
        return true;
    }
}