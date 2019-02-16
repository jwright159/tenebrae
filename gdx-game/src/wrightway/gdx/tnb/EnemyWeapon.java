package wrightway.gdx.tnb;

import wrightway.gdx.*;
import wrightway.gdx.JVSValue.Scope;
import wrightway.gdx.JVSValue.WObject;
import wrightway.gdx.JVSValue.Function;
import com.badlogic.gdx.maps.tiled.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;

public class EnemyWeapon implements Disposable,JVSValue{
	Character owner;
	String filename, type;
	float str, spawnX, spawnY, targetX, targetY, speed, scale;
	int id = 0;
	TiledMapTileSet tileset;
	boolean touch;
	Scope vars;

	EnemyWeapon(Character owner, String filename){
		vars = new Scope(null, "ew");
		this.owner = owner;
		this.filename = filename;
		tileset = Tenebrae.mp.loadTileset(filename);
		/*vars.put("spawn", new JSParser.Function("spawn", "projectile"){public Object run(Scope.Scoped scope){
		 Projectile projectile = (Projectile)scope.get("projectile");
		 Tenebrae.fight.spawn(projectile);
		 return null;
		 }
		 }}));*/
		vars.put("atk", new JVSValue.WValue(){
				@Override
				public Object get(){
					return str;
				}
				@Override
				public void put(Object value){
					str = value;
				}
			});
		vars.put("spawnX", new JVSValue.WValue(){
				@Override
				public Object get(){
					return spawnX;
				}
				@Override
				public void put(Object value){
					spawnX = value;
				}
			});
		vars.put("spawnY", new JVSValue.WValue(){
				@Override
				public Object get(){
					return spawnY;
				}
				@Override
				public void put(Object value){
					spawnY = value;
				}
			});
		vars.put("targetX", new JVSValue.WValue(){
				@Override
				public Object get(){
					return targetX;
				}
				@Override
				public void put(Object value){
					targetX = value;
				}
			});
		vars.put("targetY", new JVSValue.WValue(){
				@Override
				public Object get(){
					return targetY;
				}
				@Override
				public void put(Object value){
					targetY = value;
				}
			});
		vars.put("speed", new JVSValue.WValue(){
				@Override
				public Object get(){
					return speed;
				}
				@Override
				public void put(Object value){
					speed = value;
				}
			});
		vars.put("scale", new JVSValue.WValue(){
				@Override
				public Object get(){
					return scale;
				}
				@Override
				public void put(Object value){
					scale = value;
				}
			});
		vars.put("touch", new JVSValue.WValue(){
				@Override
				public Object get(){
					return touch;
				}
				@Override
				public void put(Object value){
					touch = value;
				}
			});
		vars.put("type", new JVSValue.WValue(){
				@Override
				public Object get(){
					return type;
				}
				@Override
				public void put(Object value){
					type = value.toString();
				}
			});
		vars.put("this", new JVSValue.WValue(){
				@Override
				public Object get(){
					return this;
				}
			});
		vars.put("map", new JVSValue.WValue(){
				@Override
				public Object get(){
					return Tenebrae.mp;
				}
			});
		vars.put("owner", new JVSValue.WValue(){
				@Override
				public Object get(){
					return EnemyWeapon.this.owner;
				}
			});
	}

	public void spawn(){
		vars.run("onSpawn");
		Projectile proj;
		if(type.equals("linear")){
			float velX = targetX - spawnX;
			float velY = targetY - spawnY;
			float mag = (float)Math.hypot(velX, velY);
			velX = velX / mag * speed;
			velY = velY / mag * speed;
			proj = new Projectile.LinearProjectile(tileset.getTile(1), spawnX, spawnY, velX, velY, touch, id++, this);
		}else{
			proj = new Projectile(tileset.getTile(1), spawnX, spawnY, touch, id++, this);
		}
		proj.setScale(scale);
		Tenebrae.debug("Spawning", spawnX, spawnY, proj);
		if(Tenebrae.fight == null)
			proj.dispose();
		else
			Tenebrae.fight.spawn(proj);
	}
	
	@Override
	public Object get(Scope scope){
		return vars;
	}

	@Override
	public void dispose(){

	}
}
