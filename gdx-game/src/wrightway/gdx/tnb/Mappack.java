package wrightway.gdx.tnb;

import wrightway.gdx.*;
import com.badlogic.gdx.*;
import com.badlogic.gdx.files.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.maps.*;
import com.badlogic.gdx.maps.tiled.*;
import com.badlogic.gdx.maps.tiled.tiles.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.XmlReader.*;
import java.io.*;
import com.badlogic.gdx.math.*;
import com.leff.midi.util.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.*;
import java.util.*;
import com.badlogic.gdx.maps.objects.*;
import com.badlogic.gdx.maps.tiled.objects.*;

public class Mappack implements ScriptGlob{
	public FileHandle folder;
	public String name = "Mapppack lololol", description, startMap;
	private Globals globals;
	public Array<Character> charas;
	private static final Comparator<Character> charaComp = new Comparator<Character>(){
		@Override
		public int compare(Character p1, Character p2){
			return p1.y > p2.y ? -1 : 1;
		}
	};

	public Mappack(FileHandle folder){
		this.folder = folder;
		charas = new Array<Character>(){
			@Override
			public void sort(){
				sort(charaComp);
			}
		};
		globals = new StdGlobals();
		globals.load(new MappackLib());
	}
	public TileMap loadMap(String name){
		return new TileMap(folder.child(name + ".tmx"), Tenebrae.t.getScript(name, globals));
	}
	public TileMap loadMap(){
		return loadMap(startMap);
	}
	public Enemy loadEnemy(String name){
		//return new Enemy(folder.child(name + ".tnb"), this);
		throw new UnsupportedOperationException("Not usin this rn");
	}
	public MenuItem.GameItem loadItem(String name, Character owner){
		return new MenuItem.GameItem(name, Tenebrae.t.getProto(name), owner, Tenebrae.t.getSkin());
	}
	public TiledMapTileSet loadTileset(String name){
		return TiledMapTileSetLoader.loadTileSet(folder.child(name + ".tsx"), null);
	}
	public MidiWavSync loadMidiWav(String name, MidiEventListener listener){
		return new MidiWavSync(folder.child(name + ".mid"), folder.child(name + ".wav"), listener);
	}
	public Projectile loadProjectile(String name){
		throw new UnsupportedOperationException("Give me bit to add textures.");
		//TiledMapTileSet ts = loadTileset(name);
		//Tenebrae.debug("Projts", ts, ts.getTile(1), ts.getTile(2));
		//return new Projectile(folder.child(name+".tnb"), ts.getTile(1));
	}
	public EnemyWeapon loadEnemyWeapon(String name, Character owner){
		EnemyWeapon rtn = new EnemyWeapon(owner, name);
		Tenebrae.t.getScript(name, rtn.getGlobals()).call();
		return rtn;
	}
	public NPC loadNPC(String name, Stage stage){
		NPC npc = new NPC(name, Tenebrae.t.getProto(name));
		charas.add(npc);
		stage.addActor(npc);
		if(Tenebrae.player.map != null)
			npc.changeMap(Tenebrae.player.map, -1, -1);
		return npc;
	}

	@Override
	public Globals getGlobals(){
		return globals;
	}

	public class MappackLib extends TwoArgFunction{
		@Override
		public LuaValue call(LuaValue modname, LuaValue env){
			LuaTable library = tableOf();
			//functions
			library.set("setTile", new VarArgFunction(){
					@Override
					public Varargs invoke(Varargs args){// x, y, layer, tileset, tileid
						Tenebrae.player.map.changeTile(args.checkint(1),
							(Tenebrae.player.map.height - 1) - args.checkint(2),
							args.checkjstring(3),
							args.checkjstring(4),
							args.checkint(5));
						return NONE;
					}
				});
			library.set("say", new VarArgFunction(){
					@Override
					public Varargs invoke(Varargs args){ // text, delayTime, charsPerSec, tapToSkip
						String text = args.checkjstring(1);
						float delay = (float)args.optdouble(2, -1), cps = (float)args.optdouble(3, -1);
						boolean tapDelay = args.toboolean(4);
						Log.debug(text, delay, cps);
						if(delay == -1 && cps == -1){
							Tenebrae.player.addDialog(text);
						}else if(cps == -1){
							Tenebrae.player.addDialog(text, delay, false, tapDelay);
						}else{
							for(int j = 1; j < text.length(); j++){
								String substring = text.substring(0, j);
								//Tenebrae.debug("Say spell out! "+spellOut+" "+substring);
								if(Utils.endsInWhitespace(substring))
									continue;
								boolean punc = (Utils.endsInPunctuation(substring) && !Utils.endsInPunctuation(text.substring(0, j + 1 <= text.length() ? j + 1 : j)) // if it ends in punctuation and the next char doesn't
									&& !text.substring(0, j + 1 <= text.length() ? j + 1 : j).endsWith("\""))														  // and next char isn't quote
									|| (Utils.endsInPunctuation(substring.substring(0, substring.length() == 1 ? 1 : substring.length() - 1))						  // OR last char was punc
									&& substring.endsWith("\""));																									  // and it ends in quote
								//if(punc)
								//Tenebrae.debug("Punctuation! " + substring);
								Tenebrae.player.addDialog(substring, (punc ? 5f : 1f) / cps, false, tapDelay);
							}
							if(delay != -1)
								Tenebrae.player.addDialog(text, delay);
							else
								Tenebrae.player.addDialog(text);
						}
						return NONE;
					}
				});
			library.set("setMap", new ThreeArgFunction(){
					@Override
					public LuaValue call(LuaValue mapName, LuaValue startX, LuaValue startY){
						Tenebrae.player.changeMap(Tenebrae.mp.loadMap(mapName.checkjstring()),
							(float)startX.optdouble(-1),
							(float)startY.optdouble(-1));
						return NONE;
					}
				});
			library.set("enableTrigger", new OneArgFunction(){
					@Override
					public LuaValue call(LuaValue trigger){
						Tenebrae.player.map.getTriggerObjects().get(trigger.checkjstring()).getProperties().put("disabled", false);
						return NONE;
					}
				});
			library.set("disableTrigger", new OneArgFunction(){
					@Override
					public LuaValue call(LuaValue trigger){
						Tenebrae.player.map.getTriggerObjects().get(trigger.checkjstring()).getProperties().put("disabled", true);
						return NONE;
					}
				});
			library.set("setTrigger", new ThreeArgFunction(){
					@Override
					public LuaValue call(LuaValue triggerObj, LuaValue type, LuaValue function){
						Tenebrae.player.map.getTriggerObjects().get(triggerObj.checkjstring()).getProperties().put(type.optjstring("onTrigger"), function.checkfunction());
						return NONE;
					}
				});
			library.set("zoom", new VarArgFunction(){
					@Override
					public Varargs invoke(Varargs args){ // amount, time, interpolation, tapOverride
						OrthographicCamera cam = Tenebrae.t.getCamera();
						Tenebrae.player.addAction(new Action.CameraAction(cam,
								cam.zoom * (float)args.optdouble(1, 1),
								Utils.getInterpolation(args.optjstring(3, "constant")),
								(float)args.optdouble(2, 0),
								args.optboolean(4, false)));
						return NONE;
					}
				});
			library.set("pan", new VarArgFunction(){
					@Override
					public Varargs invoke(Varargs args){ // x, y, time, interpolation, tapOverride
						OrthographicCamera cam = Tenebrae.t.getCamera();
						Tenebrae.player.addAction(new Action.CameraAction(cam,
								cam.position.x + (float)args.optdouble(1, 0) * Tenebrae.player.map.tileWidth,
								cam.position.y + (float)args.optdouble(2, 0) * Tenebrae.player.map.tileHeight,
								Utils.getInterpolation(args.optjstring(4, "constant")),
								(float)args.optdouble(3, 0),
								args.optboolean(5, false)));
						return NONE;
					}
				});
			library.set("panTo", new VarArgFunction(){
					@Override
					public Varargs invoke(Varargs args){ // x, y, time, interpolation, tapOverride
						OrthographicCamera cam = Tenebrae.t.getCamera();
						Tenebrae.player.addAction(new Action.CameraAction(cam,
								(float)args.optdouble(1, 0) * Tenebrae.player.map.tileWidth - Tenebrae.player.activeDeadzone.width / 2 - Tenebrae.player.activeDeadzone.x + Tenebrae.screenRect.width / 2,
								(float)args.optdouble(2, 0) * Tenebrae.player.map.tileHeight - Tenebrae.player.activeDeadzone.height / 2 - Tenebrae.player.activeDeadzone.y + Tenebrae.screenRect.height / 2,
								Utils.getInterpolation(args.optjstring(4, "constant")),
								(float)args.optdouble(3, 0),
								args.optboolean(5, false)));
						return NONE;
					}
				});
			library.set("resetCamera", new ThreeArgFunction(){
					@Override
					public LuaValue call(LuaValue time, LuaValue interpolation, LuaValue tapOverride){
						OrthographicCamera cam = Tenebrae.t.getCamera();
						Vector3 l = Tenebrae.player.lastCameraPos;
						Tenebrae.player.addAction(new Action.CameraAction(cam,
								l.x,
								l.y,
								l.z,
								Utils.getInterpolation(interpolation.optjstring("constant")),
								(float)time.optdouble(0),
								tapOverride.optboolean(false)));
						return NONE;
					}
				});
			library.set("delay", new TwoArgFunction(){
					@Override
					public LuaValue call(LuaValue time, LuaValue function){
						Tenebrae.player.addDelay((float)time.optdouble(0), function.checkfunction());
						return NONE;
					}
				});
			library.set("NPC", new OneArgFunction(){
					@Override
					public LuaValue call(LuaValue name){
						return loadNPC(name.checkjstring(), Tenebrae.t.getStage()).getGlobals();
					}
				});
			LuaTable ent = tableOf();
			library.set("Entity", ent);
			ent.setmetatable(tableOf());
			ent.getmetatable().set(CALL, new ZeroArgFunction(){
					@Override
					public LuaValue call(){
						return new Entity().vars;
					}
				});
			ent.set("add", new OneArgFunction(){
					@Override
					public LuaValue call(LuaValue ent){
						Entity e = (Entity)ent.getmetatable().get(Entity.ENTITY).checkuserdata(Entity.class);
						Tenebrae.t.getStage().addActor(e.debug());
						return NONE;
					}
				});
			ent.set("closeOn", new VarArgFunction(){
					@Override
					public Varargs invoke(Varargs args){ // x, y, targetX, targetY, speed, delta
						float dx = (float)(args.checkdouble(3) - args.checkdouble(1)), dy = (float)(args.checkdouble(4) - args.checkdouble(2));
						float mag = (float)Math.hypot(dx, dy);
						if(mag <= (float)(args.checkdouble(5) * args.checkdouble(6)))
							return varargsOf(args.arg(3), args.arg(4));
						dx /= mag; dy /= mag;
						return varargsOf(valueOf(args.checkdouble(1) + dx * args.checkdouble(5) * args.checkdouble(6)), valueOf(args.checkdouble(2) + dy * args.checkdouble(5) * args.checkdouble(6)));
					}
				});

			//attributes
			library.setmetatable(tableOf());
			library.getmetatable().set(INDEX, new TwoArgFunction(){
					@Override
					public LuaValue call(LuaValue self, LuaValue key){
						Log.debug("Getting", key.checkjstring());
						switch(key.checkjstring()){
							case "name":
								return valueOf(Mappack.this.name);
							case "description":
								return valueOf(description);
							case "startMap":
								return valueOf(startMap);
							case "player":
								return Tenebrae.player.getGlobals();
							case "cameraX":
								return valueOf(Tenebrae.t.getCamera().position.x - Tenebrae.screenRect.width / 2 + Tenebrae.player.activeDeadzone.x + Tenebrae.player.activeDeadzone.width / 2);
							case "cameraY":
								return valueOf(Tenebrae.t.getCamera().position.y - Tenebrae.screenRect.height / 2 + Tenebrae.player.activeDeadzone.y + Tenebrae.player.activeDeadzone.height / 2);
							case "cameraZoom":
								return valueOf(Tenebrae.t.getCamera().zoom);
							case "screen":
								return screen.getVars();
							case "screenWidth":
								return valueOf(Tenebrae.player.activeDeadzone.width * Tenebrae.t.getCamera().zoom);
							case "screenHeight":
								return valueOf(Tenebrae.player.activeDeadzone.height * Tenebrae.t.getCamera().zoom);
							default:
								return self.rawget(key);
						}
					}
				});
			library.getmetatable().set(NEWINDEX, new ThreeArgFunction(){
					@Override
					public LuaValue call(LuaValue self, LuaValue key, LuaValue value){
						switch(key.checkjstring()){
							case "name":
								Mappack.this.name = value.checkjstring();
								break;
							case "description":
								description = value.checkjstring();
								break;
							case "startMap":
								startMap = value.checkjstring();
								break;
							case "player":
								self.error("player is read-only");
								break;
							case "cameraX":
								Tenebrae.t.getCamera().position.x = (float)value.checkdouble() - Tenebrae.player.activeDeadzone.width / 2 - Tenebrae.player.activeDeadzone.x + Tenebrae.screenRect.width / 2;
								break;
							case "cameraY":
								Tenebrae.t.getCamera().position.y = (float)value.checkdouble() - Tenebrae.player.activeDeadzone.height / 2 - Tenebrae.player.activeDeadzone.y + Tenebrae.screenRect.height / 2;
								break;
							case "cameraZoom":
								Tenebrae.t.getCamera().zoom = (float)value.checkdouble();
								break;
							case "screen":
								self.error("screen dimensions are read-only");
								break;
							case "screenWidth":
								self.error("screen dimensions are read-only");
								break;
							case "screenHeight":
								self.error("screen dimensions are read-only");
								break;
							default:
								self.rawset(key, value);
						}
						return NONE;
					}
				});

			//constructors
			/*final LuaTable npcClass = tableOf();
			 npcClass.set(INDEX, npcClass); // instances will look up stuff in class. npcInstance will have npcClass, not npcInstance, so no inf recurse
			 npcClass.set("new", new TwoArgFunction(){
			 @Override
			 public LuaValue call(LuaValue clazz, LuaValue name){
			 LuaTable instance = loadNPC(name.checkjstring(), Tenebrae.t.getStage()).getGlobals();
			 instance.setmetatable(clazz); // Actually this line is killing the inherited metatables
			 return instance;
			 }
			 });
			 library.set("NPC", npcClass);*/

			ScriptGlob.S.setLibToEnv(library, env);
			return env;
		}
	}

	public ScreenDims screen = new ScreenDims();
	public static class ScreenDims{
		private LuaTable vars;
		public ScreenDims(){
			vars = LuaValue.tableOf();
			if(vars.getmetatable() == null) vars.setmetatable(LuaValue.tableOf());
			vars.getmetatable().set(LuaValue.INDEX, new TwoArgFunction(){
					@Override
					public LuaValue call(LuaValue self, LuaValue key){
						switch(key.checkjstring()){
							case "x":
								return valueOf(Tenebrae.screenRect.getX());
							case "y":
								return valueOf(Tenebrae.screenRect.getY());
							case "width":
								return valueOf(Tenebrae.screenRect.getWidth());
							case "height":
								return valueOf(Tenebrae.screenRect.getHeight());
							default:
								return self.rawget(key);
						}
					}
				});
			vars.getmetatable().set(LuaValue.NEWINDEX, new ThreeArgFunction(){
					@Override
					public LuaValue call(LuaValue self, LuaValue key, LuaValue value){
						self.error("screen dimensions are read-only");
						return NONE;
					}
				});
		}
		public LuaTable getVars(){
			return vars;
		}
	}
}
