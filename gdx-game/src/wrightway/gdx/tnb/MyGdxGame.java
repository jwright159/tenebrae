package wrightway.gdx.tnb;

import com.badlogic.gdx.*;

public class MyGdxGame extends Game{
	public static MyGdxGame game;
	
	@Override
	public void create(){
		game = this;
		Gdx.app.setLogLevel(Application.LOG_DEBUG);
		//Tenebrae.debug("ROOT FILEPATH: "+Gdx.files.external("WrightWay/Tenebre 9/pak/").path());
		setScreen(new Tenebrae());
	}

	@Override
	public void dispose(){
		//Tenebrae.debug("Disposing! :D");
		super.dispose();
		getScreen().dispose();
	}
}
