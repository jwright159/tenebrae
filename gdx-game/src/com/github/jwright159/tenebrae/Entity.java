package com.github.jwright159.tenebrae;

import com.github.jwright159.gdx.*;
import com.github.jwright159.gdx.actor.*;
import com.github.jwright159.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.maps.tiled.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.*;
import com.badlogic.gdx.maps.*;
import com.badlogic.gdx.maps.objects.*;
import com.badlogic.gdx.maps.tiled.objects.*;
import com.badlogic.gdx.scenes.scene2d.*;

public abstract class Entity extends ScreenActor implements Comparable<Entity>, Disposable{
	public static final String ENTITY = "__entity";

	protected Tenebrae game;
	public LuaTable vars;
	private float x, y, originX, originY, tileWidth, tileHeight;
	public float lifetime;
	
	public Entity(Tenebrae game, float x, float y, float tileWidth, float tileHeight){
		this.game = game;
		vars = LuaValue.tableOf();
		new EntityLib().call(LuaValue.valueOf(""), vars);
		setTileScalar(tileWidth, tileHeight);
		setTilePosition(x, y);
	}

	public void setTileScalar(float tileWidth, float tileHeight){
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
		setTilePosition(x, y);
		setTileOrigin(originX, originY);
	}
	public float getTileScalarWidth(){
		return tileWidth;
	}
	public float getTileScalarHeight(){
		return tileHeight;
	}
	public void setTileX(float x){
		this.x = x;
		setX(x * tileWidth);
	}
	public float getTileX(){
		return x;
	}
	public void setTileY(float y){
		this.y = y;
		setY(y * tileHeight);
	}
	public float getTileY(){
		return y;
	}
	public void setTilePosition(float x, float y){
		setTileX(x);
		setTileY(y);
	}
	public void moveTileBy(float dx, float dy){
		setTileX(x + dx);
		setTileY(y + dy);
	}
	public void setTileOriginX(float originX){
		this.originX = originX;
		setOriginX(originX * tileWidth);
	}
	public float getTileOriginX(){
		return originX;
	}
	public void setTileOriginY(float originY){
		this.originY = originY;
		setOriginY(originY * tileHeight);
	}
	public float getTileOriginY(){
		return originY;
	}
	public void setTileOrigin(float originX, float originY){
		setTileOriginX(originX);
		setTileOriginY(originY);
	}

	@Override
	public int compareTo(Entity c){
		return y > c.y ? -1 : 1;
	}

	public void act(float delta, float time){
		LuaValue act = vars.get("act");
		if(!act.isnil())
			act.call(vars, LuaValue.valueOf(delta), LuaValue.valueOf(time));
	}
	@Override
	public void act(float delta){
		super.act(delta);
		lifetime += delta;
		act(delta, lifetime);
	}

	@Override
	public String toString(){
		return super.toString() + "§" + "{" + x + "x" + y + "}";
	}

	public class EntityLib extends TwoArgFunction{
		@Override
		public LuaValue call(LuaValue modname, final LuaValue env){
			LuaTable library = tableOf();

			library.set("setColor", new VarArgFunction(){
					@Override
					public Varargs invoke(Varargs args){
						setColor((float)args.checkdouble(1), (float)args.checkdouble(2), (float)args.checkdouble(3), (float)args.optdouble(4, 1));
						return NONE;
					}
				});
			library.set("setAlpha", new OneArgFunction(){
					@Override
					public LuaValue call(LuaValue alpha){
						setColor(getColor().r, getColor().g, getColor().b, (float)alpha.checkdouble());
						return NONE;
					}
				});
			library.setmetatable(tableOf());
			library.getmetatable().set(INDEX, new TwoArgFunction(){
					@Override
					public LuaValue call(LuaValue self, LuaValue key){
						switch(key.checkjstring()){
							case "x":
								return valueOf(getTileX());
							case "y":
								return valueOf(getTileY());
							case "rotation":
								return valueOf(getRotation());
							case "originX":
								return valueOf(getTileOriginX());
							case "originY":
								return valueOf(getTileOriginY());
							default:
								return NIL;
						}
					}
				});
			library.getmetatable().set(NEWINDEX, new ThreeArgFunction(){
					@Override
					public LuaValue call(LuaValue self, LuaValue key, LuaValue value){
						switch(key.checkjstring()){
							case "x":
								setTileX((float)value.checkdouble());
								break;
							case "y":
								setTileY((float)value.checkdouble());
								break;
							case "rotation":
								setRotation((float)value.checkdouble());
								break;
							case "originX":
								setTileOriginX((float)value.checkdouble());
								break;
							case "originY":
								setTileOriginY((float)value.checkdouble());
								break;
							default:
								return TRUE;
						}
						return NONE;
					}
				});
			library.getmetatable().set(ENTITY, CoerceJavaToLua.coerce(Entity.this));

			ScriptGlob.S.setLibToEnv(library, env);
			return env;
		}
	}
	
	public static class TextureEntity extends Entity{
		private TextureActor actor;
		private float width, height;
		private TiledMapTile tile;
		private MapObject mapobj; // Either RectMapObj or TileMapObj
		private ScreenActor tap;
		//private Array<Trigger> trigs; // Do this if u want the longer prop searching
		private MapProperties props; // Just for triggers
		
		public TextureEntity(Tenebrae game, float x, float y, float width, float height, float tileWidth, float tileHeight){
			super(game, x, y, tileWidth, tileHeight);
			new TextureEntityLib().call(LuaValue.valueOf(""), vars);
			actor = new TextureActor();
			/*trigs = new Array<Trigger>();*/
			props = new MapProperties();
			//props.put("onTouch", "onTouch");
			props.put("onTrigger", "onTrigger");
			tap = new ScreenActor(){
				@Override
				public void tap(float x, float y, int count, int button){
					super.tap(x, y, count, button);
					LuaValue tap = vars.get("onTap");
					if(!tap.isnil())
						tap.call(vars);
				}
				@Override
				public void draw(Batch batch, float parentAlpha){
					// Just don't do anything
				}
			};
			//tap.setDebug(true);
			setTileBounds(x, y, width, height);
		}
		public TextureEntity(Tenebrae game, float x, float y, float width, float height, float tileWidth, float tileHeight, TextureRegion region){
			this(game, x, y, width, height, tileWidth, tileHeight);
			setRegion(0, region);
		}
		
		@Override
		public void setTileScalar(float tileWidth, float tileHeight){
			super.setTileScalar(tileWidth, tileHeight);
			setTileSize(width, height);
		}
		
		public float getTileX(int align){
			if(Align.isLeft(align))
				return getTileX();
			else if(Align.isRight(align))
				return getTileX() + width;
			else
				return getTileX() + width / 2;
		}
		public float getTileY(int align){
			if(Align.isBottom(align))
				return getTileY();
			else if(Align.isTop(align))
				return getTileY() + height;
			else
				return getTileY() + height / 2;
		}
		public void setTileWidth(float width){
			this.width = width;
			if(getWidth() != 0)
				setScaleX(width * getTileScalarWidth() / getWidth());
		}
		public float getTileWidth(){
			return width;
		}
		public void setTileHeight(float height){
			this.height = height;
			if(getHeight() != 0)
				setScaleY(height * getTileScalarHeight() / getHeight());
		}
		public float getTileHeight(){
			return height;
		}
		public void setTileSize(float width, float height){
			setTileWidth(width);
			setTileHeight(height);
		}
		public void setTileBounds(float x, float y, float width, float height){
			setTileX(x);
			setTileY(y);
			setTileWidth(width);
			setTileHeight(height);
		}
		private Rectangle tileRect = new Rectangle();
		public Rectangle toTileRect(){
			tileRect.set(getTileX(), getTileY(), getTileWidth(), getTileHeight());
			return tileRect;
		}

		public void setRegion(TextureRegion newRegion){
			actor.setRegion(newRegion);
			validateRegion();
		}
		public void setRegion(int i, TextureRegion newRegion){
			actor.setRegion(i, newRegion);
			validateRegion();
		}
		public TextureRegion removeRegion(int i){
			TextureRegion region = actor.removeRegion(i);
			validateRegion();
			return region;
		}
		public void setRegions(LayeredTextureRegion region){
			actor.setRegions(region);
			validateRegion();
		}
		public LayeredTextureRegion getRegions(){
			return actor.getRegions();
		}
		private void validateRegion(){
			setSize(actor.getWidth(), actor.getHeight());
			setTileSize(width, height);
		}

		public void setMapObject(MapObject mapobj){
			this.mapobj = mapobj;
			/*trigs.clear();
			 if(mapobj instanceof RectangleMapObject)
			 trigs.add(new Trigger(((RectangleMapObject)mapobj).getRectangle(), mapobj.getProperties()));
			 else if(mapobj instanceof TiledMapTileMapObject){
			 TiledMapTileMapObject obj = (TiledMapTileMapObject)mapobj;
			 trigs.add(new Trigger(new Rectangle(0, 0, obj.getTextureRegion().getRegionWidth()*obj.getScaleX(), obj.getTextureRegion().getRegionHeight()*obj.getScaleY()), obj.getProperties()));
			 }*/
		}
		/*public MapObject getMapObject(){
		 return mapobj;
		 }*/
		public boolean hasMapObject(){
			return mapobj != null;
		}

		public boolean isInMapObjects(MapObjects objs){
			return objs.getIndex(mapobj) != -1;
		}

		public Trigger getTrigger(String prop){
			/*for(Trigger trig : trigs)
			 if(trig.hasProperty(prop))
			 return trig;
			 return null;*/
			return !vars.get(prop).isnil() ? new Trigger(game, toRect(), props) : null;
		}
		
		private static Vector2 coordBuffer = new Vector2(), sizeBuffer = new Vector2();
		public void moveTapActor(){
			localToScreenCoordinates(coordBuffer.set(0, 0));
			localToScreenCoordinates(sizeBuffer.set(getWidth(), getHeight()));
			sizeBuffer.sub(coordBuffer).scl(1, -1); // sizeBuffer gives top-right point
			coordBuffer.y = game.getStage().getViewport().getScreenHeight() - coordBuffer.y - game.getStage().getViewport().getBottomGutterHeight();// + game.getStage().getViewport().getLeftGutterWidth(); // screen is y-down // also gutters are messing stuff up???
			//Log.debug(getWidth(), getScaleX(), getTileScalarWidth(), getTileX(), getTrueWidth(), coordBuffer, sizeBuffer);
			tap.setBounds(coordBuffer.x, coordBuffer.y, sizeBuffer.x, sizeBuffer.y);
		}

		@Override
		public void act(float delta, float time){
			super.act(delta, time);
			if(game.player.collide && toTileRect().overlaps(game.player.toTileRect())){
				LuaValue onTouch = vars.get("onTouch");
				if(!onTouch.isnil())
					onTouch.call(vars);
			}

			if(tap.getStage() != null)
				moveTapActor();

			if(tile != null)
				setRegion(tile.getTextureRegion());
		}

		@Override
		public void draw(Batch batch, float parentAlpha){
			//Log.debug("Drawing", this, getX(), getY()+"\n"+ batch.getTransformMatrix());
			Utils.setActorFromActor(actor, this); // Could be greatly optimized
			actor.draw(batch, parentAlpha);
		}
		
		@Override
		public String toString(){
			return super.toString() + "§" + "{" + width + "x" + height + "}";
		}
		
		@Override
		public void dispose(){
			super.dispose();
			tap.dispose();
			actor.dispose();
		}
		
		public class TextureEntityLib extends TwoArgFunction{
			@Override
			public LuaValue call(LuaValue modname, final LuaValue env){
				LuaTable library = tableOf();

				library.set("setTexture", new TwoArgFunction(){
						@Override
						public LuaValue call(LuaValue tsx, LuaValue tid){
							if(tid.isnil()){
								tile = null;
								setRegion(game.getSkin().getRegion(tsx.checkjstring()));
							}else{
								TiledMapTileSet tileset = game.mappack.loadTileset(tsx.checkjstring());
								tile = tileset.getTile(tid.checkint());
								if(tile == null)
									env.error("no tile with id "+tid.checkint());
								setRegion(tile.getTextureRegion());
							}
							return NONE;
						}
					});
				library.setmetatable(tableOf());
				library.getmetatable().set(INDEX, new TwoArgFunction(){
						@Override
						public LuaValue call(LuaValue self, LuaValue key){
							switch(key.checkjstring()){
								case "width":
									return valueOf(getTileWidth());
								case "height":
									return valueOf(getTileHeight());
								default:
									return NIL;
							}
						}
					});
				library.getmetatable().set(NEWINDEX, new ThreeArgFunction(){
						@Override
						public LuaValue call(LuaValue self, LuaValue key, LuaValue value){
							switch(key.checkjstring()){
								case "width":
									setTileWidth((float)value.checkdouble());
									break;
								case "height":
									setTileHeight((float)value.checkdouble());
									break;
								case "onTap":
									if(!value.isnil())
										game.getUiStage().addActor(tap);
									else
										tap.remove();
									return TRUE;
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
	
	public static class GroupEntity extends Entity{
		private TransGroup group;
		
		public GroupEntity(Tenebrae game, float x, float y, float tileWidth, float tileHeight){
			super(game, x, y, tileWidth, tileHeight);
			new GroupEntityLib().call(LuaValue.valueOf(""), vars);
			group = new TransGroup();
		}
		
		public void addEntity(Entity ent){
			group.addActor(ent);
		}

		@Override
		public void act(float delta, float time){
			super.act(delta, time);
			group.act(delta);
		}

		@Override
		public void draw(Batch batch, float parentAlpha){
			//Log.debug("Drawing", this, getX(), getY()+"\n"+ batch.getTransformMatrix());
			Utils.setActorFromActor(group, this);
			// Problem: when batch computes transformation matrix, also premuls with parent,
			//          but this group has no parent so the whole transformatrix gets set to only what the group has
			group.draw(batch, parentAlpha);
		}
		
		public class GroupEntityLib extends TwoArgFunction{
			@Override
			public LuaValue call(LuaValue modname, final LuaValue env){
				LuaTable library = tableOf();

				library.set("add", new OneArgFunction(){
						@Override
						public LuaValue call(LuaValue actor){
							group.addActor((Actor)actor.getmetatable().get(Entity.ENTITY).checkuserdata(Actor.class));
							return NONE;
						}
					});
				library.set("remove", new OneArgFunction(){
						@Override
						public LuaValue call(LuaValue actor){
							group.removeActor((Actor)actor.getmetatable().get(Entity.ENTITY).checkuserdata(Actor.class));
							return NONE;
						}
					});
				library.setmetatable(tableOf());
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
					});

				ScriptGlob.S.setLibToEnv(library, env);
				return env;
			}
		}
		
		public static class TransGroup extends Group{
			@Override
			public void draw(Batch batch, float parentAlpha){
				applyTransform(batch, computeTransform().mulLeft(batch.getTransformMatrix()));
				drawChildren(batch, parentAlpha);
				resetTransform(batch);
			}
		}
	}
}
