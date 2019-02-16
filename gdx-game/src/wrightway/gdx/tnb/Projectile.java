package wrightway.gdx.tnb;

import wrightway.gdx.*;
import com.badlogic.gdx.files.*;
import com.badlogic.gdx.maps.tiled.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.g2d.*;

public class Projectile extends WActor.WTexture{
	EnemyWeapon owner;
	String id;
	float cooldown = 0, timeExisted;
	boolean touch;

	Projectile(TiledMapTile tile, float x, float y, boolean touch, int id, EnemyWeapon owner){
		super(tile.getTextureRegion());
		this.touch = touch;
		this.owner = owner;
		this.id = "_temp"+owner.filename+id;
		
		TileMap map = Tenebrae.player.map;
		setSize(map.tilewidth, map.tileheight);

		setPosition(x, y);
		
		Tenebrae.verbose("New proj", toString(), getX(), getY(), getWidth(), getHeight(), getScaleX(), getScaleY(), touch);
	}

	public boolean isColliding(){
		return toRect().overlaps(Tenebrae.fight.sprite.toRect());
	}

	@Override
	public void touchDown(float x, float y, int pointer, int button){
		if(!touch)
			return;
		Tenebrae.debug("He touched me");
		owner.owner.enemy.attack(false);
		dispose();
	}
	
	public void move(float delta){}

	@Override
	public void act(float delta){
		super.act(delta);
		
		timeExisted += delta;
		cooldown -= delta;
		move(delta);

		if(timeExisted > 10){
			Tenebrae.debug("Killing projectile");
			dispose();
			return;
		}
		
		if(cooldown < 0 && isColliding()){
			cooldown = 1.5f;
			owner.owner.setStat(id, Character.Stats.str, owner.str);
			owner.owner.attack(false);
		}
	}
	@Override
	public void draw(Batch batch, float parentAlpha){
		if(cooldown < 0)
			super.draw(batch, parentAlpha);
		else if((int)(cooldown*4) % 2 == 0)
			return;
		else
			super.draw(batch, parentAlpha);
	}

	@Override
	public String toString(){
		return super.toString()+"§"+id;
	}
	
	
	public static class LinearProjectile extends Projectile{
		float velX, velY;
		LinearProjectile(TiledMapTile tile, float x, float y, float velX, float velY, boolean touch, int id, EnemyWeapon owner){
			super(tile, x, y, touch, id, owner);
			this.velX = velX;
			this.velY = velY;
		}
		@Override
		public void move(float delta){
			moveBy(velX*delta, velY*delta);
		}
	}
}
