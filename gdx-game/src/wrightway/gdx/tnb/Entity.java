package wrightway.gdx.tnb;

import wrightway.gdx.*;
import org.luaj.vm2.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.*;

public class Entity extends WActor.WRect{
	public static final String ENTITY = "__entity";
	
	public LuaTable vars;
	public float x,y,width,height;
	public Entity(){
		super(new Rectangle(0, 0, 0, 0), Color.WHITE);
		vars = LuaValue.tableOf();
		new EntityLib().call(LuaValue.valueOf(""), vars);
	}
	@Override
	public void act(float delta){
		super.act(delta);
		LuaValue func = vars.get("act");
		if(!func.isnil() && func.isfunction())
			func.call(vars, LuaValue.valueOf(delta));
	}

	public class EntityLib extends TwoArgFunction{
		@Override
		public LuaValue call(LuaValue modname, LuaValue env){
			LuaTable library = tableOf();

			/*library.set("setIdleRoutine", new OneArgFunction(){
					@Override
					public LuaValue call(LuaValue routine){
						idleRoutine = new Array<FunctionAction>();
						LuaValue k = LuaValue.NIL;
						while(true){
							Varargs en = routine.checktable().next(k);
							if((k = en.arg1()).isnil())
								break;
							LuaValue v = en.arg(2);
							idleRoutine.add(new FunctionAction(v.checkfunction()));
						}
						return NONE;
					}
				});*/
			library.setmetatable(tableOf());
			library.getmetatable().set(INDEX, new TwoArgFunction(){
					@Override
					public LuaValue call(LuaValue self, LuaValue key){
						switch(key.checkjstring()){
							case "x":
								return valueOf(x);
							case "y":
								return valueOf(y);
							case "width":
								return valueOf(width);
							case "height":
								return valueOf(height);
							default:
								return self.rawget(key);
						}
					}
				});
			library.getmetatable().set(NEWINDEX, new ThreeArgFunction(){
					@Override
					public LuaValue call(LuaValue self, LuaValue key, LuaValue value){
						switch(key.checkjstring()){
							case "x":
								x = (float)value.checkdouble();
								setX(x*Tenebrae.player.map.tilewidth);
								break;
							case "y":
								y = (float)value.checkdouble();
								setY(y*Tenebrae.player.map.tileheight);
								break;
							case "width":
								width = (float)value.checkdouble();
								setWidth(width*Tenebrae.player.map.tilewidth);
								break;
							case "height":
								height = (float)value.checkdouble();
								setHeight(height*Tenebrae.player.map.tileheight);
								break;
							default:
								self.rawset(key, value);
						}
						return NONE;
					}
				});
			library.getmetatable().set(ENTITY, CoerceJavaToLua.coerce(Entity.this));

			ScriptGlob.S.setLibToEnv(library, env);
			return env;
		}
	}
}
