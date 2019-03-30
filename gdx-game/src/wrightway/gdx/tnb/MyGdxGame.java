package wrightway.gdx.tnb;

import com.badlogic.gdx.*;

public class MyGdxGame extends Game{
	public static MyGdxGame game;
	
	@Override
	public void create(){
		game = this;
		setScreen(new MainMenu());
	}

	@Override
	public void dispose(){
		super.dispose();
		getScreen().dispose();
	}
}
