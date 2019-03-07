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
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.*;
import com.badlogic.gdx.maps.tiled.renderers.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.maps.tiled.objects.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.scenes.scene2d.*;
import org.luaj.vm2.*;

public class TileMap extends WActor{
	String filepath,filename;
	TiledMap map;
	WOrthogonalTiledMapRenderer maprenderer;
	float tilewidth, tileheight, tilebasewidth, tilebaseheight;
	int width, height;
	boolean hasNpcObj;

	public TileMap(FileHandle mapFile, LuaFunction trigScriptFile){
		//stage.addActor(debug());

		filepath = mapFile.path();
		filename = mapFile.nameWithoutExtension();

		TmxMapLoader.Parameters params = new TmxMapLoader.Parameters();
		params.generateMipMaps = true;
		map = new TmxMapLoader(new ExternalFileHandleResolver()).load(filepath, params);
		//Tenebrae.debug("Making map! "+file.nameWithoutExtension());

		Rectangle pRect = null;
		for(MapLayer layer : map.getLayers()){
			Log.verbose2("Layer found! " + layer.getName() + " " + layer instanceof TiledMapTileLayer);
			//for(MapObject obj : layer.getObjects())
			//Tenebrae.debug("Object found! "+obj.getName()+" "+obj.getProperties().get("gid"));

			for(MapObject obj : layer.getObjects()){
				if(obj.getName().equals("player")){
					if(obj instanceof RectangleMapObject){
						RectangleMapObject player = (RectangleMapObject)obj;
						pRect = player.getRectangle();
					}else if(obj instanceof TiledMapTileMapObject){
						TiledMapTileMapObject player = (TiledMapTileMapObject)obj;
						pRect = new Rectangle(player.getX(), player.getY(), player.getTextureRegion().getRegionWidth() * player.getScaleX(), player.getTextureRegion().getRegionHeight() * player.getScaleY());
					}
				}
			}
		}
		//Tenebrae.debug(pRect.toString());

		//mapRect must stay same size for this code to work not in orient() (bc of scale in constructor)
		TiledMapTileLayer tilelayer = (TiledMapTileLayer)getCollisionLayers().get(0);
		tilebasewidth = tilelayer.getTileWidth();
		tilebaseheight = tilelayer.getTileHeight();
		float scale = (Math.min(Tenebrae.screenRect.width, Tenebrae.screenRect.height) / Tenebrae.tiles) / Math.max(pRect.width, pRect.height); //mapRect.width or screenRect.width
		tilewidth = tilebasewidth * scale;
		tileheight = tilebaseheight * scale;
		//Tenebrae.debug("Tile Dims! "+tilewidth+"x"+tileheight);
		width = tilelayer.getWidth();
		height = tilelayer.getHeight();
		setPosition(0, 0);
		setSize(width * tilewidth, height * tileheight);

		//final WRect objRenderer = new WRect(new Rectangle(), Color.WHITE, Color.BLACK, 1);
		maprenderer = new WOrthogonalTiledMapRenderer(map, scale){
			@Override
			public void renderObject(MapObject object){
				for(NPC npc : Tenebrae.mp.npcs)
					if(object == npc.mapobj && hasOnMap(npc))
						npc.draw(getBatch(), 1);
				if(object.getName().equals("player"))
					Tenebrae.player.draw(getBatch(), 1);
			}
			/*@Override
			public void renderTileLayer(TiledMapTileLayer layer){
				super.renderTileLayer(layer);
				Array<Character> npcs = this.npcs.get(layer);
				if(npcs != null)
					for(Character npc : npcs)
						npc.draw(getBatch(), 1);
			}*/
		};
		hasNpcObj = getObject("npcs") != null;

		for(int i = 0; i < tilelayer.getWidth(); i++){
			for(int j = 0; j < tilelayer.getHeight(); j++){
				boolean t;
				if(t = tilelayer.getCell(i, j).getTile().getProperties().get("collide", false, Boolean.class)){
					tilelayer.getCell(i, j).setFlipVertically(true);
					tilelayer.getCell(i, j).setFlipHorizontally(true);
				}
				Log.verbose2("Found tile! "+t+" "+i+" "+j);
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
	public MapObjects getCollisionObjects(float x, float y){
		Cell cell = getCell(x, y);
		if(cell == null || cell.getTile().getObjects().getCount() == 0)
			return null;
		MapObjects rtn = new MapObjects();
		for(MapObject obj : cell.getTile().getObjects())
			if(obj instanceof RectangleMapObject){
				Rectangle r = ((RectangleMapObject)obj).getRectangle();
				RectangleMapObject robj = Player.copy((RectangleMapObject)obj);
				robj.getRectangle().set(relateRectMapObjToMap(obj, x, y));
				rtn.add(robj);
				Log.verbose2("Found collision object! " + r);
			}
		return rtn;
	}
	public Rectangle relateRectMapObjToMap(MapObject mapobj, float x, float y){
		if(!(mapobj instanceof RectangleMapObject))
			return null;
		RectangleMapObject obj = (RectangleMapObject)mapobj;
		Rectangle r = obj.getRectangle();
		return new Rectangle((int)x + r.getX() / tilebasewidth, (int)y + r.getY() / tilebaseheight, r.getWidth() / tilebasewidth, r.getHeight() / tilebaseheight);
	}
	public MapObjects getCollidingTriggerObjects(float x, float y, float width, float height){
		Rectangle player = new Rectangle(x, y, width, height);
		//Tenebrae.debug("Getting colliding trigger objects! "+player);
		MapObjects objs = new MapObjects();
		MapObjects trigs = getTriggerObjects();
		for(RectangleMapObject obj : trigs){
			//Tenebrae.debug("Test colliding triggers! "+obj.getName()+" "+obj.getRectangle());
			if(obj.getRectangle().overlaps(player)){
				objs.add(obj);
			}
		}
		return objs;
	}
	public MapObjects getTriggerObjects(){
		MapObjects objs = new MapObjects();
		for(MapLayer layer : map.getLayers()){
			for(MapObject obj : layer.getObjects()){
				if(obj instanceof RectangleMapObject){
					//RectangleMapObject rob = (RectangleMapObject)obj;
					if(!obj.getProperties().get("onTrigger", "", String.class).isEmpty()){
						objs.add(obj);
					}
				}
			}
		}
		return objs;
	}
	public MapObjects getCollidingEnteranceObjects(float x, float y, float width, float height){
		Rectangle player = new Rectangle(x, y, width, height);
		//Tenebrae.debug("Getting colliding enterance objects! "+player);
		MapObjects objs = new MapObjects();
		MapObjects trigs = getEnteranceObjects();
		for(RectangleMapObject obj : trigs){
			//Tenebrae.debug("Test colliding enters! "+obj.getName()+" "+obj.getRectangle());
			if(obj.getRectangle().overlaps(player)){
				objs.add(obj);
			}
		}
		return objs;
	}
	public MapObjects getEnteranceObjects(){
		MapObjects objs = new MapObjects();
		for(MapLayer layer : map.getLayers()){
			for(MapObject obj : layer.getObjects()){
				if(obj instanceof RectangleMapObject){
					//RectangleMapObject rob = (RectangleMapObject)obj;
					if(!obj.getProperties().get("onEnter", "", String.class).isEmpty() || !obj.getProperties().get("onExit", "", String.class).isEmpty()){
						objs.add(obj);
					}
				}
			}
		}
		return objs;
	}
	public Cell getCell(float x, float y){
		MapLayers layers = getCollisionLayers();
		int size = layers.getCount() - 1;
		for(int i = size; i >= 0; i--){
			//Tenebrae.debug("Layers for getting cells! "+i+" "+size);
			TiledMapTileLayer layer = ((TiledMapTileLayer)layers.get(i));
			if(layer.getCell((int)x, (int)y) != null)
				return layer.getCell((int)x, (int)y);
		}
		return null;
	}
	public Enemy getTileEnemy(float x, float y){
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
			Log.debug("Finding enemy on tile! "+line+" "+parts[0]);
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

	@Override
	public boolean setZIndex(int i){
		boolean changed = false;
		if(i < getZIndex())
			changed = super.setZIndex(i);
		for(NPC npc : Tenebrae.mp.npcs)
			npc.setZIndex(i);
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
	
	public static class WOrthogonalTiledMapRenderer extends OrthogonalTiledMapRenderer{
		public ArrayMap<TiledMapTileLayer,Array<Character>> npcs = new ArrayMap<TiledMapTileLayer,Array<Character>>();
		public WOrthogonalTiledMapRenderer(TiledMap map, float scale){
			super(map, scale);
		}
	}
}
