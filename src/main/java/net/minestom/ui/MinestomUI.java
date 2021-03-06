package net.minestom.ui;

import imgui.app.Application;
import net.minestom.server.MinecraftServer;
import net.minestom.server.snapshot.ServerSnapshot;
import net.minestom.server.timer.TaskSchedule;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.atomic.AtomicBoolean;

public final class MinestomUI {
    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    public static void launch() {
        if (!initialized.compareAndSet(false, true)) {
            throw new IllegalStateException("MinestomUI is already initialized");
        }
        var process = MinecraftServer.process();
        if (process == null) {
            throw new IllegalStateException("Minestom server must be initialized before launching the UI");
        }
        ServerUI ui = new ServerUI();
        new Thread(() -> Application.launch(ui)).start();

        var scheduler = process.scheduler();
        // Retrieve up-to-date server info every tick
        scheduler.scheduleTask(() -> {
            ui.snapshotReference = ServerSnapshot.update();
        }, TaskSchedule.nextTick(), TaskSchedule.nextTick());
        // Close the ui when the server is closed
        scheduler.buildShutdownTask(() -> GLFW.glfwSetWindowShouldClose(ui.getHandle(), true));
    }
}
