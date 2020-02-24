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
	public static final FileHandle MENUSTATE = Gdx.files.local("menustate.json");
	
	private static final String JSON_MAPPACKPATH = "mappackPath";

	private NineRegionTextureAtlas tnbta, mappackta;
	private TextureActor bgactor;
	private TextureRegion bg;
	private FileHandle mappackpath, savestatepath;
	private JSONObject mappack;

	public MainMenu(){
		super();
		Log.setVerbosity(GAMEPATH.child("v").readString());
		Log.setLogFile(GAMEPATH.child("debug.log"));

		FileHandle menustate = MENUSTATE;
		if(menustate.exists()){
			JSONObject state;
			try{
				state = new JSONObject(menustate.readString());
			}catch(JSONException ex){
				Log.error(ex);
				menustate = null;
				return;
			}

			try{
				mappackpath = GAMEPATH.child(state.getString(JSON_MAPPACKPATH));
				savestatepath = GAMEPATH.child(mappackpath.name()+".json");
				if(mappackpath.exists() && mappackpath.child("mappack.json").exists())
					mappack = new JSONObject(mappackpath.child("mappack.json").readString());
				else
					mappackpath = null;
			}catch(JSONException ex){
				Log.error(ex);
				mappackpath = null;
			}
		}
		String mpname = "Tenebrae Engine";
		if(mappack != null)
			try{
				mpname = mappack.getString("name");
			}catch(JSONException ex){}
			
		loadSkin();

		Skin skin = getSkin();

		Stack stack = new Stack();
		getTable().add(stack).grow();
		final FocusTable table =  new FocusTable(skin);
		table.setBackground("window");
		table.pad(Tenebrae.MARGIN);
		stack.add(new Container<Table>(table).width(Value.percentWidth(.33f, getTable())).height(Value.percentHeight(.67f, getTable())));

		final Label name = new Label(mpname, skin, "title");
		name.setAlignment(Align.center);
		name.setEllipsis(true);
		Container<Label> namecont = new Container<Label>(name);
		namecont.fill();
		namecont.setClip(true);
		namecont.maxWidth(Value.percentWidth(1, table));
		table.add(namecont).grow().uniform();

		final TextButton newgame, contgame, mp, quit;

		newgame = new TextButton("New Game", skin);
		table.row();
		table.registerFocus(table.add((Button)newgame).grow().uniform());
		if(mappackpath == null)
			newgame.setDisabled(true);
		newgame.addListener(new ChangeListener(){
				@Override
				public void changed(ChangeEvent event, Actor a){
					MyGdxGame.game.setScreen(new Tenebrae(mappackpath, mappack, savestatepath, false));
					dispose();
				}
			});
			
		contgame = new TextButton("Continue Game", skin);
		table.row();
		table.registerFocus(table.add((Button)contgame).grow().uniform());
		if(mappackpath == null || !savestatepath.exists())
			contgame.setDisabled(true);
		contgame.addListener(new ChangeListener(){
				@Override
				public void changed(ChangeEvent event, Actor a){
					MyGdxGame.game.setScreen(new Tenebrae(mappackpath, mappack, savestatepath, true));
					dispose();
				}
			});

		mp = new TextButton("Change Mappack", skin);
		table.row();
		table.registerFocus(table.add((Button)mp).grow().uniform());

		quit = new TextButton("Quit", skin);
		table.row();
		table.registerFocus(table.add((Button)quit).grow().uniform());
		quit.addListener(new ChangeListener(){
				@Override
				public void changed(ChangeEvent event, Actor a){
					Gdx.app.exit();
				}
			});

		setFocusTable(table);

		final FocusTable mappackselect = new FocusTable(skin);
		mappackselect.setBackground("window");
		mappackselect.pad(Tenebrae.MARGIN);
		mappackselect.setVisible(false);
		stack.add(new Container<Table>(mappackselect).fill().pad(Tenebrae.MARGIN));


		//getTable().setDebug(true, true);

		mp.addListener(new ChangeListener(){
				@Override
				public void changed(ChangeEvent event, Actor a){
					table.setVisible(false);
					mappackselect.setVisible(true);
					mappackselect.clearChildren();
					Array<TextButton> buttons = new Array<>();
					for(final FileHandle f : GAMEPATH.list())
						if(f.isDirectory() && f.child("mappack.json").exists()){
							final String newmpname;
							try{
								JSONObject newmappack = new JSONObject(f.child("mappack.json").readString());
								newmpname = newmappack.getString("name");
							}catch(JSONException ex){
								Log.error(ex);
								newmpname = null;
							}

							Skin skin = getSkin();
							TextButton button = new TextButton(newmpname, skin);
							button.addListener(new ChangeListener(){
									@Override
									public void changed(ChangeEvent event, Actor a){
										mappackselect.setVisible(false);
										table.setVisible(true);
										setFocusTable(table);

										mappackpath = f;
										savestatepath = GAMEPATH.child(mappackpath.name()+".json");
										try{
											mappack = new JSONObject(mappackpath.child("mappack.json").readString());
										}catch(JSONException ex){
											mappackpath = null;
										}

										loadSkin();
										Skin skin = getSkin();
										name.setText(newmpname);
										name.setStyle(skin.get("title", Label.LabelStyle.class));
										table.setBackground(skin.getDrawable("window"));
										newgame.setDisabled(mappackpath == null);
										newgame.setStyle(skin.get("default", TextButton.TextButtonStyle.class));
										contgame.setDisabled(mappackpath == null || !savestatepath.exists());
										contgame.setStyle(skin.get("default", TextButton.TextButtonStyle.class));
										mp.setStyle(skin.get("default", TextButton.TextButtonStyle.class));
										quit.setStyle(skin.get("default", TextButton.TextButtonStyle.class));
										mappackselect.setBackground(skin.getDrawable("window"));
									}
								});
							buttons.add(button);
						}
					buttons.sort(new java.util.Comparator<TextButton>(){
							@Override
							public int compare(TextButton p1, TextButton p2){
								return p1.getText().toString().compareTo(p2.getText().toString());
							}
						});
					for(Button button : buttons){
						mappackselect.registerFocus(mappackselect.add(button).growX().fillY());
						mappackselect.row();
					}
					setFocusTable(mappackselect);
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
			float scale; // I have no clue why scale isn't working
			if(bgactor.getWidth()/bgactor.getHeight() > getWidth()/getHeight())
				scale = getWidth()/bgactor.getWidth();
			else
				scale = getHeight()/bgactor.getHeight();
			bgactor.setSize(bgactor.getWidth()*scale, bgactor.getHeight()*scale);
			bgactor.setOrigin(bgactor.getWidth()/2, bgactor.getHeight()/2);
			getStage().addActor(bgactor);
			getCamera().position.x = 0;
			getCamera().position.y = 0;
		}
	}

	@Override
	public void hide(){
		super.hide();

		try{
			JSONObject menustate = new JSONObject();
			menustate.put(JSON_MAPPACKPATH, mappackpath.name());
			MENUSTATE.writeString(menustate.toString(), false);
		}catch(JSONException ex){
			Log.debug(ex);
		}
	}

	@Override
	public void dispose(boolean disposeUi){
		super.dispose(disposeUi);
		if(mappackta != null)
			mappackta.dispose();
		if(bg != null)
			bg.getTexture().dispose();
	}
}
