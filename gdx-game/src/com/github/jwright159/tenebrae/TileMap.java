package com.github.jwright159.tenebrae;

import com.github.jwright159.gdx.*;
import com.github.jwright159.gdx.actor.*;
import com.github.jwright159.gdx.tiled.*;
import com.badlogic.gdx.*;
import com.badlogic.gdx.assets.loaders.resolvers.*;
import com.badlogic.gdx.files.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.maps.*;
import com.badlogic.gdx.maps.objects.*;
import com.badlogic.gdx.maps.tiled.*;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.maps.tiled.renderers.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.maps.tiled.objects.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.scenes.scene2d.*;
import org.luaj.vm2.*;
import java.util.*;

public class TileMap extends ScreenActor{
	private Tenebrae game;
	private String filepath;
	private TiledMap map;
	private OrthogonalTiledMapRenderer maprenderer;
	private float tileWidth, tileHeight;
	public int width, height;
	private float offsetX, offsetY;
	private Group ents;
	public static String EMPTY_PATH = Gdx.files.internal("empty.tmx").path();
	private LuaFunction script;
	public float lifetime;

	private Rectangle bound;
	public boolean setBoundToBigDZLater = false;

	public TileMap(Tenebrae game, FileHandle mapFile, LuaFunction script, Batch batch){
		this.game = game;
		this.script = script;
		ents = new Group(){
			@Override
			public void act(float delta){
				super.act(delta);
				getChildren().sort();
			}

			@Override
			public void draw(Batch batch, float parentAlpha){
				// We do this
			}
		};

		boolean exists = mapFile != null && mapFile.exists();
		filepath = exists ? mapFile.path() : EMPTY_PATH;

		TmxMapLoader.Parameters params = new TmxMapLoader.Parameters();
		params.generateMipMaps = true;
		map = new TmxMapLoader(exists ? new ExternalFileHandleResolver() : new InternalFileHandleResolver()).load(filepath, params);
		//Tenebrae.debug("Making map! "+file.nameWithoutExtension());

		TiledMapTileLayer tilelayer = (TiledMapTileLayer)getCollisionLayers().get(0);
		tileWidth = tilelayer.getTileWidth();
		tileHeight = tilelayer.getTileHeight();
		//Tenebrae.debug("Tile Dims! "+tilewidth+"x"+tileheight);
		width = tilelayer.getWidth();
		height = tilelayer.getHeight();
		setPosition(0, 0);
		setSize(width, height);

		//final WRect objRenderer = new WRect(new Rectangle(), Color.WHITE, Color.BLACK, 1);
		maprenderer = new OrthogonalExtendedTiledMapRenderer(map, 1/tileWidth, batch){
			@Override
			public void renderObjects(MapLayer layer){
				Entity.DrawableEntity ent;
				for(Entity e : ents.getChildren())
					if(e instanceof Entity.DrawableEntity && (ent = (Entity.DrawableEntity)e) != null &&
						ent.hasMapObject() && ent.isInMapObjects(layer.getObjects()) && ent.isVisible()){
						ent.draw(getBatch(), 1);
					}
			}
		};

		for(TiledMapTileSet tileset : map.getTileSets())
			Log.verbose2("Found tileset! " + tileset.getName());

		bound = new Rectangle();
		setBound(-1, -1, -1, -1);

		//Tenebrar.debug("Map made! "+toString());
	}

	public LuaValue call(){
		LuaValue rtn = script.call();
		return rtn;
	}

	public MapLayers getCollisionLayers(){
		MapLayers rtn = new MapLayers();
		for(MapLayer layer : map.getLayers())
			if(layer instanceof TiledMapTileLayer && layer.getProperties().get("collide", true, Boolean.class))
				rtn.add(layer);
		return rtn;
	}
	public MapObjects getCollisionObjects(int tileX, int tileY){
		Cell cell = getCell(tileX, tileY);
		if(cell == null || cell.getTile().getObjects().getCount() == 0)
			return null;
		MapObjects rtn = new MapObjects();
		for(MapObject obj : cell.getTile().getObjects())
			if(obj instanceof RectangleMapObject){
				RectangleMapObject robj = (RectangleMapObject)obj;
				relateRectMapObjToMap(robj, tileX, tileY);
				rtn.add(robj);
				Log.verbose2("Found collision object!", tileX, tileY, robj.getRectangle());
			}
		return rtn;
	}
	/**
		Relates RectangleMapObjects inside tiles to the map.
	*/
	public void relateRectMapObjToMap(RectangleMapObject obj, float tileX, float tileY){
		MapProperties p = obj.getProperties();
		Rectangle r = obj.getRectangle();
		if(p.get("__x") == null){ // keep the original states
			relateTiledRectToMap(r);
			p.put("__x", r.getX());
			p.put("__y", r.getY());
			p.put("__w", r.getWidth());
			p.put("__h", r.getHeight());
		}else{ // set to original states for re-translation
			r.set(p.get("__x"), p.get("__y"), p.get("__w"), p.get("__h"));
		}
		r.setPosition(tileX + r.getX(), tileY + r.getY());
	}
	public Rectangle relateTiledRectToMap(Rectangle rect){
		return rect.set(rect.getX()/tileWidth, rect.getY()/tileHeight, rect.getWidth()/tileWidth, rect.getHeight()/tileHeight);
	}
	public Array<Trigger> getCollidingObjects(Rectangle player, String prop){
		Log.verbose2("Getting colliding objects!", player);
		Array<Trigger> trigs = getTriggerObjects(prop);
		Iterator<Trigger> iter = trigs.iterator();
		while(iter.hasNext()){
			//Log.verbose2("Test colliding triggers!", trig, trig.getName(), trig.getRectangle());
			if(!iter.next().getRectangle().overlaps(player))
				iter.remove();
		}
		return trigs;
	}
	public Array<Trigger> getTriggerObjects(String prop){
		Array<Trigger> trigs = new Array<Trigger>();
		for(MapLayer layer : map.getLayers())
			for(MapObject obj : layer.getObjects())
				if(obj instanceof RectangleMapObject)
					if(!obj.getProperties().get(prop, "", String.class).isEmpty())
						trigs.add(new Trigger(game, ((RectangleMapObject)obj).getRectangle(), obj.getProperties()));

		for(Entity e : ents.getChildren())
			if(e instanceof Entity.DrawableEntity && e != game.player){
				Entity.DrawableEntity ent = (Entity.DrawableEntity)e;
				Trigger trig = ent.getTrigger(prop);
				if(trig == null)
					continue;
				trig.getProperties().put(Entity.ENTITY, ent);
				trigs.add(trig);
			}
		return trigs;
	}
	public Cell getCell(int x, int y){
		x = clampX(x);
		y = clampY(y);
		MapLayers layers = getCollisionLayers();
		int size = layers.getCount() - 1;
		for(int i = size; i >= 0; i--){
			TiledMapTileLayer layer = ((TiledMapTileLayer)layers.get(i));
			if(layer.getCell(x, y) != null)
				return layer.getCell(x, y);
		}
		return null;
	}
	public Character getTileEnemy(int x, int y){
		//return enemies.getValueAt(0);
		TiledMapTile tile = getCell(x, y).getTile();
		String eneprop = tile.getProperties().get("enemies", "", String.class);
		if(eneprop.isEmpty())
			return null;
		String[] tileenemies = eneprop.split(";");
		ArrayMap<String, Float> enemap = new ArrayMap<String, Float>();
		for(String line : tileenemies){
			line = line.trim();
			String[] parts = line.split(",");
			Log.debug("Finding enemy on tile! " + line + " " + parts[0]);
			enemap.put(parts[0], Float.parseFloat(parts[1]));
		}
		if(enemap.size == 0)
			return null;

		float totchance = 0;
		for(int i = 0; i < enemap.size; i++)
			totchance += enemap.getValueAt(i);
		float random = (float)Math.random() * totchance;
		totchance = 0;
		String enemy = null;
		for(int i = 0; i < enemap.size; i++){
			totchance += enemap.getValueAt(i);
			if(totchance > random){
				enemy = enemap.getKeyAt(i);
				break;
			}
		}
		enemy = enemy.trim();
		//Tenebrae.debug( "Chancing enemy! "+enemy+" "+checkchance+" "+random);
		//Tenebrae.debug( "Enemy list! "+enemies.firstKey()+" "+enemy+" "+enemies.containsKey(enemy));
		return null;
	}
	public MapObject getMapObject(String name){
		for(MapLayer layer : map.getLayers())
			for(MapObject obj : layer.getObjects()){
				if(obj.getName().equals(name))
					return obj;
			}
		return null;
	}
	public boolean hasOnMap(Entity ent){
		return ent.getX() >= 0 && ent.getX(Align.right) <= width && ent.getY() >= 0 && ent.getY(Align.top) <= height;
	}

	public void changeTile(int x, int y, String z, String tileset, int id){
		((TiledMapTileLayer)map.getLayers().get(z)).getCell(x, y).setTile(map.getTileSets().getTileSet(tileset).getTile(id));
	}

	public void addEntity(Entity ent){
		ents.addActor(ent);
	}

	public void setBound(float x, float y, float width, float height){
		if(setBoundToBigDZLater){
			setBoundToBigDZLater = false;
			if(x < 0) x = 0;
			if(y < 0) y = 0;
		}

		bound.set(x, y, width, height);
		game.player.clamp(bound);
	}
	public Rectangle getBound(){
		return bound;
	}
	public void setBoundToBigDZ(){
		if(game.player.bigdzRect == null){
			setBoundToBigDZLater = true;
			return;
		}
		setBoundToBigDZLater = false;
		setBound(0, 0,
			game.player.bigdzRect.width  * game.getCamera().zoom,
			game.player.bigdzRect.height * game.getCamera().zoom);
	}
	
	public float getTileWidth(){
		return tileWidth;
	}
	public float getTileHeight(){
		return tileHeight;
	}
	
	public void setTileOffsetX(float x){
		offsetX = x;
	}
	public float getTileOffsetX(){
		return offsetX;
	}
	public void setTileOffsetY(float y){
		offsetY = y;
	}
	public float getTileOffsetY(){
		return offsetY;
	}
	public void setTileOffset(float x, float y){
		setTileOffsetX(x);
		setTileOffsetY(y);
	}

	@Override
	protected void setStage(Stage stage){
		super.setStage(stage);
		if(stage != null)
			stage.addActor(ents);
		else
			ents.remove();
	}

	public int clampX(int x){
		return MathUtils.clamp(x, 0, width - 1);
	}
	public int clampY(int y){
		return MathUtils.clamp(y, 0, height - 1);
	}

	@Override
	public boolean setZIndex(int i){
		boolean changed = false;
		if(i < getZIndex())
			changed = super.setZIndex(i);
		for(Character c : game.mappack.charas)
			c.setZIndex(i);
		if(i >= getZIndex())
			changed = super.setZIndex(i);
		return changed;
		//mapOutline.setZIndex(i);
	}

	@Override
	public void act(float delta){
		super.act(delta);
		lifetime += delta;
	}
	
	@Override
	public void draw(Batch batch){
		batch.end();
		//Tenebrae.mp.charas.sort();
		OrthographicCamera cam = (OrthographicCamera)getStage().getCamera();
		cam.translate(-offsetX, -offsetY, 0);
		cam.update();
		maprenderer.setView(cam);
		maprenderer.render();
		batch.begin();
		for(Actor a : ents.getChildren())
			if((!(a instanceof Entity.DrawableEntity) || !((Entity.DrawableEntity)a).hasMapObject()) && a.isVisible())
				a.draw(batch, 1);
		cam.translate(offsetX, offsetY, 0);
		cam.update();
	}
	
	@Override
	public void dispose(){
		LuaValue onDestroy = game.mappack.getGlobals().get("onDestroy");
		if(!onDestroy.isnil())
			onDestroy.call();
		super.dispose();
		map.dispose();
		maprenderer.dispose();
		//boundActor.dispose();
		for(Actor a : ents.getChildren())
		 	if(a instanceof Entity && !(a instanceof Character))
				((Entity)a).dispose();
	}

	@Override
	public String toString(){
		return super.toString() + "{" + filepath + ", " + width + "x" + height + "}";
	}
}
