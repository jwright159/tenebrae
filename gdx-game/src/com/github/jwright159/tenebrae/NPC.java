package com.github.jwright159.tenebrae;

import com.github.jwright159.gdx.*;
import com.github.jwright159.tenebrae.Action.*;
import com.badlogic.gdx.files.*;
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

	public NPC(Tenebrae game, String name, Prototype script){
		super(game, name);
		getGlobals().load(new NPCLib());
		new LuaClosure(setup, getGlobals()).call();
		new LuaClosure(script, getGlobals()).call();
		hp = maxhp();
		mp = maxmp();
		updateBoxHP();
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
	public void draw(Batch batch, float parentAlpha){
		//Log.debug("Drawing", this);
		if(enabled)
			super.draw(batch, parentAlpha);
	}

	@Override
	public String toString(){
		return super.toString()+","+enabled;
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
								boolean had = game.mappack.charas.contains(NPC.this, true);
								if(!had){
									game.mappack.charas.add(NPC.this);
									game.getUiStage().addActor(smolStatBox);
								}
								return valueOf(!had);
							}
						});
			library.set("disable", new ZeroArgFunction(){
							@Override
							public LuaValue call(){
								enabled = false;
								boolean had = game.mappack.charas.contains(NPC.this, true);
								if(had){
									game.mappack.charas.removeValue(NPC.this, true);
									smolStatBox.remove();
								}
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
			
			ScriptGlob.S.setLibToEnv(library, env);
			return env;
		}
	}
}
