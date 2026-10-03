package viewpointvr.diagnostic;
import java.nio.file.*;
import java.util.Properties;
import viewpointvr.input.TurnSettings;
public final class SettingsStoreTest {
 static int checks;static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 public static void main(String[] args)throws Exception{
  Path root=Files.createTempDirectory(Path.of(args[0]),"settings-test-");Path file=root.resolve("config/settings.properties");
  var store=new SettingsStore(file);
  check(store.turning().equals(TurnSettings.DEFAULT)&&store.scale()==1,"first startup defaults");
  check(!Files.exists(file.getParent()),"loading missing preferences creates no files");
  store.turning(new TurnSettings(2,60,150));store.scale(1.75);
  var restarted=new SettingsStore(file);
  check(restarted.turning().equals(new TurnSettings(2,60,150)),"turning restored across new instance");
  check(restarted.scale()==1.75f,"scale restored across new instance");
  var props=new Properties();try(var reader=Files.newBufferedReader(file)){props.load(reader);}
  check(props.size()==4&&!props.containsKey("mode")&&!props.containsKey("controllers"),"only four preferences stored, no activation");
  restarted.turning(new TurnSettings(0,90,240));
  check(new SettingsStore(file).scale()==1.75f,"turn change preserves scale");
  restarted.scale(.25);check(new SettingsStore(file).turning().mode()==0,"scale change preserves turning Off");
  restarted.scale(4);check(new SettingsStore(file).scale()==4,"scale boundaries round trip");
  for(double value:new double[]{Double.NaN,Double.POSITIVE_INFINITY,.24,4.01}){
   try{restarted.scale(value);throw new AssertionError("invalid scale accepted");}catch(IllegalArgumentException expected){checks++;}
   check(new SettingsStore(file).scale()==4,"invalid update leaves saved scale intact");
  }
  Files.writeString(file,"turnMode=99\nsnapAngle=bad\nsmoothSpeed=31\nworldScale=NaN\n");
  var invalid=new SettingsStore(file);check(invalid.turning().equals(TurnSettings.DEFAULT)&&invalid.scale()==1,"invalid fields use defaults");
  Files.writeString(file,"turnMode=2\nsnapAngle=15\nsmoothSpeed=bad\nworldScale=2.5\n");
  invalid=new SettingsStore(file);check(invalid.turning().equals(new TurnSettings(2,15,90))&&invalid.scale()==2.5f,"invalid field preserves other valid preferences");
  Files.writeString(file,"snapAngle=45\n");invalid=new SettingsStore(file);check(invalid.turning().equals(new TurnSettings(1,45,90))&&invalid.scale()==1,"missing keys independently default");
  Files.writeString(file,"bad="+"\\"+"uZZZZ");invalid=new SettingsStore(file);check(!invalid.warning().isEmpty()&&invalid.turning().equals(TurnSettings.DEFAULT),"malformed file does not prevent startup");
  Path blocked=root.resolve("blocked");Files.createDirectory(blocked);Path sentinel=blocked.resolve("keep");Files.writeString(sentinel,"existing");
  var failure=new SettingsStore(blocked);failure.scale(2);
  check(failure.scale()==2&&!failure.warning().isEmpty(),"failed write retains current session choice with warning");
  check(Files.readString(sentinel).equals("existing"),"failed replacement preserves previous target");
  try(var paths=Files.list(root)){check(paths.noneMatch(p->p.getFileName().toString().endsWith(".tmp")),"failed write cleans temporary file");}
  Files.delete(sentinel);Files.delete(blocked);failure.scale(3);
  check(failure.warning().isEmpty()&&new SettingsStore(blocked).scale()==3,"later successful save clears warning");
  System.out.println("Persistent settings: "+checks+" checks passed");
 }
}
