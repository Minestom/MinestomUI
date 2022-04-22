package net.minestom.ui;

import imgui.app.Application;
import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerProcess;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.atomic.AtomicBoolean;

public final class MinestomUI {
    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    public static void launch() {
        if (!initialized.compareAndSet(false, true)) {
            throw new IllegalStateException("MinestomUI is already initialized");
        }
        ServerProcess process = MinecraftServer.process();
        if (process == null) {
            throw new IllegalStateException("Minestom server must be initialized before launching the UI");
        }
        ServerUI ui = new ServerUI();
        new Thread(() -> Application.launch(ui)).start();
    
        // Close the ui when the server is closed
        process.scheduler().buildShutdownTask(() -> GLFW.glfwSetWindowShouldClose(ui.getHandle(), true));
    }
}
