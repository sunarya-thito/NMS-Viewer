package net.minecraft.server.packs;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.packs.metadata.ResourcePackMetaParser;
import net.minecraft.server.packs.metadata.pack.ResourcePackInfo;
import net.minecraft.server.packs.resources.IResource;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ResourcePackVanilla implements IResourcePack, ResourceProvider {

    public static Path generatedDir;
    private static final Logger LOGGER = LogManager.getLogger();
    public static Class<?> clientObject;
    private static final Map<EnumResourcePackType, Path> ROOT_DIR_BY_TYPE = (Map) SystemUtils.a(() -> {
        Class oclass = ResourcePackVanilla.class;

        synchronized (ResourcePackVanilla.class) {
            Builder<EnumResourcePackType, Path> builder = ImmutableMap.builder();
            EnumResourcePackType[] aenumresourcepacktype = EnumResourcePackType.values();
            int i = aenumresourcepacktype.length;

            for (int j = 0; j < i; ++j) {
                EnumResourcePackType enumresourcepacktype = aenumresourcepacktype[j];
                String s = "/" + enumresourcepacktype.a() + "/.mcassetsroot";
                URL url = ResourcePackVanilla.class.getResource(s);

                if (url == null) {
                    ResourcePackVanilla.LOGGER.error("File {} does not exist in classpath", s);
                } else {
                    try {
                        URI uri = url.toURI();
                        String s1 = uri.getScheme();

                        if (!"jar".equals(s1) && !"file".equals(s1)) {
                            ResourcePackVanilla.LOGGER.warn("Assets URL '{}' uses unexpected schema", uri);
                        }

                        Path path = a(uri);

                        builder.put(enumresourcepacktype, path.getParent());
                    } catch (Exception exception) {
                        ResourcePackVanilla.LOGGER.error("Couldn't resolve path to vanilla assets", exception);
                    }
                }
            }

            return builder.build();
        }
    });
    public final ResourcePackInfo packMetadata;
    public final Set<String> namespaces;

    private static Path a(URI uri) throws IOException {
        try {
            return Paths.get(uri);
        } catch (FileSystemNotFoundException filesystemnotfoundexception) {
            ;
        } catch (Throwable throwable) {
            ResourcePackVanilla.LOGGER.warn("Unable to get path for: {}", uri, throwable);
        }

        try {
            FileSystems.newFileSystem(uri, Collections.emptyMap());
        } catch (FileSystemAlreadyExistsException filesystemalreadyexistsexception) {
            ;
        }

        return Paths.get(uri);
    }

    public ResourcePackVanilla(ResourcePackInfo resourcepackinfo, String... astring) {
        this.packMetadata = resourcepackinfo;
        this.namespaces = ImmutableSet.copyOf(astring);
    }

    @Override
    public InputStream b(String s) throws IOException {
        if (!s.contains("/") && !s.contains("\\")) {
            if (ResourcePackVanilla.generatedDir != null) {
                Path path = ResourcePackVanilla.generatedDir.resolve(s);

                if (Files.exists(path, new LinkOption[0])) {
                    return Files.newInputStream(path);
                }
            }

            return this.a(s);
        } else {
            throw new IllegalArgumentException("Root resources can only be filenames, not paths (no / allowed!)");
        }
    }

    @Override
    public InputStream a(EnumResourcePackType enumresourcepacktype, MinecraftKey minecraftkey) throws IOException {
        InputStream inputstream = this.c(enumresourcepacktype, minecraftkey);

        if (inputstream != null) {
            return inputstream;
        } else {
            throw new FileNotFoundException(minecraftkey.getKey());
        }
    }

    @Override
    public Collection<MinecraftKey> a(EnumResourcePackType enumresourcepacktype, String s, String s1, int i, Predicate<String> predicate) {
        Set<MinecraftKey> set = Sets.newHashSet();

        if (ResourcePackVanilla.generatedDir != null) {
            try {
                a(set, i, s, ResourcePackVanilla.generatedDir.resolve(enumresourcepacktype.a()), s1, predicate);
            } catch (IOException ioexception) {
                ;
            }

            if (enumresourcepacktype == EnumResourcePackType.CLIENT_RESOURCES) {
                Enumeration enumeration = null;

                try {
                    enumeration = ResourcePackVanilla.clientObject.getClassLoader().getResources(enumresourcepacktype.a() + "/");
                } catch (IOException ioexception1) {
                    ;
                }

                while (enumeration != null && enumeration.hasMoreElements()) {
                    try {
                        URI uri = ((URL) enumeration.nextElement()).toURI();

                        if ("file".equals(uri.getScheme())) {
                            a(set, i, s, Paths.get(uri), s1, predicate);
                        }
                    } catch (IOException | URISyntaxException urisyntaxexception) {
                        ;
                    }
                }
            }
        }

        try {
            Path path = (Path) ResourcePackVanilla.ROOT_DIR_BY_TYPE.get(enumresourcepacktype);

            if (path != null) {
                a(set, i, s, path, s1, predicate);
            } else {
                ResourcePackVanilla.LOGGER.error("Can't access assets root for type: {}", enumresourcepacktype);
            }
        } catch (NoSuchFileException | FileNotFoundException filenotfoundexception) {
            ;
        } catch (IOException ioexception2) {
            ResourcePackVanilla.LOGGER.error("Couldn't get a list of all vanilla resources", ioexception2);
        }

        return set;
    }

    private static void a(Collection<MinecraftKey> collection, int i, String s, Path path, String s1, Predicate<String> predicate) throws IOException {
        Path path1 = path.resolve(s);
        Stream stream = Files.walk(path1.resolve(s1), i, new FileVisitOption[0]);

        try {
            Stream stream1 = stream.filter((path2) -> {
                return !path2.endsWith(".mcmeta") && Files.isRegularFile(path2, new LinkOption[0]) && predicate.test(path2.getFileName().toString());
            }).map((path2) -> {
                return new MinecraftKey(s, path1.relativize(path2).toString().replaceAll("\\\\", "/"));
            });

            Objects.requireNonNull(collection);
            stream1.forEach(collection::add);
        } catch (Throwable throwable) {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }
            }

            throw throwable;
        }

        if (stream != null) {
            stream.close();
        }

    }

    @Nullable
    protected InputStream c(EnumResourcePackType enumresourcepacktype, MinecraftKey minecraftkey) {
        String s = d(enumresourcepacktype, minecraftkey);

        if (ResourcePackVanilla.generatedDir != null) {
            Path path = ResourcePackVanilla.generatedDir;
            String s1 = enumresourcepacktype.a();
            Path path1 = path.resolve(s1 + "/" + minecraftkey.getNamespace() + "/" + minecraftkey.getKey());

            if (Files.exists(path1, new LinkOption[0])) {
                try {
                    return Files.newInputStream(path1);
                } catch (IOException ioexception) {
                    ;
                }
            }
        }

        try {
            URL url = ResourcePackVanilla.class.getResource(s);

            return a(s, url) ? url.openStream() : null;
        } catch (IOException ioexception1) {
            return ResourcePackVanilla.class.getResourceAsStream(s);
        }
    }

    private static String d(EnumResourcePackType enumresourcepacktype, MinecraftKey minecraftkey) {
        String s = enumresourcepacktype.a();

        return "/" + s + "/" + minecraftkey.getNamespace() + "/" + minecraftkey.getKey();
    }

    private static boolean a(String s, @Nullable URL url) throws IOException {
        return url != null && (url.getProtocol().equals("jar") || ResourcePackFolder.a(new File(url.getFile()), s));
    }

    @Nullable
    protected InputStream a(String s) {
        return ResourcePackVanilla.class.getResourceAsStream("/" + s);
    }

    @Override
    public boolean b(EnumResourcePackType enumresourcepacktype, MinecraftKey minecraftkey) {
        String s = d(enumresourcepacktype, minecraftkey);

        if (ResourcePackVanilla.generatedDir != null) {
            Path path = ResourcePackVanilla.generatedDir;
            String s1 = enumresourcepacktype.a();
            Path path1 = path.resolve(s1 + "/" + minecraftkey.getNamespace() + "/" + minecraftkey.getKey());

            if (Files.exists(path1, new LinkOption[0])) {
                return true;
            }
        }

        try {
            URL url = ResourcePackVanilla.class.getResource(s);

            return a(s, url);
        } catch (IOException ioexception) {
            return false;
        }
    }

    @Override
    public Set<String> a(EnumResourcePackType enumresourcepacktype) {
        return this.namespaces;
    }

    @Nullable
    @Override
    public <T> T a(ResourcePackMetaParser<T> resourcepackmetaparser) throws IOException {
        try {
            InputStream inputstream = this.b("pack.mcmeta");

            label52:
            {
                Object object;

                try {
                    if (inputstream == null) {
                        break label52;
                    }

                    T t0 = ResourcePackAbstract.a(resourcepackmetaparser, inputstream);

                    if (t0 == null) {
                        break label52;
                    }

                    object = t0;
                } catch (Throwable throwable) {
                    if (inputstream != null) {
                        try {
                            inputstream.close();
                        } catch (Throwable throwable1) {
                            throwable.addSuppressed(throwable1);
                        }
                    }

                    throw throwable;
                }

                if (inputstream != null) {
                    inputstream.close();
                }

                return object;
            }

            if (inputstream != null) {
                inputstream.close();
            }
        } catch (FileNotFoundException | RuntimeException runtimeexception) {
            ;
        }

        return resourcepackmetaparser == ResourcePackInfo.SERIALIZER ? this.packMetadata : null;
    }

    @Override
    public String a() {
        return "Default";
    }

    @Override
    public void close() {}

    @Override
    public IResource a(final MinecraftKey minecraftkey) throws IOException {
        return new IResource() {
            @Nullable
            InputStream inputStream;

            public void close() throws IOException {
                if (this.inputStream != null) {
                    this.inputStream.close();
                }

            }

            @Override
            public MinecraftKey a() {
                return minecraftkey;
            }

            @Override
            public InputStream b() {
                try {
                    this.inputStream = ResourcePackVanilla.this.a(EnumResourcePackType.CLIENT_RESOURCES, minecraftkey);
                } catch (IOException ioexception) {
                    throw new UncheckedIOException("Could not get client resource from vanilla pack", ioexception);
                }

                return this.inputStream;
            }

            @Override
            public boolean c() {
                return false;
            }

            @Nullable
            @Override
            public <T> T a(ResourcePackMetaParser<T> resourcepackmetaparser) {
                return null;
            }

            @Override
            public String d() {
                return minecraftkey.toString();
            }
        };
    }
}
