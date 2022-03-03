package net.minestom.ui;

import imgui.ImGui;
import imgui.app.Application;
import net.minestom.server.snapshot.ServerSnapshot;

final class ServerUI extends Application {

    volatile ServerSnapshot snapshot;

    @Override
    public void process() {
        var snapshot = this.snapshot;
        if (snapshot == null) return;

        ImGui.begin("Instances");
        for(var instance : snapshot.instances()) {
            if(ImGui.treeNode(instance.dimensionType().toString())){
                for(var entity : instance.entities()) {
                    ImGui.text(entity.type().toString());
                }
                ImGui.treePop();
            }
        }
        ImGui.end();
    }
}
