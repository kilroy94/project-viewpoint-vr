package viewpointvr.diagnostic;

import java.nio.file.*;
import java.security.*;
import java.util.*;

public final class BinaryPins {
    public static final String GAME="e1a69eb743ede60b213a0fe7f8b83d4fcab773036d256cc4543a336f3b058a33";
    public static final String VIEWPOINT="096bd37d63cb699ac95e9e7ea5245a1b9b5ff08c5161db35f6733b1d1c42b064";
    public static final Set<String> LOADERS=Set.of("eb79b9876332010733a8e0d7ce4fe0377846059a9e28859029e2a0fb449f6cf2");
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
