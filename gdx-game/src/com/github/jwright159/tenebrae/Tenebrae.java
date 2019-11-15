package com.github.jwright159.tenebrae;

import com.github.jwright159.gdx.*;
import com.github.jwright159.gdx.screen.GameScreen;
import com.github.jwright159.gdx.graphics.*;
import com.badlogic.gdx.*;
import com.badlogic.gdx.files.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.input.*;
import com.badlogic.gdx.input.GestureDetector.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import com.badlogic.gdx.utils.*;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;
import java.io.*;
import com.badlogic.gdx.utils.viewport.*;
import org.json.*;

public class Tenebrae extends GameScreen{
	public FileHandle mappackpath;
	public FileHandle savestatepath;
	private boolean continueGame;
	private JSONObject mappackinfo;
	private String mappackname;

	public static final float DEADZONE_DEFAULT = 0.7f;// 0 is at edge, 1 is at center
	public static final float TILES = 7.5f;// number of tiles on the screen by height, only accepts 1 param so deal with it (KQ is 10)
	public static final float MARGIN = 30f;
	public static final boolean TABLEDEBUG = false, SHOWEMPTY = false;
	
	public final Rectangle screenRect = new Rectangle(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()); // Dims of game screen
	public boolean doneLoading = false;

	public Player player;
	public Mappack mappack;

	public static Globals globals = new ScriptGlob.ServerGlobals();

	public Tenebrae(FileHandle pakpath, JSONObject mappackinfo, FileHandle savestatepath, boolean load){
		this.mappackpath = pakpath;
		this.savestatepath = savestatepath;
		this.continueGame = load;
		this.mappackinfo = mappackinfo;
		try{
			mappackname = mappackinfo.getString("name");
		}catch(JSONException ex){
			mappackname = "Tenebrae RPG Engine";
		}
		
		doneLoading = false;
		Log.setLogFile(pakpath.child("debug.log"));

		loadSkin(pakpath);

		getMultiplexer().addProcessor(0, new InputAdapter(){
				@Override
				public boolean touchDown(int x, int y, int pointer, int button){
					Log.verbose("Pressed down!");
					return false;
				}
				@Override
				public boolean touchUp(int x, int y, int pointer, int button){
					Log.verbose("Pressed up!");
					return false;
				}
				@Override
				public boolean keyDown(int keycode){
					switch(keycode){
						case Input.Keys.BACK:
						case Input.Keys.ESCAPE:
							if(!doneLoading || player == null){
								Gdx.app.exit();
							}else if(!player.performBack()){
								//Gdx.app.exit();
								player.setExpanded(true);
							}
							return true;

						case Input.Keys.Z:
						case Input.Keys.ENTER:
						case Input.Keys.SPACE:
							if(doneLoading)
								if(player.dialogBox.isVisible()){
									if(player.performBack())
										return true;
								}else if(player.buttonBox.getActiveBox() != null){
									getFocusTable().clickFocus();
									return true;
								}else if(player.delay == 0)
									if(player.triggerBestTrigger())
										return true;
							return false;
						
						case Input.Keys.X:
						case Input.Keys.BACKSPACE:
							if(doneLoading){
								if(!player.performBack())
									player.setExpanded(true);
								return true; // Will definitely either do back or open menu
							}	
							return false;
						
						case Input.Keys.M:
							if(doneLoading && player.isExpanded() && !player.buttonBox.menu.isDisabled()){
								player.buttonBox.menu.toggle();
								return true;
							}
							return false;
						
						case Input.Keys.I:
							if(doneLoading && player.isExpanded() && !player.buttonBox.items.isDisabled()){
								player.buttonBox.items.toggle();
								return true;
							}
							return false;

						default:
							return false;
					}
				}
			});
		getMultiplexer().addProcessor(2, new GestureDetector(new GestureListener(){
					@Override
					public boolean touchDown(float p1, float p2, int p3, int p4){
						// TODO: Implement this method
						return false;
					}
					@Override
					public boolean tap(float p1, float p2, int p3, int p4){
						if(doneLoading){
							if(player.buttonBox.getActiveBox() != null || player.dialogBox.isVisible())
								player.performBack();
							else if(player.delay == 0)
								player.triggerBestTrigger();
						}
						return true;
					}
					@Override
					public boolean longPress(float p1, float p2){
						// TODO: Implement this method
						return false;
					}
					@Override
					public boolean fling(float p1, float p2, int p3){
						// TODO: Implement this method
						return false;
					}
					@Override
					public boolean pan(float x, float y, float dx, float dy){
						if(doneLoading && player.canMove())
							player.move(dx / player.map.tileWidth * player.speedMult * zoom, -dy / player.map.tileHeight * player.speedMult * zoom, 0, true, true);
						return true;
					}
					@Override
					public boolean panStop(float p1, float p2, int p3, int p4){
						updateZoom();
						return false;
					}
					@Override
					public boolean zoom(float origdist, float dist){
						getCamera().zoom = origdist / dist * zoom;
						player.moveCamera();
						return true;
					}
					@Override
					public boolean pinch(Vector2 p1, Vector2 p2, Vector2 p3, Vector2 p4){
						// TODO: Implement this method
						return false;
					}
					@Override
					public void pinchStop(){
						// TODO: Implement this method
					}
				}));
		Gdx.input.setCatchBackKey(true);
	}
	public void loadSave(JSONObject save){
		Log.debug("Loading!", player, save);

		doneLoading = false;
		reload();

		//eventually need to do some saving/loading
		//use Serializable
		//or JayVaScript
		//Serializable on another object
		// Nope that day is here and I'm doing none of those
		if(save != null)
			mappack.loadSaveState(save);
		getScript("mappack.lua", mappack.getGlobals()).call();
		player.changeMap(mappack.loadMap(this));

		//debugBox = new WRect(new Rectangle(0, 0, 50, 50), new Color(Color.BLACK));
		//hudStage.addActor(debugBox);
		//debugBox.toFront();
		//debugBox2.toFront();

		Log.debug("Done loading!");
		doneLoading = true;
	}
	public void loadSave(){
		if(savestatepath.exists())
			try{
				loadSave(new JSONObject(savestatepath.readString()));
				return;
			}catch(JSONException ex){
				Log.error(ex);
			}
		loadSave(null);
	}
	public void reload(){
		if(player != null)
			player.endSelf();
		mappack = new Mappack(this, mappackpath);
		player = new Player(this);
		Log.verbose("Unloaded, made " + player);
	}
	private static TextureAtlas tnbta, mappackta;
	public void loadSkin(FileHandle mappackpath){
		if(tnbta != null)
			tnbta.dispose();
		if(mappackta != null)
			mappackta.dispose();

		String atlas = null, json = null;
		try{
			atlas = mappackinfo.getString("skin_atlas");
			json = mappackinfo.getString("skin_json");
		}catch(JSONException ex){
			Log.error(ex);
		}

		Skin skin = getSkin();
		skin.addRegions(tnbta = new NineRegionTextureAtlas(Gdx.files.internal("tnbskin.atlas")));
		skin.load(Gdx.files.internal("tnbskin.json"));
		if(atlas != null && json != null){
			skin.addRegions(mappackta = new NineRegionTextureAtlas(mappackpath.child(atlas)));
			skin.load(mappackpath.child(json));
		}
	}

	//private Array<Character> neworder = new Array<Character>();
	@Override
	public void act(float delta){
		/*for(ObjectMap.Entry<TiledMapTileLayer,Array<Character>> e : player.map.maprenderer.npcs){
		 TiledMapTileLayer layer = e.key;

		 }


		 Character first = oldorder.first();
		 for(Character c : oldorder){
		 if(c.getZIndex() < first.getZIndex())
		 first = c;

		 boolean did = false;
		 for(int i = 0; i < neworder.size; i++){
		 Character c2 = neworder.get(i);
		 if(c.y < c2.y){// up/bottom to down/top
		 neworder.insert(i, c);
		 did = true;
		 break;
		 }
		 }
		 if(!did)
		 neworder.add(c);
		 }
		 for(int i = 0; i < neworder.size; i++)
		 neworder.get(i).
		 */

		if(!doneLoading){
			if(scriptLoader == null){
				Log.debug("Starting loading scripts!");
				loadedScripts = false;
				final FileHandle[] flist = mappackpath.list("lua");
				if(flist.length == 0)
					throw new RuntimeException("no .lua files found");
				if(loadbar == null){
					getUiStage().addActor(splash = new Table(getSkin()));
					splash.setFillParent(true);
					splash.setDebug(TABLEDEBUG);
					splash.background("window");
					Label title;
					splash.add(title = new Label(mappackname, getSkin(), getSkin().has("title", BitmapFont.class) ? "title" : "default")).grow().bottom();
					title.setWrap(true);
					title.setAlignment(Align.bottom);
					splash.row();
					splash.add(new Label("Loading scripts...", getSkin())).pad(MARGIN * 0.25f).expandX();
					splash.row();
					splash.add(loadbar = new EntityBox.TextBar("Loadin", 0, flist.length - 1, 1, false, getSkin())).size(500f, 100f).expand().top();
				}
				scripts = new ArrayMap<String,Prototype>(){
					@Override
					public Prototype get(String key){
						Prototype x = super.get(key);
						if(x == null)
							throw new NullPointerException("Attempt to access null script " + key);
						return x;
					}
				};
				scriptLoader = new Thread(new Runnable(){
						@Override
						public void run(){
							for(int i = 0; i < flist.length; i++){
								final FileHandle f = flist[i];
								Log.debug("Compiling", f.name());
								final Prototype script;
								try{
									script = globals.compilePrototype(f.read(), f.nameWithoutExtension());
								}catch(IOException ex){throw new GdxRuntimeException("Couldn't load script " + f.name(), ex);}
								final int j = i;
								Gdx.app.postRunnable(new Runnable(){
										@Override
										public void run(){
											scripts.put(f.name(), script);
											loadbar.bar.setValue(j);
											loadbar.text.setText(flist[j < flist.length ? j + 1 : 0].nameWithoutExtension());
										}
									});
							}
							Gdx.app.postRunnable(new Runnable(){
									@Override
									public void run(){
										loadedScripts = true;
										loadbar.text.setText("");
										Log.debug("Done loading scripts!");
									}
								});
						}
					});
				scriptLoader.start();
			}else if(loadedScripts){
				if(loadbar.text.getText().length() == 0)
					loadbar.text.setText("Loading...");
				else{
					splash.remove();
					splash = null;
					
					Log.debug("Starting loading the save!", continueGame);
					if(continueGame)
						loadSave();
					else
						loadSave(null);
					Log.debug("Done loading save!");
				}
			}
		}else{ // IN-GAME ACT



		}
	}
	private Thread scriptLoader;
	private ArrayMap<String,Prototype> scripts;
	private EntityBox.TextBar loadbar;
	private Table splash;
	private boolean loadedScripts;
	public LuaClosure getScript(String name, Globals env){
		Log.debug("Getting script", name);
		return new LuaClosure(scripts.get(name), env);
	}
	public Prototype getProto(String name){
		return scripts.get(name);
	}

	//@Override
	public void draww(){
		Batch batch = getStage().getBatch();
		NinePatchDrawable patch = new NinePatchDrawable(getSkin().getPatch("button"));
		batch.begin();
		patch.draw(batch, 200, 200, 50, 100);
		patch.draw(batch, 200, 300, 100, 100);
		patch.draw(batch, 200, 400, 300, 100);
		patch.draw(batch, 200, 500, 1200, 100);
		batch.end();
	}

	public float zoom = 1;
	public void updateZoom(){
		zoom = getCamera().zoom;
	}

	@Override
	public void resize(int x, int y){
		super.resize(x, y);
		Viewport vp = getViewport(), uvp = getUiViewport();
		Log.debug("Game", vp.getScreenWidth(), vp.getScreenHeight(), vp.getWorldWidth(), vp.getWorldHeight(),
			"UI", uvp.getScreenWidth(), vp.getScreenHeight(), uvp.getWorldWidth(), uvp.getWorldHeight());
		vp.setWorldSize(vp.getScreenWidth(), vp.getScreenHeight());
		screenRect.set(vp.getScreenX(), vp.getScreenY(), vp.getScreenWidth(), vp.getScreenHeight());
		Log.debug("ScreenRect", screenRect);
		player.dzRect = null;
	}

	@Override
	public void dispose(){
		super.dispose();
		tnbta.dispose();
		mappackta.dispose();
		mappack.dispose();
	}

	public class StdEntGlobals extends ScriptGlob.StdGlobals{
		public StdEntGlobals(){
			super(mappackpath);
			if(getmetatable() == null) setmetatable(tableOf());
			LuaValue mt = getmetatable();
			final LuaValue oldind = mt.get(INDEX).isnil() ? ScriptGlob.defindex : mt.get(INDEX),
				oldnewind = mt.get(NEWINDEX).isnil() ? ScriptGlob.defnewindex : mt.get(NEWINDEX);
			mt.set(INDEX, new TwoArgFunction(){
					@Override
					public LuaValue call(LuaValue self, LuaValue key){
						switch(key.checkjstring()){
							case "player":
								return player.getGlobals();
							case "map":
								return mappack.getGlobals();
							default:
								return oldind.call(self, key);
						}
					}
				});
			mt.set(NEWINDEX, new ThreeArgFunction(){
					@Override
					public LuaValue call(LuaValue self, LuaValue key, LuaValue value){
						switch(key.checkjstring()){
							case "player":
								self.error("player is read-only");
								break;
							case "map":
								self.error("map is read-only");
								break;
							default:
								oldnewind.call(self, key, value);
						}
						return NONE;
					}
				});
		}
	}
}
