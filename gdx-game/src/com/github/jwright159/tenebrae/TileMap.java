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
	private String filepath;
	private TiledMap map;
	private OrthogonalTiledMapRenderer maprenderer;
	public float tileWidth, tileHeight;
	public int width, height;
	private Group ents;
	public static String EMPTY_PATH = Gdx.files.internal("empty.tmx").path();
	private LuaFunction script;

	public TileMap(FileHandle mapFile, LuaFunction script, Batch batch){
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
		setSize(width * tileWidth, height * tileHeight);

		//final WRect objRenderer = new WRect(new Rectangle(), Color.WHITE, Color.BLACK, 1);
		maprenderer = new OrthogonalExtendedTiledMapRenderer(map, batch){
			@Override
			public void renderObjects(MapLayer layer){
				for(Entity ent : ents.getChildren())
					if(ent.hasMapObject() && ent.isInMapObjects(layer.getObjects()) && ent.isVisible())
						ent.draw(getBatch(), 1);
			}
		};

		for(TiledMapTileSet tileset : map.getTileSets())
			Log.verbose2("Found tileset! " + tileset.getName());

		//Tenebrar.debug("Map made! "+toString());
	}
	
	public LuaValue call(){
		return script.call();
	}

	public MapLayers getCollisionLayers(){
		MapLayers rtn = new MapLayers();
		for(MapLayer layer : map.getLayers())
			if(layer instanceof TiledMapTileLayer && layer.getProperties().get("collide", true, Boolean.class))
				rtn.add(layer);
		return rtn;
	}
	public MapObjects getCollisionObjects(int x, int y){
		Cell cell = getCell(x, y);
		if(cell == null || cell.getTile().getObjects().getCount() == 0)
			return null;
		MapObjects rtn = new MapObjects();
		for(MapObject obj : cell.getTile().getObjects())
			if(obj instanceof RectangleMapObject){
				RectangleMapObject robj = (RectangleMapObject)obj;
				relateRectMapObjToMap(robj, x, y);
				rtn.add(robj);
				Log.verbose2("Found collision object!", robj.getRectangle());
			}
		return rtn;
	}
	/*public static RectangleMapObject copy(RectangleMapObject o){ // Bad flex
	 RectangleMapObject r = new RectangleMapObject();
	 r.setColor(o.getColor());
	 r.setName(o.getName());
	 r.setOpacity(o.getOpacity());
	 r.setVisible(o.isVisible());
	 r.getRectangle().set(o.getRectangle());
	 r.getProperties().putAll(o.getProperties());
	 return r;
	 }*/
	public void relateRectMapObjToMap(RectangleMapObject obj, float x, float y){
		Rectangle r = obj.getRectangle();
		if(!obj.getProperties().containsKey("__x")){
			obj.getProperties().put("__x", r.getX());
			obj.getProperties().put("__y", r.getY());
		}
		if(!obj.getProperties().containsKey("__w")){
			obj.getProperties().put("__w", r.getWidth());
			obj.getProperties().put("__h", r.getHeight());
		}
		r.set(x + obj.getProperties().get("__x", Float.class) / tileWidth, y + obj.getProperties().get("__y", Float.class) / tileHeight, obj.getProperties().get("__w", Float.class) / tileWidth, obj.getProperties().get("__h", Float.class) / tileHeight);
	}
	public void relateRectMapObjToMapPix(RectangleMapObject obj, float x, float y){
		if(!obj.getProperties().containsKey("__x")){
			obj.getProperties().put("__x", obj.getRectangle().getX());
			obj.getProperties().put("__y", obj.getRectangle().getY());
		}
		obj.getRectangle().setPosition(x * tileWidth + obj.getProperties().get("__x", Float.class), y * tileHeight + obj.getProperties().get("__y", Float.class));
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
						trigs.add(new Trigger(((RectangleMapObject)obj).getRectangle(), obj.getProperties()));
		
		for(Entity ent : ents.getChildren())
			if(ent != Tenebrae.player){
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

	public void addEntity(Entity ent, MapObject obj){
		ents.addActor(ent);
	}
	public void addEntity(Entity ent){
		addEntity(ent, null);
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
		for(Character c : Tenebrae.mp.charas)
			c.setZIndex(i);
		if(i >= getZIndex())
			changed = super.setZIndex(i);
		return changed;
		//mapOutline.setZIndex(i);
	}

	@Override
	public void draw(Batch batch, float parentAlpha){
		batch.end();
		//Tenebrae.mp.charas.sort();
		maprenderer.setView((OrthographicCamera)getStage().getCamera());
		maprenderer.render();
		batch.begin();
		for(Entity ent : ents.getChildren())
			if(!ent.hasMapObject() && ent.isVisible())
				ent.draw(batch, parentAlpha);
	}

	@Override
	public void dispose(){
		LuaValue onDestroy = Tenebrae.mp.getGlobals().get("onDestroy");
		if(!onDestroy.isnil())
			onDestroy.call();
		super.dispose();
		map.dispose();
		maprenderer.dispose();
		for(Entity ent : ents.getChildren())
		 	if(!(ent instanceof Character))
				ent.dispose();
	}

	@Override
	public String toString(){
		return super.toString() + "{" + filepath + ", " + width + "x" + height + "}";
	}
}