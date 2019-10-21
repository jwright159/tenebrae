package com.github.jwright159.gdx;

import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.*;
import org.luaj.vm2.compiler.*;
import com.badlogic.gdx.*;
import java.io.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.files.*;

public interface ScriptGlob{
	public Globals getGlobals()

	public static class StdGlobals extends Globals{
		public StdGlobals(final FileHandle extResFolder){
			load(new JseBaseLib());
			load(new PackageLib());
			load(new Bit32Lib());
			load(new TableLib());
			load(new StringLib());
			load(new JseMathLib());
			load(new LogLib());
			
			// i know i know its a risk but who cares
			LoadState.install(this);
			LuaC.install(this);
			
			finder = new ResourceFinder(){
				public InputStream findResource(String name){
					try{
						if(name.startsWith("assets/"))
							return Gdx.files.internal(name.substring(name.indexOf('/') + 1)).read();
						return extResFolder.child(name).read();
					}
					catch(GdxRuntimeException e){return null;}
				}
			};
			rawset("inspect", get("require").call("assets.inspect"));
		}
	}
	public static class ServerGlobals extends Globals{
		public ServerGlobals(){
			load(new JseBaseLib());
			load(new PackageLib());
			load(new StringLib());

			// To load scripts, we occasionally need a math library in addition to compiler support.
			// To limit scripts using the debug library, they must be closures, so we only install LuaC.
			load(new JseMathLib());
			LoadState.install(this);
			LuaC.install(this);
		}
	}
	
	public class LogLib extends TwoArgFunction{
		@Override
		public LuaValue call(LuaValue modname, LuaValue env){
			env.set("println", new VarArgFunction(){
					@Override
					public Varargs invoke(Varargs args){
						Log.userLog(args);
						return NONE;
					}
				});
			env.set("print", env.get("println"));
			env.set("errorln", new VarArgFunction(){
					@Override
					public Varargs invoke(Varargs args){
						Log.log((byte)0b000_0011, args);
						return NONE;
					}
				});
			return env;
		}
	}

	public static LuaValue defindex = new TwoArgFunction(){
		@Override
		public LuaValue call(LuaValue self, LuaValue key){
			return self.rawget(key);
		}
	}, defnewindex = new ThreeArgFunction(){
		@Override
		public LuaValue call(LuaValue self, LuaValue key, LuaValue value){
			self.rawset(key, value);
			return NONE;
		}
	};

	public static abstract class S{
		public static void setLibToEnv(final LuaTable lib, final LuaValue env){
			for(LuaValue key : lib.keys())
				env.set(key, lib.get(key));
			if(env.getmetatable() == null)
				env.setmetatable(LuaValue.tableOf());
			if(lib.getmetatable() != null){
				final LuaValue oldmt = env.getmetatable();
				LuaTable newmt = LuaValue.tableOf();
				final LuaValue libmt = lib.getmetatable();
					
				LuaValue k = LuaValue.NIL;
				while(true){
					Varargs en = oldmt.next(k);
					if((k = en.arg1()).isnil())
						break;
					LuaValue v = en.arg(2);
					newmt.rawset(k, v);
				}
				while(true){
					Varargs en = libmt.next(k);
					if((k = en.arg1()).isnil())
						break;
					LuaValue v = en.arg(2);
					newmt.rawset(k, v);
				}

				newmt.set(LuaValue.INDEX, new TwoArgFunction(){
						@Override
						public LuaValue call(LuaValue self, LuaValue key){
							//Log.parseLog("Getting", self, key.tojstring());
							LuaValue value = libmt.get(INDEX).isnil() ? NIL : libmt.get(INDEX).call(lib, key);
							//Log.parseLog("Got?", value.tojstring(), oldmt.get(INDEX).tojstring());
							if(value.isnil())
								value = oldmt.get(INDEX).isnil() ? self.rawget(key) : oldmt.get(INDEX).call(env, key);
							//Log.parseLog("Got", value.tojstring());
							return value;
						}
					});
				newmt.set(LuaValue.NEWINDEX, new ThreeArgFunction(){
						@Override
						public LuaValue call(LuaValue self, LuaValue key, LuaValue value){
							boolean passed = true;
							if(!libmt.get(NEWINDEX).isnil())
								passed = lib.getmetatable().get(NEWINDEX).call(lib, key, value).optboolean(false);
							if(passed && !oldmt.get(NEWINDEX).isnil())
								passed = oldmt.get(NEWINDEX).call(env, key, value).optboolean(false);
							if(passed)
								self.rawset(key, value);
							return NONE;
						}
					});
				env.setmetatable(newmt);
			}//else
				//Log.debug("mt not replaced", env.getmetatable());
		}
	}
}
