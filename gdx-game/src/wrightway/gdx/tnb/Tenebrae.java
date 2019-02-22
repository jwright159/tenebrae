package wrightway.gdx.tnb;

import com.badlogic.gdx.*;
import com.badlogic.gdx.files.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.input.*;
import com.badlogic.gdx.input.GestureDetector.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.reflect.*;
import java.util.*;
import wrightway.gdx.*;
import wrightway.gdx.JVSValue.Function;
import wrightway.gdx.JVSValue.Scope;
import wrightway.gdx.WActor.*;
import com.leff.midi.util.*;
import com.leff.midi.event.*;
import java.io.*;
import com.badlogic.gdx.maps.tiled.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;

public class Tenebrae extends WScreen{
	static final String PAKPATH = "WrightWay/Tenebrae/pak/";
	static final String TAG = "Tenebrae";

	static float deadzone = 0.7f;//0.45f;//0 is at edge, 1 is at center
	static float tiles = 7.5f;//number of tiles on the screen by width, only accepts 1 param so deal with it (KQ is 10)

	static Rectangle screenRect = new Rectangle(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	static GlyphLayout gLayout;
	static float margin = 30f;
	static float stroke = 5f;
	static boolean doneLoading = false;
	static boolean tableDebug = false, showEmpty = false;

	static Tenebrae t;
	static Player player;
	static Mappack mp;

	Tenebrae(){
		doneLoading = false;
		t = this;
		Log.setVerbosity(Gdx.files.external(PAKPATH).child("v").readString());
		Log.setLogFile(Gdx.files.external(PAKPATH).child("debug.log"));

		loadSkin();

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
					if(keycode == Input.Keys.BACK){
						if(player == null){
							Gdx.app.exit();
							return true;
						}else if(!doneLoading || !player.performBack()){
							Gdx.app.exit();
							//player.setExpanded(true);
							return true;
						}
					}
					return false;
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
												 else
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
											 if(doneLoading && player.buttonBox.getActiveBox() == null)
												 player.move(dx / player.map.tilewidth * player.speedMult, -dy / player.map.tileheight * player.speedMult, 0, true);
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
		unload();

		//eventually need to do some saving/loading
		//use Serializable
		//or JayVaScript
		//Serializable on another object
		getScript("mappack").get(mp.vars, null);
		player.changeMap(mp.loadMap(), -1, -1);

		//debugBox = new WRect(new Rectangle(0, 0, 50, 50), new Color(Color.BLACK));
		//hudStage.addActor(debugBox);
		//debugBox.toFront();
		//debugBox2.toFront();

		Log.debug("Done loading!");
		doneLoading = true;
	}
	public void unload(){
		if(player != null)
			player.endSelf();
		player = new Player();
		getStage().addActor(player);
		Log.verbose("Unloaded, made " + player);
	}
	private static TextureAtlas ta1, ta2;
	private void loadSkin(){
		getSkin().addRegions(ta1 = new NineRegionTextureAtlas(Gdx.files.internal("tnbskin.atlas")));
		getSkin().load(Gdx.files.internal("tnbskin.json"));
		
		FileHandle folder = Gdx.files.external(PAKPATH).child("skin");
		FileHandle[] atlas = folder.list("atlas");
		if(atlas.length > 0)
			getSkin().addRegions(ta2 = new NineRegionTextureAtlas(atlas[0]));
		FileHandle[] json = folder.list("json");
		if(json.length > 0)
			getSkin().load(json[0]);
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
				mp = new Mappack(PAKPATH);
				FileHandle folder = Gdx.files.external(PAKPATH);
				final FileHandle[] flist = folder.list("tnb");
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
					splash.add(new Label("Loading scripts...", getSkin())).pad(margin * 0.25f).expandX();
					splash.row();
					splash.add(loadbar = new EntityBox.TextBar("Loadin", 0, flist.length - 1, 1, getSkin())).size(500f, 100f).expand().top();
				}
				scripts = new ArrayMap<String,Function>(){
					@Override
					public Function get(String key){
						Function x = super.get(key);
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
								Log.debug("Parsing", f.name());
								final Function script = JVSParser.parseCode(f.readString(), null);
								final int j = i;
								Gdx.app.postRunnable(new Runnable(){
										@Override
										public void run(){
											scripts.put(f.nameWithoutExtension(), script);
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
		}
	}
	private Thread scriptLoader;
	private ArrayMap<String,Function> scripts;
	EntityBox.TextBar loadbar;
	Table splash;
	private boolean loadedScripts;
	public Function getScript(String name){
		Log.debug("Getting script", name);
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

	float zoom = 1;
	public void updateZoom(){
		zoom = getCamera().zoom;
	}

	@Override
	public void resize(int x, int y){
		screenRect.setSize(x, y);
		if(mp != null)
			mp.screen.set(screenRect);
		super.resize(x, y);
	}

	@Override
	public void dispose(){
		super.dispose();
		ta1.dispose();
		ta2.dispose();
		//music.dispose();
	}
}
