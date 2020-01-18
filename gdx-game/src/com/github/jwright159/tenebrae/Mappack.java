package com.github.jwright159.tenebrae;

import com.github.jwright159.gdx.*;
import com.github.jwright159.gdx.tiled.*;
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
import com.badlogic.gdx.utils.viewport.*;
import org.json.*;
import com.github.jwright159.gdx.actor.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import com.github.jwright159.gdx.graphics.*;

public class Mappack implements ScriptGlob, Disposable{
	private Tenebrae game;
	public FileHandle folder;
	public String startMapTmx, startMapLua;
	private Globals globals;
	public Array<Character> charas;
	private ObjectMap<String,TiledMapTileSet> tilesets;
	
	private float lastdeadzone = -1;

	public Mappack(Tenebrae game, FileHandle folder){
		this.game = game;
		this.folder = folder;
		charas = new Array<Character>();
		tilesets = new ObjectMap<String,TiledMapTileSet>();
		globals = new StdGlobals(game.mappackpath);
		globals.load(new MappackLib());
	}
	public TileMap loadMap(String tmx, String lua){
		if(tmx == null){
			TileMap map = new TileMap(game, null, game.getScript(lua, globals), game.getStage().getBatch());
			map.setBoundToBigDZ();
			if(lastdeadzone == -1){
				lastdeadzone = game.player.deadzone;
				game.player.deadzone = 0;
			}
			return map;
		}else{
			TileMap map = new TileMap(game, folder.child(tmx), game.getScript(lua, globals), game.getStage().getBatch());
			map.setBound(-1, -1, -1, -1);
			if(lastdeadzone != -1){
				game.player.deadzone = lastdeadzone;
				lastdeadzone = -1;
			}
			return map;
		}
	}
	public TileMap loadMap(){
		return loadMap(startMapTmx, startMapLua);
	}
	public MenuItem.GameItem loadItem(String lua, Character owner){
		return new MenuItem.GameItem(game, Utils.filename(lua), game.getProto(lua), owner, game.getSkin());
	}
	public TiledMapTileSet loadTileset(String tsx){
		TiledMapTileSet tileset = null;
		if(tilesets.containsKey(tsx))
			tileset = tilesets.get(tsx);
		else
			tilesets.put(tsx, tileset = TiledMapTileSetLoader.loadTileSet(folder.child(tsx), null));
		return tileset;
	}
	public NPC loadNPC(String lua){
		NPC npc = new NPC(game, Utils.filename(lua), game.getProto(lua));
		charas.add(npc);
		if(game.map != null)
			npc.changeMap(game.map);
		return npc;
	}

	@Override
	public Globals getGlobals(){
		return globals;
	}

	public void loadSaveState(JSONObject savestate){
		Log.debug("Loading", savestate);
		LuaTable save = LuaValue.tableOf();
		Iterator<String> keys = savestate.keys();
		while(keys.hasNext()){
			String key = keys.next();
			try{
				save.set(key, savestate.get(key).toString());
			}catch(JSONException ex){
				Log.error(ex);
			}
		}
		globals.set("savestate", save);
	}
	public JSONObject saveSaveState(){
		LuaTable save = globals.get("savestate").checktable();
		JSONObject savestate = new JSONObject();
		for(LuaValue key : save.keys()){
			try{
				savestate.put(key.checkjstring(), save.get(key));
			}catch(JSONException ex){
				Log.error(ex);
			}
		}
		Log.debug("Saving", savestate);
		return savestate;
	}

	@Override
	public void dispose(){
		Array<Texture> disposed = new Array<Texture>();
		ObjectMap.Values<TiledMapTileSet> tilesets = this.tilesets.values();
		while(tilesets.hasNext())
			for(TiledMapTile tile : tilesets.next()){
				Texture tex = tile.getTextureRegion().getTexture();
				if(!disposed.contains(tex, true)){
					tex.dispose();
					disposed.add(tex);
				}
			}
	}

	private static Vector2 tmp = new Vector2();
	public class MappackLib extends TwoArgFunction{
		@Override
		public LuaValue call(LuaValue modname, LuaValue env){
			LuaTable library = tableOf();
			//functions
			library.set("setTile", new VarArgFunction(){
					@Override
					public Varargs invoke(Varargs args){// x, y, layer, tileset, tileid
						game.map.changeTile(args.checkint(1),
							(game.map.height - 1) - args.checkint(2),
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
							game.player.addDialog(text);
						}else if(cps == -1){
							game.player.addDialog(text, delay, false, tapDelay);
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
								game.player.addDialog(substring, (punc ? 5f : 1f) / cps, false, tapDelay);
							}
							if(delay != -1)
								game.player.addDialog(text, delay);
							else
								game.player.addDialog(text);
						}
						return NONE;
					}
				});
			library.set("setMap", new TwoArgFunction(){
					@Override
					public LuaValue call(LuaValue tmx, LuaValue lua){
						if(tmx.isnil() || (lua.isnil() && tmx.isstring())){
							game.player.changeMap(loadMap(null, tmx.isnil() ? lua.checkjstring() : tmx.checkjstring()));
						}else{
							if(lua.isnil() && tmx.istable())
								game.player.changeMap(loadMap(tmx.get(1).checkjstring(), tmx.get(2).checkjstring()));
							else
								game.player.changeMap(loadMap(tmx.checkjstring(), lua.checkjstring()));
						}
						return NONE;
					}
				});
			library.set("enableTrigger", new OneArgFunction(){
					@Override
					public LuaValue call(LuaValue trigger){
						game.map.getMapObject(trigger.checkjstring()).getProperties().put("disabled", false);
						return NONE;
					}
				});
			library.set("disableTrigger", new OneArgFunction(){
					@Override
					public LuaValue call(LuaValue trigger){
						game.map.getMapObject(trigger.checkjstring()).getProperties().put("disabled", true);
						return NONE;
					}
				});
			library.set("setTrigger", new ThreeArgFunction(){
					@Override
					public LuaValue call(LuaValue triggerObj, LuaValue type, LuaValue function){
						game.map.getMapObject(triggerObj.checkjstring()).getProperties().put(type.optjstring("onTrigger"), function.checkfunction());
						return NONE;
					}
				});
			library.set("zoom", new VarArgFunction(){
					@Override
					public Varargs invoke(Varargs args){ // amount, time, interpolation, tapOverride
						OrthographicCamera cam = game.getCamera();
						game.player.addAction(new Action.CameraAction(game, cam,
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
						OrthographicCamera cam = game.getCamera();
						game.player.addAction(new Action.CameraAction(game, cam,
								cam.position.x + (float)args.optdouble(1, 0),
								cam.position.y + (float)args.optdouble(2, 0),
								Utils.getInterpolation(args.optjstring(4, "constant")),
								(float)args.optdouble(3, 0),
								args.optboolean(5, false)));
						return NONE;
					}
				});
			library.set("panTo", new VarArgFunction(){
					@Override
					public Varargs invoke(Varargs args){ // [x, y | entity], time, interpolation, tapOverride
						OrthographicCamera cam = game.getCamera();
						if(args.isnumber(0))
							game.player.addAction(new Action.CameraAction(game, cam,
									(float)args.optdouble(1, 0),
									(float)args.optdouble(2, 0),
									Utils.getInterpolation(args.optjstring(4, "constant")),
									(float)args.optdouble(3, 0),
									args.optboolean(5, false)));
						else
							game.player.addAction(new Action.CameraAction(game, cam,
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
						OrthographicCamera cam = game.getCamera();
						Vector3 l = game.player.lastCameraPos;
						game.player.addAction(new Action.CameraAction(game,
								cam,
								l.x,
								l.y,
								l.z,
								Utils.getInterpolation(interpolation.optjstring("constant")),
								(float)time.optdouble(0),
								tapOverride.optboolean(false)));
						return NONE;
					}
				});
			library.set("setResolution", new TwoArgFunction(){
					@Override
					public LuaValue call(LuaValue width, LuaValue height){
						game.setViewport(new FitViewport((float)width.checkdouble(), (float)height.checkdouble()));
						return NONE;
					}
				});
			library.set("applyInterp", new VarArgFunction(){
				@Override
				public Varargs invoke(Varargs args){ // interp, alpha, [start, end]
					if(args.isnil(3) || args.isnil(4))
						return valueOf(Utils.getInterpolation(args.checkjstring(1)).apply((float)args.checkdouble(2)));
					else
						return valueOf(Utils.getInterpolation(args.checkjstring(1)).apply((float)args.checkdouble(3), (float)args.checkdouble(4), (float)args.checkdouble(2)));
				}
			});
			library.set("savestate", tableOf());
			library.set("save", new ZeroArgFunction(){
					@Override
					public LuaValue call(){
						game.savestatepath.writeString(saveSaveState().toString(), false);
						return NONE;
					}
				});
			library.set("delay", new TwoArgFunction(){
					@Override
					public LuaValue call(LuaValue time, LuaValue function){
						game.player.addDelay((float)time.optdouble(0), function.checkfunction());
						return NONE;
					}
				});
			library.set("addTexture", new OneArgFunction(){
					@Override
					public LuaValue call(LuaValue path){
						game.getSkin().add(Utils.filename(path.checkjstring()), new TextureRegion(new Texture(game.mappackpath.child(path.checkjstring()))), TextureRegion.class);
						return NONE;
					}
				});
			library.set("addRegion", new VarArgFunction(){
					@Override
					public Varargs invoke(Varargs args){ // texture, name, x, y, w, h
						Texture tex = game.getSkin().getRegion(args.checkjstring(1)).getTexture();
						game.getSkin().add(args.checkjstring(2), new TextureRegion(tex, args.checkint(3), args.checkint(4), args.checkint(5), args.checkint(6)));
						return NONE;
					}
				});
			library.set("regionRect", new OneArgFunction(){
				@Override
				public LuaValue call(LuaValue name){
					TextureRegion region = game.getSkin().getRegion(name.checkjstring());
					LuaTable rect = tableOf();
					rect.set("x", region.getRegionX());
					rect.set("y", region.getRegionY());
					rect.set("width", region.getRegionWidth());
					rect.set("height", region.getRegionHeight());
					return rect;
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
					public Varargs invoke(Varargs args){ // self(?), x, y, [width, height | region, isPatch]
						if(args.arg(4).isnumber()){
							return new Entity.DrawableEntity(game,
								(float)args.checkdouble(2), (float)args.checkdouble(3),
								(float)args.checkdouble(4), (float)args.checkdouble(5),
								new LayeredTextureRegionDrawable(new LayeredTextureRegion(game.getSkin().getRegion("white")))
								).vars;
						}else{
							if(args.arg(5).optboolean(false)){
								NinePatch patch = game.getSkin().getPatch(args.checkjstring(4));
								return new Entity.DrawableEntity(game,
									(float)args.checkdouble(2), (float)args.checkdouble(3),
									(float)patch.getTotalWidth(), (float)patch.getTotalHeight(),
									new NinePatchDrawable(patch)
									).vars;
							}else{
								TextureRegion region = game.getSkin().getRegion(args.checkjstring(4));
								return new Entity.DrawableEntity(game,
									(float)args.checkdouble(2), (float)args.checkdouble(3),
									(float)region.getRegionWidth(), (float)region.getRegionHeight(),
									new LayeredTextureRegionDrawable(new LayeredTextureRegion(region))
									).vars;
							}
						}
					}
				});
			ent.set("add", new OneArgFunction(){
					@Override
					public LuaValue call(LuaValue ent){
						Entity e = (Entity)ent.getmetatable().get(Entity.ENTITY).checkuserdata(Entity.class);
						Log.verbose("Added", e);
						game.map.addEntity(e);
						return NONE;
					}
				});
			ent.set("remove", new OneArgFunction(){
					@Override
					public LuaValue call(LuaValue ent){
						Entity e = (Entity)ent.getmetatable().get(Entity.ENTITY).checkuserdata(Entity.class);
						Log.verbose("Removed", e);
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
			library.set("Group", new VarArgFunction(){
					@Override
					public Varargs invoke(Varargs args){
						Entity.GroupEntity group = new Entity.GroupEntity(game, 0, 0);
						for(int i = 1; i <= args.narg(); i++)
							group.addEntity((Entity)args.arg(i).getmetatable().get(Entity.ENTITY).checkuserdata(Entity.class));
						return group.vars;
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
							case "startMap":
								LuaTable startMap = tableOf();
								startMap.set(1, startMapTmx);
								startMap.set(2, startMapLua);
								return startMap;
							case "player":
								return game.player.getGlobals();
							case "moveSpeed":
								return valueOf(game.player.speedMult);
							case "cameraX":
								return valueOf(game.getCamera().position.x);
							case "cameraY":
								return valueOf(game.getCamera().position.y);
							case "cameraZoom":
								return valueOf(game.getCamera().zoom);
							case "boundryX":
								return valueOf(game.map.getBound().getX());
							case "boundryY":
								return valueOf(game.map.getBound().getY());
							case "boundryWidth":
								return valueOf(game.map.getBound().getWidth());
							case "boundryHeight":
								return valueOf(game.map.getBound().getHeight());
							case "offsetX":
								return valueOf(game.map.getTileOffsetX());
							case "offsetY":
								return valueOf(game.map.getTileOffsetY());
							case "screenX":
								tmp.set(game.getViewport().getLeftGutterWidth(), game.getViewport().getBottomGutterHeight());
								return valueOf(game.getStage().screenToStageCoordinates(tmp).x);
							case "screenY":
								tmp.set(game.getViewport().getLeftGutterWidth(), game.getViewport().getBottomGutterHeight());
								return valueOf(game.getStage().screenToStageCoordinates(tmp).y);
							case "screenWidth":
								return valueOf(game.getViewport().getScreenWidth()  * game.getCamera().zoom);
							case "screenHeight":
								return valueOf(game.getViewport().getScreenHeight() * game.getCamera().zoom);
							default:
								return self.rawget(key);
						}
					}
				});
			library.getmetatable().set(NEWINDEX, new ThreeArgFunction(){
					@Override
					public LuaValue call(LuaValue self, LuaValue key, LuaValue value){
						switch(key.checkjstring()){
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
							case "moveSpeed":
								game.player.speedMult = (float)value.checkdouble();
								break;
							case "cameraX":
								game.getCamera().position.x = (float)value.checkdouble();
								break;
							case "cameraY":
								game.getCamera().position.y = (float)value.checkdouble();
								break;
							case "cameraZoom":
								game.getCamera().zoom = (float)value.checkdouble();
								break;
							case "boundryX":
								Rectangle bound = game.map.getBound();
								game.map.setBound((float)value.checkdouble(), bound.getY(), bound.getWidth(), bound.getHeight());
								break;
							case "boundryY":
								bound = game.map.getBound();
								game.map.setBound(bound.getX(), (float)value.checkdouble(), bound.getWidth(), bound.getHeight());
								break;
							case "boundryWidth":
								bound = game.map.getBound();
								game.map.setBound(bound.getX(), bound.getY(), (float)value.checkdouble(), bound.getHeight());
								break;
							case "boundryHeight":
								bound = game.map.getBound();
								game.map.setBound(bound.getX(), bound.getY(), bound.getWidth(), (float)value.checkdouble());
								break;
							case "offsetX":
								game.map.setTileOffsetX((float)value.checkdouble());
								break;
							case "offsetY":
								game.map.setTileOffsetY((float)value.checkdouble());
								break;
							case "screenX":
								self.error("screen dimensions are read-only");
								break;
							case "screenY":
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
			 LuaTable instance = loadNPC(name.checkjstring(), game.getStage()).getGlobals();
			 instance.setmetatable(clazz); // Actually this line is killing the inherited metatables
			 return instance;
			 }
			 });
			 library.set("NPC", npcClass);*/

			ScriptGlob.S.setLibToEnv(library, env);
			return env;
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
