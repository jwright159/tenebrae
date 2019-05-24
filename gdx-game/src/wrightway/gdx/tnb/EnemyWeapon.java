package wrightway.gdx.tnb;

import wrightway.gdx.*;
import com.badlogic.gdx.maps.tiled.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;

public class EnemyWeapon implements Disposable,ScriptGlob{
	public Character owner;
	public String type;
	public float str, spawnX, spawnY, targetX, targetY, speed, scale;
	public int id = 0;
	private TiledMapTileSet tileset;
	public boolean touch;
	private Globals globals;

	EnemyWeapon(Character owner, String filename){
		globals = new Tenebrae.StdEntGlobals();
		this.owner = owner;
		tileset = Tenebrae.mp.loadTileset(filename);
		/*vars.put("spawn", new JSParser.Function("spawn", "projectile"){public Object run(Scope.Scoped scope){
		 Projectile projectile = (Projectile)scope.get("projectile");
		 Tenebrae.fight.spawn(projectile);
		 return null;
		 }
		 }}));*/
	}

	public void spawn(){
		globals.get("onSpawn").checkfunction().call();
		Entity proj = null;
		if(type.equals("linear")){
			float velX = targetX - spawnX;
			float velY = targetY - spawnY;
			float mag = (float)Math.hypot(velX, velY);
			velX = velX / mag * speed;
			velY = velY / mag * speed;
			//proj = new Projectile.LinearProjectile(tileset.getTile(1), spawnX, spawnY, velX, velY, touch, id++, this);
		}else{
			//proj = new Projectile(tileset.getTile(1), spawnX, spawnY, touch, id++, this);
		}
		proj.setScale(scale);
		Log.debug("Spawning", spawnX, spawnY, proj);
		//if(Tenebrae.fight == null)
			proj.dispose();
		//else
			//Tenebrae.fight.spawn(proj);
	}
	
	@Override
	public Globals getGlobals(){
		return globals;
	}

	@Override
	public void dispose(){

	}
	
	public class EnenmyWeaponLib extends TwoArgFunction{
		@Override
		public LuaValue call(LuaValue modname, LuaValue env){
			LuaTable library = tableOf();
			
			library.setmetatable(tableOf());
			library.getmetatable().set(INDEX, new TwoArgFunction(){
					@Override
					public LuaValue call(LuaValue self, LuaValue key){
						switch(key.checkjstring()){
							case "owner":
								return owner.getGlobals();
							case "atk":
								return valueOf(str);
							case "spawnX":
								return valueOf(spawnX);
							case "spawnY":
								return valueOf(spawnY);
							case "targetX":
								return valueOf(targetX);
							case "targetY":
								return valueOf(targetY);
							case "speed":
								return valueOf(speed);
							case "scale":
								return valueOf(scale);
							case "touch":
								return valueOf(touch);
							case "type":
								return valueOf(type);
							default:
								return NIL;
						}
					}
				});
			library.getmetatable().set(NEWINDEX, new ThreeArgFunction(){
					@Override
					public LuaValue call(LuaValue self, LuaValue key, LuaValue value){
						switch(key.checkjstring()){
							case "owner":
								self.error("owner is read-only");
								break;
							case "atk":
								str = (float)value.checkdouble();
								break;
							case "spawnX":
								spawnX = (float)value.checkdouble();
								break;
							case "spawnY":
								spawnY = (float)value.checkdouble();
								break;
							case "targetX":
								targetX = (float)value.checkdouble();
								break;
							case "targetY":
								targetY = (float)value.checkdouble();
								break;
							case "speed":
								speed = (float)value.checkdouble();
								break;
							case "scale":
								scale = (float)value.checkdouble();
								break;
							case "touch":
								touch = value.checkboolean();
								break;
							case "type":
								type = value.checkjstring();
								break;
							default:
								return TRUE;
						}
						return NONE;
					}
				});
				
			ScriptGlob.S.setLibToEnv(library, env);
			return env;
		}
	}
}
