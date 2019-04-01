package wrightway.gdx.tnb;

import wrightway.gdx.*;
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

public class TileMap extends WActor{
	private String filepath,filename;
	private TiledMap map;
	private OrthogonalTiledMapRenderer maprenderer;
	public float tileWidth, tileHeight;
	public int width, height;

	public TileMap(FileHandle mapFile, LuaFunction trigScriptFile, Batch batch){
		//stage.addActor(debug());

		filepath = mapFile.path();
		filename = mapFile.nameWithoutExtension();

		TmxMapLoader.Parameters params = new TmxMapLoader.Parameters();
		params.generateMipMaps = true;
		map = new TmxMapLoader(new ExternalFileHandleResolver()).load(filepath, params);
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
				Tenebrae.mp.charas.sort();
				for(Character c : Tenebrae.mp.charas)
					if(c.mapobj != null)
						c.draw(getBatch(), 1);
			}
		};

		for(int i = 0; i < tilelayer.getWidth(); i++){
			for(int j = 0; j < tilelayer.getHeight(); j++){
				boolean t;
				if(t = tilelayer.getCell(i, j).getTile().getProperties().get("collide", false, Boolean.class)){
					tilelayer.getCell(i, j).setFlipVertically(true);
					tilelayer.getCell(i, j).setFlipHorizontally(true);
				}
				//Log.verbose2("Found tile! "+t+" "+i+" "+j);
			}
		}

		for(TiledMapTileSet tileset : map.getTileSets())
			Log.verbose2("Found tileset! " + tileset.getName());

		trigScriptFile.call();

		//Tenebrar.debug("Map made! "+toString());
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
	public MapObjects getCollidingTriggerObjects(Rectangle player){
		Log.verbose2("Getting colliding trigger objects!", player);
		MapObjects objs = new MapObjects();
		MapObjects trigs = getTriggerObjects();
		for(RectangleMapObject obj : trigs){
			Log.verbose2("Test colliding triggers!", obj, obj.getName(), obj.getRectangle());
			if(obj.getRectangle().overlaps(player))
				objs.add(obj);
		}
		return objs;
	}
	public MapObjects getTriggerObjects(){
		MapObjects objs = new MapObjects();
		for(MapLayer layer : map.getLayers())
			for(MapObject obj : layer.getObjects())
				if(obj instanceof RectangleMapObject)
					if(!obj.getProperties().get("onTrigger", "", String.class).isEmpty())
						objs.add(obj);
		return objs;
	}
	public MapObjects getCollidingEnteranceObjects(Rectangle player){
		//Tenebrae.debug("Getting colliding enterance objects! "+player);
		MapObjects objs = new MapObjects();
		MapObjects trigs = getEnteranceObjects();
		for(RectangleMapObject obj : trigs){
			//Tenebrae.debug("Test colliding enters! "+obj.getName()+" "+obj.getRectangle());
			if(obj.getRectangle().overlaps(player))
				objs.add(obj);
		}
		return objs;
	}
	public MapObjects getEnteranceObjects(){
		MapObjects objs = new MapObjects();
		for(MapLayer layer : map.getLayers())
			for(MapObject obj : layer.getObjects())
				if(obj instanceof RectangleMapObject)
					if(!obj.getProperties().get("onEnter", "", String.class).isEmpty() || !obj.getProperties().get("onExit", "", String.class).isEmpty())
						objs.add(obj);
		return objs;
	}
	public Cell getCell(int x, int y){
		x = clampX(x);
		y = clampY(y);
		MapLayers layers = getCollisionLayers();
		int size = layers.getCount() - 1;
		for(int i = size; i >= 0; i--){
			//Tenebrae.debug("Layers for getting cells! "+i+" "+size);
			TiledMapTileLayer layer = ((TiledMapTileLayer)layers.get(i));
			if(layer.getCell(x, y) != null)
				return layer.getCell(x, y);
		}
		return null;
	}
	public Enemy getTileEnemy(int x, int y){
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
		return Tenebrae.mp.loadEnemy(enemy);
	}
	public MapObject getObject(String name){
		for(MapLayer layer : map.getLayers())
			for(MapObject obj : layer.getObjects()){
				Log.debug("Looking for mapobj", name, "and found", obj.getName(), "on", layer.getName());
				if(obj.getName().equals(name))
					return obj;
			}
		return null;
	}
	public boolean hasOnMap(Character chara){
		return chara.x >= 0 && chara.x <= width - chara.width && chara.y >= 0 && chara.y <= height - chara.height;
	}

	public void changeTile(int x, int y, String z, String tileset, int id){
		((TiledMapTileLayer)map.getLayers().get(z)).getCell(x, y).setTile(map.getTileSets().getTileSet(tileset).getTile(id));
	}

	public int clampX(int x){
		return MathUtils.clamp(x, 0, width-1);
	}
	public int clampY(int y){
		return MathUtils.clamp(y, 0, height-1);
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
		maprenderer.setView((OrthographicCamera)getStage().getCamera());
		maprenderer.render();
		batch.begin();
	}

	@Override
	public void dispose(){
		super.dispose();
		map.dispose();
		maprenderer.dispose();
	}

	@Override
	public String toString(){
		return super.toString() + "§" + filename + "{" + filepath + ", " + width + "x" + height + "}";
	}
}
