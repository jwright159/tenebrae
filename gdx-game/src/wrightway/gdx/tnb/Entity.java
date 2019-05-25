package wrightway.gdx.tnb;

import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.maps.tiled.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.*;
import wrightway.gdx.*;
import com.badlogic.gdx.maps.*;
import com.badlogic.gdx.maps.objects.*;
import com.badlogic.gdx.maps.tiled.objects.*;
import com.badlogic.gdx.scenes.scene2d.*;

public class Entity extends TextureActor implements Comparable<Entity>{
	public static final String ENTITY = "__entity";

	public LuaTable vars;
	private float x, y, width, height, tileWidth, tileHeight;
	public float lifetime;
	private TiledMapTile tile;
	private MapObject mapobj; // Either RectMapObj or TileMapObj
	//private Array<Trigger> trigs; // Do this if u want the longer prop searching
	private MapProperties props; // Just for triggers
	private WActor tap;
	
	public Entity(float x, float y, float width, float height, float tileWidth, float tileHeight){
		vars = LuaValue.tableOf();
		new EntityLib().call(LuaValue.valueOf(""), vars);
		/*trigs = new Array<Trigger>();*/
		props = new MapProperties();
		//props.put("onTouch", "onTouch");
		props.put("onTrigger", "onTrigger");
		tap = new WActor(){
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
		setTileScalar(tileWidth, tileHeight);
		setTileBounds(x, y, width, height);
	}
	public Entity(float x, float y, float width, float height, float tileWidth, float tileHeight, TextureRegion region){
		this(x, y, width, height, tileWidth, tileHeight);
		setRegion(0, region);
	}

	public void setTileScalar(float tileWidth, float tileHeight){
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
		setTileBounds(x, y, width, height);
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
	public float getTileX(int align){
		if(Align.isLeft(align))
			return x;
		else if(Align.isRight(align))
			return x + width;
		else
			return x + width / 2;
	}
	public void setTileY(float y){
		this.y = y;
		setY(y * tileHeight);
	}
	public float getTileY(){
		return y;
	}
	public float getTileY(int align){
		if(Align.isBottom(align))
			return y;
		else if(Align.isTop(align))
			return y + height;
		else
			return y + height / 2;
	}
	public void setTilePosition(float x, float y){
		setTileX(x);
		setTileY(y);
	}
	public void setTileWidth(float width){
		this.width = width;
		if(getWidth() != 0)
			setScaleX(width * tileWidth / getWidth());
	}
	public float getTileWidth(){
		return width;
	}
	public void setTileHeight(float height){
		this.height = height;
		if(getHeight() != 0)
			setScaleY(height * tileHeight / getHeight());
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
	public void moveTileBy(float dx, float dy){
		setTileX(x + dx);
		setTileY(y + dy);
	}
	private Rectangle tileRect = new Rectangle();
	public Rectangle toTileRect(){
		tileRect.set(getTileX(), getTileY(), getTileWidth(), getTileHeight());
		return tileRect;
	}

	@Override
	public void setRegion(int i, TextureRegion newRegion){
		super.setRegion(i, newRegion);
		setTileSize(width, height);
	}
	@Override
	public TextureRegion removeRegion(int i){
		TextureRegion region = super.removeRegion(i);
		setTileSize(width, height);
		return region;
	}

	@Override
	public void setRegions(LayeredTextureRegion region){
		super.setRegions(region);
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
		return !vars.get(prop).isnil() ? new Trigger(toRect(), props) : null;
	}

	@Override
	public int compareTo(Entity c){
		return y > c.y ? -1 : 1;
	}
	
	private static Vector2 coordBuffer = new Vector2(), sizeBuffer = new Vector2();
	public void moveTapActor(){
		localToScreenCoordinates(coordBuffer.set(0, 0));
		localToScreenCoordinates(sizeBuffer.set(getWidth(), getHeight()));
		sizeBuffer.sub(coordBuffer).scl(1, -1); // sizeBuffer gives top-right point
		coordBuffer.y = Tenebrae.t.getStage().getViewport().getScreenHeight() - coordBuffer.y - Tenebrae.t.getStage().getViewport().getBottomGutterHeight();// + Tenebrae.t.getStage().getViewport().getLeftGutterWidth(); // screen is y-down // also gutters are messing stuff up???
		//Log.debug(getWidth(), getScaleX(), getTileScalarWidth(), getTileX(), getTrueWidth(), coordBuffer, sizeBuffer);
		tap.setBounds(coordBuffer.x, coordBuffer.y, sizeBuffer.x, sizeBuffer.y);
	}

	public void act(float delta, float time){
		LuaValue act = vars.get("act");
		if(!act.isnil())
			act.call(vars, LuaValue.valueOf(delta), LuaValue.valueOf(time));
		
		if(Tenebrae.player.collide && toTileRect().overlaps(Tenebrae.player.toTileRect())){
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
	public void act(float delta){
		super.act(delta);
		lifetime += delta;
		act(delta, lifetime);
	}

	@Override
	public String toString(){
		return super.toString() + "§" + "{" + x + "x" + y + ", " + width + "x" + height + ", " + getScaleX() + "x" + getScaleY() + "}";
	}

	@Override
	public void dispose(){
		super.dispose();
		tap.dispose();
	}

	public class EntityLib extends TwoArgFunction{
		@Override
		public LuaValue call(LuaValue modname, final LuaValue env){
			LuaTable library = tableOf();

			library.set("setTexture", new TwoArgFunction(){
					@Override
					public LuaValue call(LuaValue tsx, LuaValue tid){
						if(tid.isnil()){
							tile = null;
							setRegion(Tenebrae.t.getSkin().getRegion(tsx.checkjstring()));
						}else{
							TiledMapTileSet tileset = Tenebrae.mp.loadTileset(tsx.checkjstring());
							tile = tileset.getTile(tid.checkint());
							if(tile == null)
								env.error("no tile with id "+tid.checkint());
							setRegion(tile.getTextureRegion());
						}
						return NONE;
					}
				});
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
								return valueOf(x);
							case "y":
								return valueOf(y);
							case "width":
								return valueOf(width);
							case "height":
								return valueOf(height);
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
								x = (float)value.checkdouble();
								setX(x * Tenebrae.player.map.tileWidth);
								break;
							case "y":
								y = (float)value.checkdouble();
								setY(y * Tenebrae.player.map.tileHeight);
								break;
							case "width":
								width = (float)value.checkdouble();
								setWidth(width * Tenebrae.player.map.tileWidth);
								break;
							case "height":
								height = (float)value.checkdouble();
								setHeight(height * Tenebrae.player.map.tileHeight);
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
							case "onTap":
								if(!value.isnil())
									Tenebrae.t.getUiStage().addActor(tap);
								else
									tap.remove();
								return TRUE;
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
}
