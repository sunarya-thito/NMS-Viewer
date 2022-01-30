package net.minecraft.server;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ResourcePackVanilla implements IResourcePack {

    public static java.nio.file.Path a;
    private static final Logger d = LogManager.getLogger();
    public static Class<?> b;
    public final Set<String> c;

    public ResourcePackVanilla(String... astring) {
        this.c = ImmutableSet.copyOf(astring);
    }

    public InputStream b(String s) throws IOException {
        if (!s.contains("/") && !s.contains("\\")) {
            if (ResourcePackVanilla.a != null) {
                java.nio.file.Path java_nio_file_path = ResourcePackVanilla.a.resolve(s);

                if (Files.exists(java_nio_file_path, new LinkOption[0])) {
                    return Files.newInputStream(java_nio_file_path);
                }
            }

            return this.a(s);
        } else {
            throw new IllegalArgumentException("Root resources can only be filenames, not paths (no / allowed!)");
        }
    }

    public InputStream a(EnumResourcePackType enumresourcepacktype, MinecraftKey minecraftkey) throws IOException {
        InputStream inputstream = this.c(enumresourcepacktype, minecraftkey);

        if (inputstream != null) {
            return inputstream;
        } else {
            throw new FileNotFoundException(minecraftkey.getKey());
        }
    }

    public Collection<MinecraftKey> a(EnumResourcePackType enumresourcepacktype, String s, int i, Predicate<String> predicate) {
        Set<MinecraftKey> set = Sets.newHashSet();
        URI uri;

        if (ResourcePackVanilla.a != null) {
            try {
                set.addAll(this.a(i, "minecraft", ResourcePackVanilla.a.resolve(enumresourcepacktype.a()).resolve("minecraft"), s, predicate));
            } catch (IOException ioexception) {
                ;
            }

            if (enumresourcepacktype == EnumResourcePackType.CLIENT_RESOURCES) {
                Enumeration enumeration = null;

                try {
                    enumeration = ResourcePackVanilla.b.getClassLoader().getResources(enumresourcepacktype.a() + "/minecraft");
                } catch (IOException ioexception1) {
                    ;
                }

                while (enumeration != null && enumeration.hasMoreElements()) {
                    try {
                        uri = ((URL) enumeration.nextElement()).toURI();
                        if ("file".equals(uri.getScheme())) {
                            set.addAll(this.a(i, "minecraft", Paths.get(uri), s, predicate));
                        }
                    } catch (IOException | URISyntaxException urisyntaxexception) {
                        ;
                    }
                }
            }
        }

        try {
            URL url = ResourcePackVanilla.class.getResource("/" + enumresourcepacktype.a() + "/.mcassetsroot");

            if (url == null) {
                ResourcePackVanilla.d.error("Couldn't find .mcassetsroot, cannot load vanilla resources");
                return set;
            }

            uri = url.toURI();
            if ("file".equals(uri.getScheme())) {
                URL url1 = new URL(url.toString().substring(0, url.toString().length() - ".mcassetsroot".length()) + "minecraft");

                if (url1 == null) {
                    return set;
                }

                java.nio.file.Path java_nio_file_path = Paths.get(url1.toURI());

                set.addAll(this.a(i, "minecraft", java_nio_file_path, s, predicate));
            } else if ("jar".equals(uri.getScheme())) {
                FileSystem filesystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
                Throwable throwable = null;

                try {
                    java.nio.file.Path java_nio_file_path1 = filesystem.getPath("/" + enumresourcepacktype.a() + "/minecraft");

                    set.addAll(this.a(i, "minecraft", java_nio_file_path1, s, predicate));
                } catch (Throwable throwable1) {
                    throwable = throwable1;
                    throw throwable1;
                } finally {
                    if (filesystem != null) {
                        if (throwable != null) {
                            try {
                                filesystem.close();
                            } catch (Throwable throwable2) {
                                throwable.addSuppressed(throwable2);
                            }
                        } else {
                            filesystem.close();
                        }
                    }

                }
            } else {
                ResourcePackVanilla.d.error("Unsupported scheme {} trying to list vanilla resources (NYI?)", uri);
            }
        } catch (NoSuchFileException | FileNotFoundException filenotfoundexception) {
            ;
        } catch (IOException | URISyntaxException urisyntaxexception1) {
            ResourcePackVanilla.d.error("Couldn't get a list of all vanilla resources", urisyntaxexception1);
        }

        return set;
    }

    private Collection<MinecraftKey> a(int i, String s, java.nio.file.Path java_nio_file_path, String s1, Predicate<String> predicate) throws IOException {
        List<MinecraftKey> list = Lists.newArrayList();
        Iterator iterator = Files.walk(java_nio_file_path.resolve(s1), i, new FileVisitOption[0]).iterator();

        while (iterator.hasNext()) {
            java.nio.file.Path java_nio_file_path1 = (java.nio.file.Path) iterator.next();

            if (!java_nio_file_path1.endsWith(".mcmeta") && Files.isRegularFile(java_nio_file_path1, new LinkOption[0]) && predicate.test(java_nio_file_path1.getFileName().toString())) {
                list.add(new MinecraftKey(s, java_nio_file_path.relativize(java_nio_file_path1).toString().replaceAll("\\\\", "/")));
            }
        }

        return list;
    }

    @Nullable
    protected InputStream c(EnumResourcePackType enumresourcepacktype, MinecraftKey minecraftkey) {
        String s = "/" + enumresourcepacktype.a() + "/" + minecraftkey.b() + "/" + minecraftkey.getKey();

        if (ResourcePackVanilla.a != null) {
            java.nio.file.Path java_nio_file_path = ResourcePackVanilla.a.resolve(enumresourcepacktype.a() + "/" + minecraftkey.b() + "/" + minecraftkey.getKey());

            if (Files.exists(java_nio_file_path, new LinkOption[0])) {
                try {
                    return Files.newInputStream(java_nio_file_path);
                } catch (IOException ioexception) {
                    ;
                }
            }
        }

        try {
            URL url = ResourcePackVanilla.class.getResource(s);

            return url != null && ResourcePackFolder.a(new File(url.getFile()), s) ? ResourcePackVanilla.class.getResourceAsStream(s) : null;
        } catch (IOException ioexception1) {
            return ResourcePackVanilla.class.getResourceAsStream(s);
        }
    }

    @Nullable
    protected InputStream a(String s) {
        return ResourcePackVanilla.class.getResourceAsStream("/" + s);
    }

    public boolean b(EnumResourcePackType enumresourcepacktype, MinecraftKey minecraftkey) {
        InputStream inputstream = this.c(enumresourcepacktype, minecraftkey);
        boolean flag = inputstream != null;

        IOUtils.closeQuietly(inputstream);
        return flag;
    }

    public Set<String> a(EnumResourcePackType enumresourcepacktype) {
        return this.c;
    }

    @Nullable
    public <T> T a(ResourcePackMetaParser<T> resourcepackmetaparser) throws IOException {
        try {
            InputStream inputstream = this.b("pack.mcmeta");
            Throwable throwable = null;

            Object object;

            try {
                object = ResourcePackAbstract.a(resourcepackmetaparser, inputstream);
            } catch (Throwable throwable1) {
                throwable = throwable1;
                throw throwable1;
            } finally {
                if (inputstream != null) {
                    if (throwable != null) {
                        try {
                            inputstream.close();
                        } catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    } else {
                        inputstream.close();
                    }
                }

            }

            return object;
        } catch (FileNotFoundException | RuntimeException runtimeexception) {
            return null;
        }
    }

    public String a() {
        return "Default";
    }

    public void close() {}
}
