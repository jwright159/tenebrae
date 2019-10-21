package com.github.jwright159.tenebrae;

import com.github.jwright159.gdx.*;
import com.github.jwright159.gdx.screen.GameScreen;
import com.github.jwright159.gdx.actor.*;
import com.github.jwright159.gdx.graphics.*;
import com.badlogic.gdx.*;
import com.badlogic.gdx.files.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.*;
import org.json.*;

public class MainMenu extends GameScreen{
	public static final FileHandle GAMEPATH = Gdx.files.external("WrightWay").child("Tenebrae");

	private NineRegionTextureAtlas tnbta, mappackta;
	private TextureActor bgactor;
	private TextureRegion bg;
	private FileHandle mappackpath;
	private float t = MathUtils.random() * 10;
	private JSONObject mappack;

	public MainMenu(){
		super();
		Log.setVerbosity(GAMEPATH.child("v").readString());
		Log.setLogFile(GAMEPATH.child("debug.log"));

		FileHandle menustate = Gdx.files.local("menustate");
		if(menustate.exists()){
			String[] state = menustate.readString().split("\n");
			int i = 0;
			long stateversion = Long.parseLong(state[i++]); // for reverse compatibility
			mappackpath = GAMEPATH.child(state[i++]);
			if(mappackpath.exists() && mappackpath.child("mappack.json").exists())
				try{
					mappack = new JSONObject(mappackpath.child("mappack.json").readString());
				}catch(JSONException ex){
					Log.error(ex);
					mappackpath = null;
				}
			else
				mappackpath = null;
		}

		loadSkin();

		Skin skin = getSkin();

		Stack stack = new Stack();
		getTable().add(stack).grow();
		final Table table = new Table(skin);
		table.setBackground("window");
		table.pad(Tenebrae.MARGIN);
		stack.add(new Container<Table>(table).width(Value.percentWidth(.33f, getTable())).height(Value.percentHeight(.67f, getTable())));
		
		final Label name = new Label("Tenebrae Engine", skin, "title");
		name.setAlignment(Align.center);
		name.setEllipsis(true);
		Container<Label> namecont = new Container<Label>(name);
		namecont.fill();
		namecont.setClip(true);
		namecont.maxWidth(Value.percentWidth(1,table));
		table.add(namecont).grow().uniform();
		
		final TextButton play, mp, quit;
		
		play = new TextButton("Play", skin){
			@Override
			public void setDisabled(boolean disabled){
				super.setDisabled(disabled);
				setStyle(getSkin().get(disabled ? "disabled" : "default", TextButtonStyle.class));
			}
		};
		table.row();
		table.add(play).grow().uniform();
		if(mappackpath == null)
			play.setDisabled(true);
		play.addListener(new ChangeListener(){
				@Override
				public void changed(ChangeEvent event, Actor a){
					MyGdxGame.game.setScreen(new Tenebrae(mappackpath));
					dispose();
				}
			});
			
		mp = new TextButton("Change Mappack", skin);
		table.row();
		table.add(mp).grow().uniform();
		
		quit = new TextButton("Quit", skin);
		table.row();
		table.add(quit).grow().uniform();
		quit.addListener(new ChangeListener(){
				@Override
				public void changed(ChangeEvent event, Actor a){
					Gdx.app.exit();
				}
			});
		
		final Table mappackselect = new Table(skin);
		mappackselect.setBackground("window");
		mappackselect.pad(Tenebrae.MARGIN);
		mappackselect.setVisible(false);
		stack.add(new Container<Table>(mappackselect).fill().pad(Tenebrae.MARGIN));
		

		getTable().setDebug(true, true);
		
		mp.addListener(new ChangeListener(){
				@Override
				public void changed(ChangeEvent event, Actor a){
					table.setVisible(false);
					mappackselect.setVisible(true);
					mappackselect.clearChildren();
					for(final FileHandle f : GAMEPATH.list())
						if(f.isDirectory() && f.child("mappack.json").exists()){
							String mpname = null;
							try{
								JSONObject newmappack = new JSONObject(f.child("mappack.json").readString());
								mpname = newmappack.getString("name");
							}catch(JSONException ex){
								Log.error(ex);
							}
							
							Skin skin = getSkin();
							Button button = new Button(skin);
							mappackselect.add(button).growX().fillY();
							mappackselect.row();
							button.addListener(new ChangeListener(){
									@Override
									public void changed(ChangeEvent event, Actor a){
										mappackselect.setVisible(false);
										table.setVisible(true);
										
										mappackpath = f;
										try{
											mappack = new JSONObject(mappackpath.child("mappack.json").readString());
										}catch(JSONException ex){
											mappackpath = null;
										}
										
										loadSkin();
										Skin skin = getSkin();
										name.setStyle(skin.get("title", Label.LabelStyle.class));
										table.setBackground(skin.getDrawable("window"));
										play.setDisabled(mappackpath == null);
										play.setStyle(skin.get("default", TextButton.TextButtonStyle.class));
										mp.setStyle(skin.get("default", TextButton.TextButtonStyle.class));
										quit.setStyle(skin.get("default", TextButton.TextButtonStyle.class));
										mappackselect.setBackground(skin.getDrawable("window"));
									}
								});
							button.add(new Label(mpname, skin));
						}
				}
			});
	}

	public void loadSkin(){
		if(tnbta != null)
			tnbta.dispose();
		if(mappackta != null)
			mappackta.dispose();
		if(bg != null){
			bg.getTexture().dispose();
			bgactor.dispose();
		}
		
		String mpatlas = null, mpjson = null, menubg = null;
		if(mappack != null){
			try{
				mpatlas = mappack.getString("skin_atlas");
				mpjson = mappack.getString("skin_json");
			}catch(JSONException ex){}
			try{
				menubg = mappack.getString("menu_bg");
			}catch(JSONException ex){}
		}

		tnbta = new NineRegionTextureAtlas(Gdx.files.internal("tnbskin.atlas"));
		FileHandle tnbjson = Gdx.files.internal("tnbskin.json");
		Skin skin = new FreeSkin(tnbjson, tnbta);
		getSkin().dispose();
		setSkin(skin);
		if(mpatlas != null && mpjson != null){
			mappackta = new NineRegionTextureAtlas(mappackpath.child(mpatlas));
			skin.addRegions(mappackta);
			skin.load(mappackpath.child(mpjson));
		}

		if(menubg != null){
			bg = new TextureRegion(new Texture(mappackpath.child(menubg)));
			bgactor = new TextureActor(bg);
			getStage().addActor(bgactor);
			getCamera().zoom = 0.5f;
		}
	}

	@Override
	public void act(float delta){
		if(bgactor != null){
			t += delta * (MathUtils.map(-1, 1, 0, 1, MathUtils.sin(System.nanoTime() * MathUtils.nanoToSec * 0.2f)) * 0.09f + 0.11f);
			float x = MathUtils.map(-1, 1, 0, 1, MathUtils.sin(t * 0.7f));
			float y = MathUtils.map(-1, 1, 0, 1, MathUtils.cos(t));
			float mx = getCamera().zoom * getStage().getViewport().getScreenWidth() / 2, my = getCamera().zoom * getStage().getViewport().getScreenHeight() / 2;
			getCamera().position.set(MathUtils.map(0, 1, mx, bgactor.getWidth() - mx, x), MathUtils.map(0, 1, my, bgactor.getHeight() - my, y), 0);
		}
	}

	@Override
	public void dispose(boolean disposeUi){
		super.dispose(disposeUi);
		if(mappackta != null)
			mappackta.dispose();
		bg.getTexture().dispose();
	}
}
