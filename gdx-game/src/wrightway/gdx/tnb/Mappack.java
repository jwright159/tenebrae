package wrightway.gdx.tnb;

import wrightway.gdx.*;
import wrightway.gdx.JVSValue.Scope;
import wrightway.gdx.JVSValue.WObject;
import wrightway.gdx.JVSValue.Function;
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

public class Mappack implements JVSValue{
	FileHandle folder,file;
	String name = "Mapppack lololol", description, startMap;
	Scope vars;//This is the only scope, the global for all scripts, bc you can only set the scope when the file is parsed
	Array<NPC> npcs;

	public Mappack(String folderpath){
		folder = Gdx.files.external(folderpath);
		file = folder.child("mappack.tnb");
		npcs = new Array<NPC>();

		vars = new Scope(null, "mp");
		vars.put("setTile", new Function(new String[]{"x","y","layer","tileset","tileid"}, new JVSValue[]{new JVSValue(){
						@Override
						public Object get(Scope scope){
							Tenebrae.player.map.changeTile(scope.getVal("x", Integer.class, null), (Tenebrae.player.map.height - 1) - scope.getVal("y", Integer.class, null), scope.getVal("layer", String.class, null), scope.getVal("tileset", String.class, null), scope.getVal("tileid", Integer.class, null));
							return null;
						}
					}}));
		vars.put("say", new Function(new String[]{"text", "delayTime", "charsPerSec"}, new JVSValue[]{new JVSValue(){
						@Override
						public Object get(Scope scope){
							String text = scope.getVal("text", String.class, null);
							Float delay, cps;
							if((delay = scope.getVal("delayTime", Float.class, -1f)) == -1 & (cps = scope.getVal("charsPerSec", Float.class, -1f)) == -1){
								Tenebrae.player.addDialog(text);
							}else if(cps == -1){
								Tenebrae.player.addDialog(text, delay);
							}else{
								for(int j = 1; j < text.length(); j++){
									String substring = text.substring(0, j);
									//Tenebrae.debug("Say spell out! "+spellOut+" "+substring);
									if(Tenebrae.endsInWhitespace(substring))
										continue;
									boolean punc = (Tenebrae.endsInPunctuation(substring) && !Tenebrae.endsInPunctuation(text.substring(0, j + 1 <= text.length() ? j + 1 : j)) && !text.substring(0, j + 1 <= text.length() ? j + 1 : j).endsWith("\"")) || (Tenebrae.endsInPunctuation(substring.substring(0, substring.length() == 1 ? 1 : substring.length() - 1)) && substring.endsWith("\""));
									//if(punc)
									//Tenebrae.debug("Punctuation! " + substring);
									Tenebrae.player.addDialog(substring, (punc ? 5f : 1f) / cps);
								}
								if(delay != -1)
									Tenebrae.player.addDialog(text, delay);
								else
									Tenebrae.player.addDialog(text);
							}
							return null;
						}
					}}));
		vars.put("setMap", new Function(new String[]{"mapName", "startX", "startY"}, new JVSValue[]{new JVSValue(){
						@Override
						public Object get(Scope scope){
							Tenebrae.player.changeMap(Tenebrae.mp.loadMap(scope.getVal("mapName", String.class, null)), scope.getVal("x", Float.class, -1f), scope.getVal("y", Float.class, -1f));
							return null;
						}
					}}));
		vars.put("enableTrigger", new Function(new String[]{"trigger"}, new JVSValue[]{new JVSValue(){
						@Override
						public Object get(Scope scope){
							Tenebrae.player.map.getTriggerObjects().get(scope.getVal("trigger", String.class, null)).getProperties().put("disabled", false);
							return null;
						}
					}}));
		vars.put("disableTrigger", new Function(new String[]{"trigger"}, new JVSValue[]{new JVSValue(){
						@Override
						public Object get(Scope scope){
							Tenebrae.player.map.getTriggerObjects().get(scope.getVal("trigger", String.class, null)).getProperties().put("disabled", true);
							return null;
						}
					}}));
		vars.put("setTrigger", new Function(new String[]{"trigger", "function", "value"}, new JVSValue[]{new JVSValue(){
						@Override
						public Object get(Scope scope){
							String value = scope.getVal("value", String.class, ""), func = scope.getVal("function", String.class, "");
							Tenebrae.player.map.getTriggerObjects().get(scope.getVal("trigger", String.class, null)).getProperties().put(!value.isEmpty() ? func : "onTrigger", !value.isEmpty() ? value : func);
							return null;
						}
					}}));
		vars.put("println", new Function(new String[]{"text"}, new JVSValue[]{new JVSValue(){
						@Override
						public Object get(Scope scope){
							Tenebrae.userLog(scope.getVal("text", String.class, null));
							return null;
						}
					}}));
		vars.put("errorln", new Function(new String[]{"text"}, new JVSValue[]{new JVSValue(){
						@Override
						public Object get(Scope scope){
							Tenebrae.log((byte)0b000_0011, scope.getVal("text", String.class, null));
							return null;
						}
					}}));
		vars.put("zoom", new Function(new String[]{"amt", "time", "interp", "tap"}, new JVSValue[]{new JVSValue(){
						@Override
						public Object get(Scope scope){
							OrthographicCamera cam = (OrthographicCamera)Tenebrae.t.worldStage.getCamera();
							Tenebrae.player.addAction(new Tenebrae.CameraAction(cam, cam.zoom * scope.getVal("amt", Float.class, 1f), Tenebrae.getInterpolation(scope.getVal("interp", String.class, "constant")), scope.getVal("time", Float.class, 0f), scope.getVal("tap", java.lang.Boolean.class, false)));
							return null;
						}
					}}));
		vars.put("pan", new Function(new String[]{"x", "y", "time", "interp", "tap"}, new JVSValue[]{new JVSValue(){
						@Override
						public Object get(Scope scope){
							OrthographicCamera cam = (OrthographicCamera)Tenebrae.t.worldStage.getCamera();
							Tenebrae.player.addAction(new Tenebrae.CameraAction(cam, cam.position.x + scope.getVal("x", Float.class, 0f) * Tenebrae.player.map.tilewidth, cam.position.y + scope.getVal("y", Float.class, 0f) * Tenebrae.player.map.tileheight, Tenebrae.getInterpolation(scope.getVal("interp", String.class, "constant")), scope.getVal("time", Float.class, 0f), scope.getVal("tap", java.lang.Boolean.class, false)));
							return null;
						}
					}}));
		vars.put("panTo", new Function(new String[]{"x", "y", "time", "interp", "tap"}, new JVSValue[]{new JVSValue(){
						@Override
						public Object get(Scope scope){
							OrthographicCamera cam = (OrthographicCamera)Tenebrae.t.worldStage.getCamera();
							Tenebrae.player.addAction(new Tenebrae.CameraAction(cam, scope.getVal("x", Float.class, 0f) * Tenebrae.player.map.tilewidth - Tenebrae.player.activeDeadzone.width / 2 - Tenebrae.player.activeDeadzone.x + Tenebrae.screenRect.width / 2, scope.getVal("y", Float.class, 0f) * Tenebrae.player.map.tileheight - Tenebrae.player.activeDeadzone.height / 2 - Tenebrae.player.activeDeadzone.y + Tenebrae.screenRect.height / 2, Tenebrae.getInterpolation(scope.getVal("interp", String.class, "constant")), scope.getVal("time", Float.class, 0f), scope.getVal("tap", java.lang.Boolean.class, false)));
							return null;
						}
					}}));
		vars.put("resetCamera", new Function(new String[]{"time", "interp", "tap"}, new JVSValue[]{new JVSValue(){
						@Override
						public Object get(Scope scope){
							OrthographicCamera cam = (OrthographicCamera)Tenebrae.t.worldStage.getCamera();
							Vector3 l = Tenebrae.player.lastCameraPos;
							Tenebrae.player.addAction(new Tenebrae.CameraAction(cam, l.x, l.y, l.z, Tenebrae.getInterpolation(scope.getVal("interp", String.class, "constant")), scope.getVal("time", Float.class, 0f), scope.getVal("tap", java.lang.Boolean.class, false)));
							return null;
						}
					}}));
		vars.put("delay", new Function(new String[]{"delayTime", "func"}, new JVSValue[]{new JVSValue(){
						@Override
						public Object get(Scope scope){
							Tenebrae.player.addDelay(scope.getVal("delayTime", Float.class, null), scope.getVal("func", Function.class, null), scope);
							return null;
						}
					}}));
		vars.put("this", new JVSValue.WValue(){//actually useful 0_o, needed for vars across files
				@Override
				public Object get(){
					return vars;
				}
			});
		vars.put("name", new JVSValue.WValue(){
				@Override
				public Object get(){
					return name;
				}
				@Override
				public void put(Object value){
					name = value.toString();
				}
			});
		vars.put("description", new JVSValue.WValue(){
				@Override
				public Object get(){
					return description;
				}
				@Override
				public void put(Object value){
					description = value.toString();
				}
			});
		vars.put("startMap", new JVSValue.WValue(){
				@Override
				public Object get(){
					return startMap;
				}
				@Override
				public void put(Object value){
					startMap = value.toString();
				}
			});
		vars.put("player", new JVSValue.WValue(){
				@Override
				public Object get(){
					return Tenebrae.player.get(null);
				}
			});
		vars.put("cameraX", new JVSValue.WValue(){
				@Override
				public Object get(){
					return Tenebrae.t.worldStage.getCamera().position.x - Tenebrae.screenRect.width / 2 + Tenebrae.player.activeDeadzone.x + Tenebrae.player.activeDeadzone.width / 2;
				}
				@Override
				public void put(Object value){
					Tenebrae.t.worldStage.getCamera().position.x = (float)value - Tenebrae.player.activeDeadzone.width / 2 - Tenebrae.player.activeDeadzone.x + Tenebrae.screenRect.width / 2;
				}
			});
		vars.put("cameraY", new JVSValue.WValue(){
				@Override
				public Object get(){
					return Tenebrae.t.worldStage.getCamera().position.y - Tenebrae.screenRect.height / 2 + Tenebrae.player.activeDeadzone.y + Tenebrae.player.activeDeadzone.height / 2;
				}
				@Override
				public void put(Object value){
					Tenebrae.t.worldStage.getCamera().position.y = (float)value - Tenebrae.player.activeDeadzone.height / 2 - Tenebrae.player.activeDeadzone.y + Tenebrae.screenRect.height / 2;
				}
			});
		vars.put("cameraZoom", new JVSValue.WValue(){
				@Override
				public Object get(){
					return ((OrthographicCamera)Tenebrae.t.worldStage.getCamera()).zoom;
				}
				@Override
				public void put(Object value){
					((OrthographicCamera)Tenebrae.t.worldStage.getCamera()).zoom = value;
				}
			});
		vars.put("screen", new JVSValue.WValue(){
				@Override
				public Object get(){
					return screen;
				}
			});
		vars.put("cameraHeight", new JVSValue.WValue(){
				@Override
				public Object get(){
					return Tenebrae.player.activeDeadzone.height * ((OrthographicCamera)Tenebrae.t.worldStage.getCamera()).zoom;
				}
			});
		vars.put("cameraWidth", new JVSValue.WValue(){
				@Override
				public Object get(){
					return Tenebrae.player.activeDeadzone.width * ((OrthographicCamera)Tenebrae.t.worldStage.getCamera()).zoom;
				}
			});
	}
	public TileMap loadMap(String name){
		return new TileMap(folder.child(name + ".tmx"), Tenebrae.t.getScript(name));
	}
	public TileMap loadMap(){
		return loadMap(startMap);
	}
	public Enemy loadEnemy(String name){
		//return new Enemy(folder.child(name + ".tnb"), this);
		throw new UnsupportedOperationException("Not usin this rn");
	}
	public MenuItem.GameItem loadItem(String name, Character owner){
		return new MenuItem.GameItem(name, Tenebrae.t.getScript(name), owner, Tenebrae.skin);
	}
	public TiledMapTileSet loadTileset(String name){
		return TiledMapTileSetLoader.loadTileSet(folder.child(name + ".tsx"));
	}
	public MidiWavSync loadMidiWav(String name, MidiEventListener listener){
		return new MidiWavSync(folder.child(name + ".mid"), folder.child(name + ".wav"), listener);
	}
	public Projectile loadProjectile(String name){
		throw new UnsupportedOperationException("Too slow to be used effectively until the code system can be revamped.");
		//TiledMapTileSet ts = loadTileset(name);
		//Tenebrae.debug("Projts", ts, ts.getTile(1), ts.getTile(2));
		//return new Projectile(folder.child(name+".tnb"), ts.getTile(1));
	}
	public EnemyWeapon loadEnemyWeapon(String name, Character owner){
		EnemyWeapon rtn = new EnemyWeapon(owner, name);
		Tenebrae.t.getScript(name).get(rtn.vars, null);
		return rtn;
	}
	public NPC loadNPC(String name){
		NPC npc = new NPC(name, Tenebrae.t.getScript(name));
		npcs.add(npc);
		Tenebrae.t.worldStage.addActor(npc);
		if(Tenebrae.player.map != null)
			npc.changeMap(Tenebrae.player.map, -1, -1);
		return npc;
	}

	@Override
	public Object get(Scope scope){
		return vars;
	}

	public ScopedScreen screen = new ScopedScreen();
	private static class ScopedScreen extends Rectangle implements JVSValue{
		Scope vars;
		ScopedScreen(){
			super(Tenebrae.screenRect);
			vars = new Scope(null, "sscreen");
			vars.put("x", new JVSValue.WValue(){
					@Override
					public Object get(){
						return getX();
					}
				});
			vars.put("y", new JVSValue.WValue(){
					@Override
					public Object get(){
						return getY();
					}
				});
			vars.put("width", new JVSValue.WValue(){
					@Override
					public Object get(){
						return getWidth();
					}
				});
			vars.put("height", new JVSValue.WValue(){
					@Override
					public Object get(){
						return getHeight();
					}
				});
		}
		@Override
		public Object get(Scope scope){
			return vars;
		}
	}
}
