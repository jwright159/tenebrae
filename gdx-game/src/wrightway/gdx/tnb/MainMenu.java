package wrightway.gdx.tnb;

import com.badlogic.gdx.*;
import com.badlogic.gdx.files.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import wrightway.gdx.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.*;

public class MainMenu extends WScreen{
	public static final FileHandle GAMEPATH = Gdx.files.external("WrightWay/Tenebrae");
	
	private NineRegionTextureAtlas ta1;
	private TextureRegion bg;
	private static final float bgScale = 0.5f;
	private float bgRatio, t = 0;
	
	public MainMenu(){
		Log.setVerbosity(GAMEPATH.child("v").readString());
		Log.setLogFile(GAMEPATH.child("debug.log"));
		
		Skin skin = getSkin();
		skin.addRegions(ta1 = new NineRegionTextureAtlas(Gdx.files.internal("tnbskin.atlas")));
		skin.load(Gdx.files.internal("tnbskin.json"));
		bg = new TextureRegion(new Texture(Gdx.files.internal("screenie.png")));
		bgRatio = (float)bg.getTexture().getWidth()/bg.getTexture().getHeight() * Gdx.graphics.getHeight()/Gdx.graphics.getWidth();
		getTable().setBackground(new TextureRegionDrawable(bg));
		
		Table table = new Table(skin);
		getTable().add(table).fill().width(Value.percentWidth(.33f, getTable())).height(Value.percentHeight(.67f, getTable()));
		table.setBackground("window");
		table.pad(Tenebrae.MARGIN);
		Label name = new Label("Tenebrae Engine", skin, "title");
		table.add(name).grow().uniform();
		name.setAlignment(Align.center);
		
		TextButton play, mp, quit;
		table.row();
		table.add(play = new TextButton("Play PAK", skin)).grow().uniform();
		play.addListener(new ChangeListener(){
			@Override
			public void changed(ChangeEvent event, Actor a){
				MyGdxGame.game.setScreen(new Tenebrae());
				dispose();
			}
		});
		table.row();
		table.add(mp = new TextButton("Change Mappack\n(Unsupported)", skin, "disabled")).grow().uniform();
		mp.setDisabled(true);
		table.row();
		table.add(quit = new TextButton("Quit", skin)).grow().uniform();
		quit.addListener(new ChangeListener(){
				@Override
				public void changed(ChangeEvent event, Actor a){
					Gdx.app.exit();
				}
			});
	}

	@Override
	public void act(float delta){
		t += delta*(((MathUtils.sin(System.nanoTime()*MathUtils.nanoToSec*0.2f)+1)/2)*0.09f+0.11f);
		float x = (MathUtils.sin(t*0.7f)+1)/2*bgScale;
		float y = (MathUtils.cos(t)+1)/2*bgScale;
		bg.setRegion(x, y, x+bgScale, y+bgScale*bgRatio);
	}

	@Override
	public void dispose(boolean disposeUi){
		super.dispose(disposeUi);
		ta1.dispose();
		bg.getTexture().dispose();
	}
}
