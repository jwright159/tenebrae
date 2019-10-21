package wrightway.gdx.tnb;

import com.badlogic.gdx.*;
import com.badlogic.gdx.files.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.input.*;
import com.badlogic.gdx.input.GestureDetector.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import com.badlogic.gdx.utils.*;
import wrightway.gdx.*;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;
import java.io.*;
import com.badlogic.gdx.utils.viewport.*;
import org.json.*;

public class Tenebrae extends WScreen{
	public static FileHandle pakpath;

	public static final float DEADZONE_DEFAULT = 0.7f;//0.45f;//0 is at edge, 1 is at center
	public static final float TILES = 7.5f;//number of tiles on the screen by width, only accepts 1 param so deal with it (KQ is 10)

	public static final Rectangle screenRect = new Rectangle(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	public static final float MARGIN = 30f;
	public static boolean doneLoading = false;
	public static final boolean tableDebug = false, showEmpty = false;

	public static Tenebrae t;
	public static Player player;
	public static Mappack mp;

	public static Globals globals = new ScriptGlob.ServerGlobals();

	public Tenebrae(FileHandle pakpath){
		super(new FitViewport(1920, 1080));
		this.pakpath = pakpath;
		doneLoading = false;
		t = this;
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
							if(!doneLoading || player == null){
								Gdx.app.exit();
							}else if(!player.performBack()){
								//Gdx.app.exit();
								player.setExpanded(true);
							}
							return true;
							
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
						if(doneLoading && player.buttonBox.getActiveBox() == null && !player.hasAnyAction())
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
	public void loadSave(){
		Log.debug("Loading! " + player);

		doneLoading = false;
		reload();

		//eventually need to do some saving/loading
		//use Serializable
		//or JayVaScript
		//Serializable on another object
		getScript("mappack.lua", mp.getGlobals()).call();
		player.changeMap(mp.loadMap());

		//debugBox = new WRect(new Rectangle(0, 0, 50, 50), new Color(Color.BLACK));
		//hudStage.addActor(debugBox);
		//debugBox.toFront();
		//debugBox2.toFront();

		Log.debug("Done loading!");
		doneLoading = true;
	}
	public void reload(){
		if(player != null)
			player.endSelf();
		mp = new Mappack(pakpath);
		player = new Player();
		Log.verbose("Unloaded, made " + player);
	}
	private static TextureAtlas tnbta, mappackta;
	public void loadSkin(FileHandle mappackpath){
		if(tnbta != null)
			tnbta.dispose();
		if(mappackta != null)
			mappackta.dispose();

		String atlas = null, json = null;
		JSONObject mappack = null;
		try{
			mappack = new JSONObject(mappackpath.child("mappack.json").readString());
		}catch(JSONException ex){
			mappackpath = null;
		}
		if(mappack != null){
			try{
				atlas = mappack.getString("skin_atlas");
				json = mappack.getString("skin_json");
			}catch(JSONException ex){}
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
				final FileHandle[] flist = pakpath.list("lua");
				if(flist.length == 0)
					throw new RuntimeException("no .lua files found");
				if(loadbar == null){
					getUiStage().addActor(splash = new Table(getSkin()));
					splash.setFillParent(true);
					splash.setDebug(tableDebug);
					splash.background("window");
					Label title;
					splash.add(title = new Label("Tenebrae RPG Engine", getSkin(), getSkin().has("title", BitmapFont.class) ? "title" : "default")).grow().bottom();
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
					Log.debug("Starting loading the save!");
					loadSave();
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
		screenRect.setSize(getStage().getViewport().getScreenWidth(), getStage().getViewport().getScreenHeight());
		Log.debug("Actual size:", screenRect);
	}

	@Override
	public void dispose(){
		super.dispose();
		tnbta.dispose();
		mappackta.dispose();
		mp.dispose();
	}

	public static class StdEntGlobals extends ScriptGlob.StdGlobals{
		public StdEntGlobals(){
			super(pakpath);
			if(getmetatable() == null) setmetatable(tableOf());
			LuaValue mt = getmetatable();
			final LuaValue oldind = mt.get(INDEX).isnil() ? ScriptGlob.defindex : mt.get(INDEX),
				oldnewind = mt.get(NEWINDEX).isnil() ? ScriptGlob.defnewindex : mt.get(NEWINDEX);
			mt.set(INDEX, new TwoArgFunction(){
					@Override
					public LuaValue call(LuaValue self, LuaValue key){
						switch(key.checkjstring()){
							case "player":
								return Tenebrae.player.getGlobals();
							case "map":
								return Tenebrae.mp.getGlobals();
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
