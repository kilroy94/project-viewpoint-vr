package viewpointvr.diagnostic;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;
import viewpointvr.input.TurnSettings;
/** Only preferences are persisted; runtime/controller activation is deliberately absent. */
public final class SettingsStore {
 private final Path file;
 private TurnSettings turning=TurnSettings.DEFAULT;
 private float scale=1;
 private String warning="";
 public SettingsStore(Path file){this.file=file;load();}
 public synchronized TurnSettings turning(){return turning;}
 public synchronized float scale(){return scale;}
 public synchronized String warning(){return warning;}
 private void failed(String message,Exception error){warning=message;System.err.println("[Project Viewpoint VR Settings] "+message+": "+error);}
 private void load(){
  var props=new Properties();
  try(var reader=Files.newBufferedReader(file)){props.load(reader);}
  catch(NoSuchFileException absent){return;}
  catch(IOException|IllegalArgumentException|SecurityException error){failed("Settings could not be loaded; using defaults",error);return;}
  int mode=integer(props,"turnMode",1),angle=integer(props,"snapAngle",30),speed=integer(props,"smoothSpeed",90);
  if(mode<0||mode>2)mode=1;
  if(angle!=15&&angle!=30&&angle!=45&&angle!=60&&angle!=90)angle=30;
  if(speed<30||speed>240||speed%15!=0)speed=90;
  turning=new TurnSettings(mode,angle,speed);
  try{float value=Float.parseFloat(props.getProperty("worldScale","1"));if(Float.isFinite(value)&&value>=.25f&&value<=4)scale=value;}catch(NumberFormatException ignored){}
 }
 private static int integer(Properties props,String key,int fallback){try{return Integer.parseInt(props.getProperty(key,Integer.toString(fallback)));}catch(NumberFormatException error){return fallback;}}
 public synchronized void turning(TurnSettings value){turning=java.util.Objects.requireNonNull(value);save();}
 public synchronized void scale(double value){if(!Double.isFinite(value)||value<.25||value>4)throw new IllegalArgumentException("Invalid world scale");scale=(float)value;save();}
 private void save(){
  Path temp=null;
  try{
   Path target=file.toAbsolutePath();Files.createDirectories(target.getParent());
   temp=Files.createTempFile(target.getParent(),"settings-",".tmp");
   var props=new Properties();props.setProperty("turnMode",Integer.toString(turning.mode()));props.setProperty("snapAngle",Integer.toString(turning.angle()));
   props.setProperty("smoothSpeed",Integer.toString(turning.speed()));props.setProperty("worldScale",Float.toString(scale));
   try(var writer=Files.newBufferedWriter(temp)){props.store(writer,"Project Viewpoint VR preferences");}
   try{Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
   catch(AtomicMoveNotSupportedException unsupported){Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);}
   warning="";
  }catch(IOException|SecurityException error){failed("Settings not saved; current choices apply for this session",error);}
  finally{if(temp!=null)try{Files.deleteIfExists(temp);}catch(IOException|SecurityException ignored){}}
 }
}
