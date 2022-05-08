package net.minestom.ui;

import imgui.app.Application;
import imgui.app.Configuration;
import imgui.type.ImDouble;
import imgui.type.ImFloat;
import imgui.type.ImInt;
import imgui.type.ImString;
import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerProcess;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.Section;
import net.minestom.server.instance.palette.Palette;
import net.minestom.server.thread.Acquirable;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static imgui.ImGui.*;
import static imgui.flag.ImGuiInputTextFlags.EnterReturnsTrue;
import static imgui.flag.ImGuiInputTextFlags.ReadOnly;

final class ServerUI extends Application {
    private final ServerProcess process = MinecraftServer.process();
    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    
    @Override
    protected void configure(Configuration config) {
        config.setTitle("MinestomUI");
    }
    
    @Override
    public void process() {
        begin("Instances");
        for (Instance instance : process.instance().getInstances()) {
            if (treeNode(instance.toString())) {
                instance(instance);
                treePop();
            }
        }
        end();
        
        begin("Console");
        console();
        end();
        
        tick();
    }
    
    public void tick() {
        List<Runnable> tasks = new ArrayList<>();
        queue.drainTo(tasks);
    
        for (Runnable task : tasks) {
            Acquirable.of(task).sync(Runnable::run);
        }
    }

    private void instance(@Nullable Instance instance) {
        if (instance == null) return;
        
        // World age
        inputInt("World age", new ImInt(Math.toIntExact(instance.getWorldAge())), 0, 0, ReadOnly);
        
        // Time
        ImInt time = new ImInt(Math.toIntExact(instance.getTime()));
        if (inputInt("Time", time, 100, 1000, EnterReturnsTrue))
            queue.add(() -> instance.setTime(time.get()));

        // Dimension type
        dimensionType(instance.getDimensionType());
    
        // Chunks
        if (instance.getChunks().size() > 0 && treeNode("Chunks")) {
            instance.getChunks().forEach(this::chunk);
            treePop();
        }
        
        // Entities
        if (instance.getEntities().size() > 0 && treeNode("Entities")) {
            instance.getEntities().forEach(this::entity);
            treePop();
        }
    }
    
    private void chunk(@Nullable Chunk chunk) {
        if (chunk == null) return;
        
        if (treeNode(chunk.toString())) {
            // Location
            inputInt("chunkX", new ImInt(chunk.getChunkX()), 0, 0, ReadOnly);
            inputInt("chunkZ", new ImInt(chunk.getChunkZ()), 0, 0, ReadOnly);
            
            // Sections
            if (chunk.getSections().size() > 0 && treeNode("Sections")) {
                chunk.getSections().forEach(this::section);
                treePop();
            }
            
            // Entities
            Collection<Entity> entities = chunk.getInstance().getChunkEntities(chunk);
            if (entities.size() > 0 && treeNode("Entities")) {
                entities.forEach(this::entity);
                treePop();
            }
    
            // Instance
            if (treeNode("Instance")) {
                instance(chunk.getInstance());
                treePop();
            }
            
            treePop();
        }
    }
    
    private void section(@Nullable Section section) {
        if (section == null) return;
    
        if (treeNode(section.toString())) {
            // Block palette
            if (treeNode("Block Palette")) {
                final Palette palette = section.blockPalette();
                
                inputInt("Count", new ImInt(palette.count()), ReadOnly);
                inputInt("Max size", new ImInt(palette.maxSize()), ReadOnly);
                inputInt("Dimension", new ImInt(palette.dimension()), ReadOnly);
                
                StringBuilder builder = new StringBuilder();
                palette.getAllPresent((x, y, z, value) -> {
                    builder.append(x)
                        .append(",")
                        .append(y)
                        .append(",")
                        .append(z)
                        .append(" = ")
                        .append(value)
                        .append("\n");
                });
                inputTextMultiline("Entries", new ImString(builder.toString()), ReadOnly);
        
                treePop();
            }
            
            treePop();
        }
    }

    private void entity(@Nullable Entity entity) {
        if (entity == null) return;
        
        if (treeNode(entity.getEntityId() + " " + entity.getEntityType().name())) {
            // UUID
            ImString uuid = new ImString(entity.getUuid().toString());
            if (inputText("Uuid", uuid, EnterReturnsTrue)) {
                queue.add(() -> {
                    try {
                        entity.setUuid(UUID.fromString(uuid.get()));
                    } catch (Exception ignored) {}
                });
            }
            
            // Position
            {
                ImDouble x = new ImDouble(entity.getPosition().x());
                ImDouble y = new ImDouble(entity.getPosition().y());
                ImDouble z = new ImDouble(entity.getPosition().z());
                ImFloat yaw = new ImFloat(entity.getPosition().yaw());
                ImFloat pitch = new ImFloat(entity.getPosition().pitch());
                if (treeNode("Position")) {
                    int changed = inputDouble("x", x, 1, 10, "%.6f", EnterReturnsTrue) ? 1 : 0;
                    changed += inputDouble("y", y, 1, 10, "%.6f", EnterReturnsTrue) ? 1 : 0;
                    changed += inputDouble("z", z, 1, 10, "%.6f", EnterReturnsTrue) ? 1 : 0;
                    changed += inputFloat("yaw", yaw, 1, 10, "%.6f", EnterReturnsTrue) ? 1 : 0;
                    changed += inputFloat("pitch", pitch, 1, 10, "%.6f", EnterReturnsTrue) ? 1 : 0;
    
                    if (changed > 0)
                        queue.add(() -> entity.teleport(new Pos(x.get(), y.get(), z.get(), yaw.get(), pitch.get())));
                        
                    treePop();
                }
            }
    
            // Velocity
            {
                ImDouble x = new ImDouble(entity.getVelocity().x());
                ImDouble y = new ImDouble(entity.getVelocity().y());
                ImDouble z = new ImDouble(entity.getVelocity().z());
                if (treeNode("Velocity")) {
                    int changed = inputDouble("x", x, 1, 10, "%.6f", EnterReturnsTrue) ? 1 : 0;
                    changed += inputDouble("y", y, 1, 10, "%.6f", EnterReturnsTrue) ? 1 : 0;
                    changed += inputDouble("z", z, 1, 10, "%.6f", EnterReturnsTrue) ? 1 : 0;

                    if (changed > 0)
                        queue.add(() -> entity.setVelocity(new Vec(x.get(), y.get(), z.get())));
                    
                    treePop();
                }
            }

            // Instance
            if (treeNode("Instance")) {
                instance(entity.getInstance());
                treePop();
            }
            
            // Chunk
            chunk(entity.getChunk());
            
            // Viewers
            if (entity.getViewers().size() > 0 && treeNode("Viewers")) {
                entity.getViewers().forEach(this::entity);
                treePop();
            }

            // Passengers
            if (entity.getPassengers().size() > 0 && treeNode("Passengers")) {
                entity.getPassengers().forEach(this::entity);
                treePop();
            }
            
            // Vehicle
            Entity vehicle = entity.getVehicle();
            if (vehicle != null) {
                entity(vehicle);
            }
            
            treePop();
        }
    }

    private void dimensionType(@Nullable DimensionType dimension) {
        if (dimension == null) return;
        
        if (treeNode("Dimension type")) {
            checkboxFlags("ultrawarm", bool(dimension.isUltrawarm()), ReadOnly);
            checkboxFlags("natural", bool(dimension.isNatural()), ReadOnly);
            checkboxFlags("piglin safe", bool(dimension.isPiglinSafe()), ReadOnly);
            checkboxFlags("respawn anchor safe", bool(dimension.isRespawnAnchorSafe()), ReadOnly);
            checkboxFlags("bed safe", bool(dimension.isBedSafe()), ReadOnly);
            checkboxFlags("raid capable", bool(dimension.isRaidCapable()), ReadOnly);
            checkboxFlags("skylight", bool(dimension.isSkylightEnabled()), ReadOnly);
            checkboxFlags("ceiling", bool(dimension.isCeilingEnabled()), ReadOnly);
            checkboxFlags("fixed time", bool(dimension.getFixedTime() != null), ReadOnly);
            inputFloat("ambient light", new ImFloat(dimension.getAmbientLight()), 0, 0, "%.6f", ReadOnly);
            inputInt("height", new ImInt(dimension.getHeight()), 0, 0, ReadOnly);
            inputInt("min y", new ImInt(dimension.getMinY()), 0, 0, ReadOnly);
            inputInt("logical height", new ImInt(dimension.getLogicalHeight()), 0, 0, ReadOnly);
            inputText("infiniburn", new ImString(dimension.getInfiniburn().toString()), ReadOnly);
            treePop();
        }
    }
    
    private void console() {
        ImString command = new ImString();
        if (inputText("Command", command, EnterReturnsTrue)) {
            queue.add(() -> process.command()
                .getDispatcher()
                .execute(
                    process.command().getConsoleSender(),
                    command.get()
                ));
        }
    }
    
    private static ImInt bool(boolean value) {
        return new ImInt(value ? 1 : 0);
    }
}
