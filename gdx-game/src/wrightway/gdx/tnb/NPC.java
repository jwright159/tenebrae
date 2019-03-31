package wrightway.gdx.tnb;

import wrightway.gdx.*;
import com.badlogic.gdx.files.*;
import wrightway.gdx.tnb.Action.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.g2d.*;
import java.util.*;
import org.luaj.vm2.*;
import java.io.*;
import org.luaj.vm2.lib.*;

public class NPC extends Character{
	private Array<FunctionAction> idleRoutine;
	private boolean enabled = true;
	public static final Prototype setup;
	static{
		try{
			setup = Tenebrae.globals.compilePrototype(new StringReader("function setupMap(trigger_, enter_, exit_, idleRoutine) \n trigger = trigger_ \n enter = enter_ \n exit = exit_ \n setIdleRoutine(idleRoutine) \n end"), "setup");
		}catch(IOException ex){throw new GdxRuntimeException("Couldn't load static script", ex);}
	}

	public NPC(String name, Prototype script){
		super(name);
		getGlobals().load(new NPCLib());
		new LuaClosure(script, getGlobals()).call();
		hp = maxhp();
		mp = maxmp();
		idleRoutine = new Array<FunctionAction>();
		//box.updateHP(this);
	}

	@Override
	public boolean doDefaultAction(){
		if(idleRoutine.isEmpty() || hasTarget())
			return false;
		FunctionAction a;
		addAction(a = idleRoutine.removeIndex(0));
		Log.debug("Default action!", a);
		idleRoutine.add(a);
		return true;
	}

	@Override
	public void die(){
		throw new UnsupportedOperationException("...How?");
	}

	@Override
	public void draw(Batch batch, float parentAlpha){
		if(enabled)
			super.draw(batch, parentAlpha);
	}
	
	public class NPCLib extends TwoArgFunction{
		@Override
		public LuaValue call(LuaValue modname, LuaValue env){
			LuaTable library = tableOf();

			library.set("setIdleRoutine", new OneArgFunction(){
							@Override
							public LuaValue call(LuaValue routine){
								idleRoutine.clear();
								LuaValue k = LuaValue.NIL;
								while(!routine.isnil()){
									Varargs en = routine.checktable().next(k);
									if((k = en.arg1()).isnil())
										break;
									LuaValue v = en.arg(2);
									idleRoutine.add(new FunctionAction(v.checkfunction()));
								}
								Log.debug("Set idle routine", idleRoutine);
								return NONE;
							}
						});
			library.set("enable", new ZeroArgFunction(){
							@Override
							public LuaValue call(){
								enabled = true;
								boolean had = Tenebrae.mp.charas.contains(NPC.this, true);
								if(!had)
									Tenebrae.mp.charas.add(NPC.this);
								return valueOf(!had);
							}
						});
			library.set("disable", new ZeroArgFunction(){
							@Override
							public LuaValue call(){
								enabled = false;
								boolean had = Tenebrae.mp.charas.contains(NPC.this, true);
								if(had)
									Tenebrae.mp.charas.removeValue(NPC.this, true);
								return valueOf(had);
							}
						});
			// also trigger, enter, and exit
			/*library.setmetatable(tableOf());
			library.getmetatable().set(INDEX, new TwoArgFunction(){
					@Override
					public LuaValue call(LuaValue self, LuaValue key){
						switch(key.checkjstring()){
							
							default:
								return NIL;
						}
					}
				});
			library.getmetatable().set(NEWINDEX, new ThreeArgFunction(){
					@Override
					public LuaValue call(LuaValue self, LuaValue key, LuaValue value){
						switch(key.checkjstring()){
							
							default:
								return TRUE;
						}
						//return NONE;
					}
				});*/
			new LuaClosure(setup, getGlobals()).call();
			
			ScriptGlob.S.setLibToEnv(library, env);
			return env;
		}
	}
}
