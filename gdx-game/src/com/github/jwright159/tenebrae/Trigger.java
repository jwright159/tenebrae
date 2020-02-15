package com.github.jwright159.tenebrae;

import com.badlogic.gdx.maps.*;
import com.badlogic.gdx.math.*;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.jse.*;

public class Trigger{
	private Tenebrae game;
	private Rectangle rect;
	private MapProperties props;
	
	public Trigger(Tenebrae game, Rectangle rect, MapProperties props){
		this.game = game;
		this.rect = rect;
		this.props = props;
	}
	
	public Rectangle getRectangle(){
		return rect;
	}
	
	public MapProperties getProperties(){
		return props;
	}
	
	public boolean hasProperty(String prop){
		return !props.get(prop, "", String.class).isEmpty();
	}
	
	public LuaValue getLuaValue(String prop){
		if(props.containsKey(Entity.ENTITY))
			return props.get(Entity.ENTITY, Entity.class).vars.get(props.get(prop, String.class));
		else
			return game.mappack.getGlobals().get(props.get(prop, String.class));
	}
	
	public void trigger(String prop){
		LuaValue call = getLuaValue(prop);
		if(!call.isnil())
			if(props.containsKey(Entity.ENTITY))
				call.call(props.get(Entity.ENTITY, Entity.class).vars);
			else
				call.call(game.mappack.getGlobals());
	}

	@Override
	public boolean equals(Object obj){
		return obj instanceof Trigger && props == ((Trigger)obj).props;
	}
}
