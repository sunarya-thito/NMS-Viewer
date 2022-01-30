package net.minecraft.server.dedicated;

import java.nio.file.Path;
import java.util.function.UnaryOperator;

// CraftBukkit start
import java.io.File;
import joptsimple.OptionSet;
// CraftBukkit end

public class DedicatedServerSettings {

    private final Path source;
    private DedicatedServerProperties properties;

    // CraftBukkit start
    public DedicatedServerSettings(OptionSet optionset) {
        this.source = ((File) optionset.valueOf("config")).toPath();
        this.properties = DedicatedServerProperties.load(source, optionset);
        // CraftBukkit end
    }

    public DedicatedServerProperties getProperties() {
        return this.properties;
    }

    public void save() {
        this.properties.savePropertiesFile(this.source);
    }

    public DedicatedServerSettings setProperty(UnaryOperator<DedicatedServerProperties> unaryoperator) {
        (this.properties = (DedicatedServerProperties) unaryoperator.apply(this.properties)).savePropertiesFile(this.source);
        return this;
    }
}
