package wrightway.gdx.tnb;

import com.badlogic.gdx.maps.*;
import com.badlogic.gdx.math.*;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.jse.*;

public class Trigger{
	private Rectangle rect;
	private MapProperties props;
	
	public Trigger(Rectangle rect, MapProperties props){
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
			return Tenebrae.mp.getGlobals().get(props.get(prop, String.class));
	}
	
	public void trigger(String prop){
		LuaValue call = getLuaValue(prop);
		if(!call.isnil())
			if(props.containsKey(Entity.ENTITY))
				call.call(CoerceJavaToLua.coerce(props.get(Entity.ENTITY, Entity.class)));
			else
				call.call();
	}

	@Override
	public boolean equals(Object obj){
		return obj instanceof Trigger && props == ((Trigger)obj).props;
	}
}
