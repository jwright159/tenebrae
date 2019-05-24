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
import org.luaj.vm2.lib.jse.*;
import com.badlogic.gdx.audio.*;
import com.leff.midi.*;
import com.leff.midi.event.*;

public class Mappack implements ScriptGlob{
	public FileHandle folder;
	public String name = "Mapppack lololol", description, startMapTmx, startMapLua;
	private Globals globals;
	public Array<Character> charas;

	public Mappack(FileHandle folder){
		this.folder = folder;
		charas = new Array<Character>();
		globals = new StdGlobals();
		globals.load(new MappackLib());
	}
	public TileMap loadMap(String tmx, String lua){
		return new TileMap(tmx == null ? null : folder.child(tmx), Tenebrae.t.getScript(lua, globals), Tenebrae.t.getStage().getBatch());
	}
	public TileMap loadMap(){
		return loadMap(startMapTmx, startMapLua);
	}
	public MenuItem.GameItem loadItem(String lua, Character owner){
		return new MenuItem.GameItem(Utils.filename(lua), Tenebrae.t.getProto(lua), owner, Tenebrae.t.getSkin());
	}
	public TiledMapTileSet loadTileset(String name){
		return TiledMapTileSetLoader.loadTileSet(folder.child(name), null);
	}
	public NPC loadNPC(String lua){
		NPC npc = new NPC(Utils.filename(lua), Tenebrae.t.getProto(lua));
		charas.add(npc);
		if(Tenebrae.player.map != null)
			npc.changeMap(Tenebrae.player.map);
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
			library.set("setMap", new TwoArgFunction(){
					private float deadzone = -1;
					@Override
					public LuaValue call(LuaValue tmx, LuaValue lua){
						Player p = Tenebrae.player;
						if(tmx.isnil() || (lua.isnil() && tmx.isstring())){
							p.setBound(0, 0,
								p.bigdzRect.width * Tenebrae.t.getCamera().zoom / p.map.tileWidth,
								p.bigdzRect.height * Tenebrae.t.getCamera().zoom / p.map.tileHeight);
							if(deadzone == -1){
								deadzone = p.deadzone;
								p.deadzone = 0;
							}
							p.changeMap(Tenebrae.mp.loadMap(null, tmx.isnil() ? lua.checkjstring() : tmx.checkjstring()));
						}else{
							p.setBound(-1, -1, -1, -1);
							if(deadzone != -1){
								p.deadzone = deadzone;
								deadzone = -1;
							}
							if(lua.isnil() && tmx.istable())
								p.changeMap(Tenebrae.mp.loadMap(tmx.checkjstring(1), tmx.checkjstring(2)));
							else
								p.changeMap(Tenebrae.mp.loadMap(tmx.checkjstring(), lua.checkjstring()));
						}
						return NONE;
					}
				});
			library.set("enableTrigger", new OneArgFunction(){
					@Override
					public LuaValue call(LuaValue trigger){
						Tenebrae.player.map.getMapObject(trigger.checkjstring()).getProperties().put("disabled", false);
						return NONE;
					}
				});
			library.set("disableTrigger", new OneArgFunction(){
					@Override
					public LuaValue call(LuaValue trigger){
						Tenebrae.player.map.getMapObject(trigger.checkjstring()).getProperties().put("disabled", true);
						return NONE;
					}
				});
			library.set("setTrigger", new ThreeArgFunction(){
					@Override
					public LuaValue call(LuaValue triggerObj, LuaValue type, LuaValue function){
						Tenebrae.player.map.getMapObject(triggerObj.checkjstring()).getProperties().put(type.optjstring("onTrigger"), function.checkfunction());
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
					public Varargs invoke(Varargs args){ // [x, y | entity], time, interpolation, tapOverride
						OrthographicCamera cam = Tenebrae.t.getCamera();
						if(args.isnumber(0))
							Tenebrae.player.addAction(new Action.CameraAction(cam,
									(float)args.optdouble(1, 0) * Tenebrae.player.map.tileWidth,
									(float)args.optdouble(2, 0) * Tenebrae.player.map.tileHeight,
									Utils.getInterpolation(args.optjstring(4, "constant")),
									(float)args.optdouble(3, 0),
									args.optboolean(5, false)));
						else
							Tenebrae.player.addAction(new Action.CameraAction(cam,
									(Character)args.checktable(1).get("__this").checkuserdata(Character.class),
									Utils.getInterpolation(args.optjstring(3, "constant")),
									(float)args.optdouble(2, 0),
									args.optboolean(4, false)));
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
						return loadNPC(name.checkjstring()).getGlobals();
					}
				});
			LuaTable ent = tableOf();
			library.set("Entity", ent);
			ent.setmetatable(tableOf());
			ent.getmetatable().set(CALL, new VarArgFunction(){
					@Override
					public Varargs invoke(Varargs args){ // self(?), x, y, width, height
						return new Entity((float)args.checkdouble(2), (float)args.checkdouble(3), (float)args.checkdouble(4), (float)args.checkdouble(5), Tenebrae.player.map.tileWidth, Tenebrae.player.map.tileHeight, Tenebrae.t.getSkin().getRegion("white")).vars;
					}
				});
			ent.set("add", new OneArgFunction(){
					@Override
					public LuaValue call(LuaValue ent){
						Entity e = (Entity)ent.getmetatable().get(Entity.ENTITY).checkuserdata(Entity.class);
						Log.verbose("Added", e);
						Tenebrae.player.map.addEntity(e);
						return NONE;
					}
				});
			ent.set("remove", new OneArgFunction(){
					@Override
					public LuaValue call(LuaValue ent){
						Entity e = (Entity)ent.getmetatable().get(Entity.ENTITY).checkuserdata(Entity.class);
						e.remove();
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
			LuaTable music = tableOf();
			library.set("Music", music);
			music.setmetatable(tableOf());
			music.getmetatable().set(CALL, new ThreeArgFunction(){
					@Override
					public LuaValue call(LuaValue self, LuaValue musicfile, LuaValue midifile){
						Music music = Gdx.audio.newMusic(folder.child(musicfile.checkjstring()));
						music.setLooping(true);
						MidiFile midi = null;
						if(!midifile.isnil())
							try{
								midi = new MidiFile(folder.child(midifile.checkjstring()).file());
							}catch(IOException|FileNotFoundException ex){
								throw new RuntimeException("Unable to load midi file", ex);
							}
						MusicWrapper wrapper = new MusicWrapper(music, midi);
						return wrapper.vars;
					}
				});

			//attributes
			library.setmetatable(tableOf());
			library.getmetatable().set(INDEX, new TwoArgFunction(){
					@Override
					public LuaValue call(LuaValue self, LuaValue key){
						//Log.debug("Getting", key.checkjstring());
						switch(key.checkjstring()){
							case "name":
								return valueOf(Mappack.this.name);
							case "description":
								return valueOf(description);
							case "startMap":
								LuaTable startMap = tableOf();
								startMap.set(1, startMapTmx);
								startMap.set(2, startMapLua);
								return startMap;
							case "player":
								return Tenebrae.player.getGlobals();
							case "cameraX":
								return valueOf(Tenebrae.t.getCamera().position.x / Tenebrae.player.map.tileWidth);
							case "cameraY":
								return valueOf(Tenebrae.t.getCamera().position.y / Tenebrae.player.map.tileHeight);
							case "cameraZoom":
								return valueOf(Tenebrae.t.getCamera().zoom);
							case "screen":
								return screen.getVars();
							case "screenWidth":
								return valueOf(Tenebrae.player.activeDeadzone.width * Tenebrae.t.getCamera().zoom / Tenebrae.player.map.tileWidth);
							case "screenHeight":
								return valueOf(Tenebrae.player.activeDeadzone.height * Tenebrae.t.getCamera().zoom / Tenebrae.player.map.tileHeight);
							case "x":
								return valueOf(Tenebrae.player.getBound().getX());
							case "y":
								return valueOf(Tenebrae.player.getBound().getY());
							case "width":
								return valueOf(Tenebrae.player.getBound().getWidth());
							case "height":
								return valueOf(Tenebrae.player.getBound().getHeight());
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
								if(value.isstring()){
									startMapTmx = null;
									startMapLua = value.checkjstring();
								}else{
									startMapTmx = value.get(1).checkjstring();
									startMapLua = value.get(2).checkjstring();
								}
								break;
							case "player":
								self.error("player is read-only");
								break;
							case "cameraX":
								Tenebrae.t.getCamera().position.x = (float)value.checkdouble() * Tenebrae.player.map.tileWidth;
								break;
							case "cameraY":
								Tenebrae.t.getCamera().position.y = (float)value.checkdouble() * Tenebrae.player.map.tileHeight;
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
							case "x":
								Rectangle bounds = Tenebrae.player.getBound();
								Tenebrae.player.setBound((float)value.checkdouble(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
								break;
							case "y":
								bounds = Tenebrae.player.getBound();
								Tenebrae.player.setBound(bounds.getX(), (float)value.checkdouble(), bounds.getWidth(), bounds.getHeight());
								break;
							case "width":
								bounds = Tenebrae.player.getBound();
								Tenebrae.player.setBound(bounds.getX(), bounds.getY(), (float)value.checkdouble(), bounds.getHeight());
								break;
							case "height":
								bounds = Tenebrae.player.getBound();
								Tenebrae.player.setBound(bounds.getX(), bounds.getY(), bounds.getWidth(), (float)value.checkdouble());
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

	public static class MusicWrapper{
		public static String MUSIC = "__music";

		public MusicMidiSync music;
		public LuaTable vars;
		public MusicWrapper(Music mus, MidiFile midi){
			this.music = new MusicMidiSync(mus, midi, new MidiEventListener(){
					@Override
					public void onStart(boolean fromBeginning){
						// TODO: Implement this method
					}
					@Override
					public void onEvent(MidiEvent event, long ms){
						final NoteOn note = (NoteOn)event;
						Log.verbose2("On!", note, note.getChannel(), note.getTick());
						final LuaTable levent = LuaValue.tableOf();
						levent.set("time", ms / 1000f);
						levent.set("velocity", note.getVelocity());
						levent.set("note", note.getNoteValue());
						levent.set("tick", note.getTick());
						levent.set("channel", note.getChannel());

						Gdx.app.postRunnable(new Runnable(){
								@Override
								public void run(){
									LuaValue onNote = vars.get("onNote");
									if(!onNote.isnil())
										onNote.call(vars, levent);

									onNote = vars.get("onNote" + (note.getChannel() + 1));
									if(!onNote.isnil())
										onNote.call(vars, levent);
								}
							});
					}
					@Override
					public void onStop(boolean finished){
						// TODO: Implement this method
					}
				}, new MidiEventListener(){
					@Override
					public void onStart(boolean fromBeginning){
						// TODO: Implement this method
					}
					@Override
					public void onEvent(MidiEvent event, long ms){
						final NoteOff note = (NoteOff)event;
						Log.verbose2("Off!", note, note.getChannel(), note.getTick());
						final LuaTable levent = LuaValue.tableOf();
						levent.set("time", ms / 1000f);
						levent.set("velocity", note.getVelocity());
						levent.set("note", note.getNoteValue());
						levent.set("tick", note.getTick() - note.getDelta());
						levent.set("channel", note.getChannel());

						Gdx.app.postRunnable(new Runnable(){
								@Override
								public void run(){
									LuaValue offNote = vars.get("offNote");
									if(!offNote.isnil())
										offNote.call(vars, levent);

									offNote = vars.get("offNote" + (note.getChannel() + 1));
									if(!offNote.isnil())
										offNote.call(vars, levent);
								}
							});
					}
					@Override
					public void onStop(boolean finished){
						// TODO: Implement this method
					}
				});

			vars = LuaValue.tableOf();
			vars.set("play", new ZeroArgFunction(){
					@Override
					public LuaValue call(){
						music.play();
						return NONE;
					}
				});
			vars.set("pause", new ZeroArgFunction(){
					@Override
					public LuaValue call(){
						music.pause();
						return NONE;
					}
				});
			vars.set("stop", new ZeroArgFunction(){
					@Override
					public LuaValue call(){
						music.stop();
						return NONE;
					}
				});
			vars.set("dispose", new ZeroArgFunction(){
					@Override
					public LuaValue call(){
						music.dispose();
						return NONE;
					}
				});
			vars.setmetatable(LuaValue.tableOf());
			vars.getmetatable().set(MUSIC, CoerceJavaToLua.coerce(music));
			vars.getmetatable().set(LuaValue.INDEX, new TwoArgFunction(){
					@Override
					public LuaValue call(LuaValue self, LuaValue key){
						switch(key.checkjstring()){
							case "position":
								return valueOf(music.getPosition());
							case "volume":
								return valueOf(music.getVolume());
							case "isPlaying":
								return valueOf(music.isPlaying());
							default:
								return self.rawget(key);
						}
					}
				});
			vars.getmetatable().set(LuaValue.NEWINDEX, new ThreeArgFunction(){
					@Override
					public LuaValue call(LuaValue self, LuaValue key, LuaValue value){
						switch(key.checkjstring()){
							case "position":
								music.setPosition((float)value.checkdouble());
								break;
							case "volume":
								music.setVolume((float)value.checkdouble());
								break;
							case "isPlaying":
								self.error("isPlaying is read-only");
								break;
							default:
								self.rawset(key, value);
						}
						return NONE;
					}
				});
		}
	}
}
