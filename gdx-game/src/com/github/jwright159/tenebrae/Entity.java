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
import com.badlogic.gdx.scenes.scene2d.utils.*;

public abstract class Entity extends ScreenActor implements Comparable<Entity>, Disposable{
	public static final String ENTITY = "__entity";

	protected Tenebrae game;
	public LuaTable vars;
	public float lifetime;
	
	public Entity(Tenebrae game, float x, float y){
		this.game = game;
		setPosition(x, y);
		vars = LuaValue.tableOf();
		new EntityLib().call(LuaValue.valueOf(""), vars);
	}

	@Override
	public int compareTo(Entity c){
		return getY() > c.getY() ? -1 : 1;
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
		return super.toString() + "§" + "{" + getX() + "x" + getY() + "}";
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
								return valueOf(getX());
							case "y":
								return valueOf(getY());
							case "rotation":
								return valueOf(getRotation());
							case "originX":
								return valueOf(getOriginX());
							case "originY":
								return valueOf(getOriginY());
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
								setX((float)value.checkdouble());
								break;
							case "y":
								setY((float)value.checkdouble());
								break;
							case "rotation":
								setRotation((float)value.checkdouble());
								break;
							case "originX":
								setOriginX((float)value.checkdouble());
								break;
							case "originY":
								setOriginY((float)value.checkdouble());
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
	
	public static class DrawableEntity extends Entity{
		private Drawable drawable;
		
		private TiledMapTile tile;
		private LayeredTextureRegionDrawable region;
		private NinePatchDrawable patch;
		
		private MapObject mapobj; // Either RectMapObj or TileMapObj
		private ScreenActor tap;
		//private Array<Trigger> trigs; // Do this if u want the longer prop searching
		private MapProperties props; // Just for triggers
		
		public DrawableEntity(Tenebrae game, float x, float y, float width, float height, Drawable drawable){
			super(game, x, y);
			new DrawableEntityLib().call(LuaValue.valueOf(""), vars);
			
			this.drawable = drawable;
			region = drawable instanceof LayeredTextureRegionDrawable ? (LayeredTextureRegionDrawable)drawable : new LayeredTextureRegionDrawable(new LayeredTextureRegion());
			patch = drawable instanceof NinePatchDrawable ? (NinePatchDrawable)drawable : new NinePatchDrawable();
			
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
				@Override
				public void draw(Batch batch){}
			};
			//tap.setDebug(true);
			setBounds(x, y, width, height);
		}

		public void setDrawable(Drawable drawable){
			this.drawable = drawable;
		}
		public Drawable getDrawable(){
			return drawable;
		}
		public void setRegions(LayeredTextureRegion regions){
			this.region.setRegions(regions);
			setDrawable(this.region);
		}
		public void setRegion(TextureRegion region){
			this.region.getRegions().clear();
			this.region.getRegions().set(0, region);
			setDrawable(this.region);
		}
		public void setPatch(NinePatch patch){
			this.patch.setPatch(patch);
			setDrawable(this.patch);
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
			if(game.player.collide && toRect().overlaps(game.player.toRect())){
				LuaValue onTouch = vars.get("onTouch");
				if(!onTouch.isnil())
					onTouch.call(vars);
			}

			if(tap.getStage() != null)
				moveTapActor();

			if(tile != null)
				((LayeredTextureRegionDrawable)getDrawable()).getRegions().set(0, tile.getTextureRegion());
		}

		@Override
		public void draw(Batch batch){
			//Log.debug("Drawing", this, getX(), getY()+"\n"+ batch.getTransformMatrix())
			drawable.draw(batch, 0, 0, getWidth(), getHeight());
		}
		
		@Override
		public String toString(){
			return super.toString() + "§" + "{" + getWidth() + "x" + getHeight() + "," + drawable.toString() + "}";
		}
		
		@Override
		public void dispose(){
			super.dispose();
			tap.dispose();
		}
		
		public class DrawableEntityLib extends TwoArgFunction{
			@Override
			public LuaValue call(LuaValue modname, final LuaValue env){
				LuaTable library = tableOf();

				library.set("setTexture", new TwoArgFunction(){
						@Override
						public LuaValue call(LuaValue tsx, LuaValue tid){
							if(tid.isnumber()){
								TiledMapTileSet tileset = game.mappack.loadTileset(tsx.checkjstring());
								tile = tileset.getTile(tid.checkint());
								if(tile == null)
									env.error("no tile with id "+tid.checkint());
								setRegion(tile.getTextureRegion());
							}else{
								tile = null;
								if(tid.optboolean(false))
									setPatch(game.getSkin().getPatch(tsx.checkjstring()));
								else
									setRegion(game.getSkin().getRegion(tsx.checkjstring()));
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
									return valueOf(getWidth());
								case "height":
									return valueOf(getHeight());
								case "scaleX":
									return valueOf(getScaleX());
								case "scaleY":
									return valueOf(getScaleY());
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
									setWidth((float)value.checkdouble());
									break;
								case "height":
									setHeight((float)value.checkdouble());
									break;
								case "scaleX":
									setScaleX((float)value.checkdouble());
									break;
								case "scaleY":
									setScaleY((float)value.checkdouble());
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
		private Array<Actor> group;
		
		public GroupEntity(Tenebrae game, float x, float y){
			super(game, x, y);
			new GroupEntityLib().call(LuaValue.valueOf(""), vars);
			group = new Array<Actor>();
		}
		
		public void addEntity(Entity ent){
			group.add(ent);
		}

		@Override
		public void act(float delta){
			super.act(delta);
			for(Actor actor : group)
				actor.act(delta);
		}

		@Override
		public void draw(Batch batch){
			//Log.debug("Drawing", this, getX(), getY()+"\n"+ batch.getTransformMatrix());
			// Problem: when batch computes transformation matrix, also premuls with parent,
			//          but this group has no parent so the whole transformatrix gets set to only what the group has
			// haha now im doing that for all screenactors thanks
			
			for(Actor actor : group)
				actor.draw(batch, 1);
		}
		
		public class GroupEntityLib extends TwoArgFunction{
			@Override
			public LuaValue call(LuaValue modname, final LuaValue env){
				LuaTable library = tableOf();

				library.set("add", new OneArgFunction(){
						@Override
						public LuaValue call(LuaValue actor){
							group.add((Actor)actor.getmetatable().get(Entity.ENTITY).checkuserdata(Actor.class));
							return NONE;
						}
					});
				library.set("remove", new OneArgFunction(){
						@Override
						public LuaValue call(LuaValue actor){
							group.removeValue((Actor)actor.getmetatable().get(Entity.ENTITY).checkuserdata(Actor.class), true);
							return NONE;
						}
					});
				library.setmetatable(tableOf());
				library.getmetatable().set(INDEX, new TwoArgFunction(){
						@Override
						public LuaValue call(LuaValue self, LuaValue key){
							switch(key.checkjstring()){
								case "scaleX":
									return valueOf(getScaleX());
								case "scaleY":
									return valueOf(getScaleY());
								default:
									return NIL;
							}
						}
					});
				library.getmetatable().set(NEWINDEX, new ThreeArgFunction(){
						@Override
						public LuaValue call(LuaValue self, LuaValue key, LuaValue value){
							switch(key.checkjstring()){
								case "scaleX":
									setScaleX((float)value.checkdouble());
									break;
								case "scaleY":
									setScaleY((float)value.checkdouble());
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
}
