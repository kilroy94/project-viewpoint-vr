package viewpointvr.diagnostic;

import java.nio.file.*;
import java.security.*;
import java.util.*;

public final class BinaryPins {
    public static final String GAME="e1a69eb743ede60b213a0fe7f8b83d4fcab773036d256cc4543a336f3b058a33";
    public static final String VIEWPOINT="8e2aa52087c9c111c09f28e8ee1f8f50fc8d9132c20c533a05305e134a7d695c";
    public static final Set<String> LOADERS=Set.of("6dd95cedce60f03bf8b8cefd0d19eb156230e0d54bffa07de9da5212a06c7be6","dd13e6e06e64be0e832a4f13508c6872de36c9c7b2290023c5884a7f74467283");
    public static void verify(Path game,Path viewpoint,Path loader) throws Exception {
        check(game,Set.of(GAME)); check(viewpoint,Set.of(VIEWPOINT)); check(loader,LOADERS);
    }
    public static void check(Path path,Set<String> accepted) throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        try(var in=Files.newInputStream(path)) { byte[] block=new byte[65536]; for(int n;(n=in.read(block))!=-1;) digest.update(block,0,n); }
        String hash=HexFormat.of().formatHex(digest.digest());
        if(!accepted.contains(hash)) throw new IllegalArgumentException("Unsupported binary "+path.getFileName()+": "+hash);
    }
    public static Path location(Class<?> type) throws Exception {
        return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
    }
    private BinaryPins() {}
}
