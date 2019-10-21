package com.github.jwright159.tenebrae;

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
