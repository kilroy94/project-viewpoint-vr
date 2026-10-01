package viewpointvr.diagnostic;
import java.nio.file.*;
import se.krka.kahlua.luaj.compiler.LuaCompiler;
public final class LuaSyntaxTest {
    public static void main(String[] args)throws Exception {
        try(var input=Files.newInputStream(Path.of(args[0]))){
            if(LuaCompiler.loadis(input,"ProjectViewpointVR.lua",null)==null)throw new AssertionError("No compiled closure");
        }
        System.out.println("Lua controls compile with bundled Kahlua; no script/game entry point executed");
    }
}
