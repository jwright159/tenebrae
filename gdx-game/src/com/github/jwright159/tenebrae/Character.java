package com.github.jwright159.tenebrae;

import com.github.jwright159.gdx.*;
import com.github.jwright159.gdx.graphics.*;
import static com.github.jwright159.tenebrae.EntityBox.HealthBar.f;
import com.github.jwright159.tenebrae.Action.*;
import com.badlogic.gdx.maps.*;
import com.badlogic.gdx.maps.tiled.objects.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.maps.tiled.*;
import com.badlogic.gdx.graphics.*;
import java.util.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.maps.objects.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.*;

abstract public class Character extends Entity.DrawableEntity implements ScriptGlob{
	public String name, filename;
	public float targetX=-1,targetY=-1;//in tiles
	public float exp, g, hp, mp;
	public ArrayMap<String,ArrayMap<Stats,Float>> stats;
	protected Array<MenuItem.GameItem> items;
	public ArrayMap<String,MenuItem.GameItem> equippedItems;
	public EntityBox box;
	public EntityBox.StatBox smolStatBox;
	private ArrayMap<String,Skin> skinList;
	private Array<Action> actions;
	protected Action currentAction;
	public float delay;//Using Delay Action keeps me from using only RunnableActions //But now it doesn't matter anyway as I'm not using Actions //Well i was just using Runnables with a boolean property but now i need more properties so i renamed it to Action
	protected LayeredTextureRegion regions;
	protected LayeredTile tile;
	private Globals globals;
	
	public static final String baseStats = "__base", atkMod = "__attackMod", itemMod = "__itemMod";
	public static final enum Stats{str,intl,def,agl,maxhp,maxmp}

	public Character(Tenebrae game, String filename){
		super(game, 0, 0, 1, 1, null);
		regions = new LayeredTextureRegion();
		setRegions(regions);
		tile = new LayeredTile();
		stats = new ArrayMap<String,ArrayMap<Stats,Float>>();
		items = new Array<MenuItem.GameItem>();
		equippedItems = new ArrayMap<String,MenuItem.GameItem>();
		skinList = new ArrayMap<String,Skin>();
		globals = game.new StdEntGlobals();
		vars = globals;
		globals.load(new EntityLib());
		globals.load(new DrawableEntityLib());
		globals.load(new CharacterLib());
		actions = new Array<Action>();
		this.filename = filename;
		
		box = new EntityBox(this, false, game.getSkin());
		smolStatBox = new EntityBox.FadingStatBox(box.healthBar, box.manaBar, game.getSkin());
		smolStatBox.setSize(Tenebrae.MARGIN * 10.0f, Tenebrae.MARGIN * 2.0f);
		game.getUiStage().addActor(smolStatBox);
		
		setStats(baseStats, 0, 0, 0, 0, 1, 1);
		exp = 0;
		g = 0;
		setDebug(Tenebrae.TABLEDEBUG);
	}
	public void changeMap(TileMap map){
		Rectangle rect = new Rectangle();
		MapObject mapobj = map.getMapObject(filename);
		setMapObject(mapobj);
		
		if(mapobj == null){
			rect = new Rectangle(-1, -1, 1, 1);
		}else{
			if(mapobj instanceof RectangleMapObject){
				RectangleMapObject obj = (RectangleMapObject)mapobj;
				rect.set(obj.getRectangle());
				//Log.debug("Entity is a rectangle! "+pRect);
			}else if(mapobj instanceof TiledMapTileMapObject){
				TiledMapTileMapObject obj = (TiledMapTileMapObject)mapobj;
				rect.set(obj.getX(), obj.getY(), obj.getTextureRegion().getRegionWidth() * obj.getScaleX(), obj.getTextureRegion().getRegionHeight() * obj.getScaleY());
				//Log.debug("Entity is a tile! "+pRect);
			}
			map.relateTiledRectToMap(rect);
			map.addEntity(this);
		}

		Log.debug("Changing map!", this, rect, currentAction);
		
		setBounds(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
		currentAction = null;
		smolStatBox.setVisible(false);
	}

	public void move(float newX, float newY, float speed, boolean relative, boolean collide){
		addAction(new Action.MoveAction(this, newX, newY, speed, relative, collide, false));
		triggerAction();
	}
	public boolean hasTarget(){
		return targetX != -1 || targetY != -1;
	}

	public float calcDamage(Character enemy, boolean magic){
		float rtn = ((magic ? intl() : str()) - enemy.def() * 2 / 3 + (float)Math.random() * 2) * 2;//ut player
		//float rtn = ((magic ? intl() : str()) + ((enemy.hp - 10f) / 10f) - (enemy.def() / 5f)) * (1f - 0.1f * (float)Math.random());//ut enemy(without random part)
		Log.verbose("CalcDamage", rtn, str(), enemy.hp, enemy.def());
		if(rtn > 1)
			return (int)rtn;
		return 1;
	}
	public float calcHeal(Character enemy, boolean magic){
		float rtn = (-(magic ? intl() : str()) - enemy.str() * 2 / 3 + (float)Math.random() * 2) * 2;//ut player
		//float rtn = (-(magic ? intl() : str()) + ((enemy.hp - 10f) / 10f) - (enemy.str() / 5f)) * (1f - 0.1f * (float)Math.random());//ut enemy(without random part)
		Log.verbose("CalcHeal", rtn, str(), enemy.hp, enemy.str());
		if(rtn > 1)
			return (int)rtn;
		return 1;
	}
	public float calcDodgeChance(Character enemy){
		if(enemy.agl() <= 0)
			return 1;
		return agl() / enemy.agl();
	}
	public boolean hitSuccess(Character enemy){
		return true;//need to find some way of making this work with items better
		//return Math.random() < calcDodgeChance(enemy);
	}

	public void setStat(String type, Stats stat, float value){
		if(!stats.containsKey(type))
			stats.put(type, new ArrayMap<Stats,Float>());
		stats.get(type).put(stat, value);
		updateBoxHP();
	}
	public void setStats(String type, float str, float intl, float def, float agl, float maxhp, float maxmp){
		Log.debug("Updating stats of type " + type + " to " + toString() + "! " + str + " " + intl + " " + def + " " + agl + " " + maxhp + " " + maxmp);
		setStat(type, Stats.str, str);
		setStat(type, Stats.intl, intl);
		setStat(type, Stats.def, def);
		setStat(type, Stats.agl, agl);
		setStat(type, Stats.maxhp, maxhp);
		setStat(type, Stats.maxmp, maxmp);
		Log.verbose("Updated! " + stats);
	}
	public void removeStats(String type){
		//Log.debug("Removing stats of type " + type + " from " + toString() + "!");
		ArrayMap<Stats,Float> stat = stats.removeKey(type);
		if(hp > maxhp())
			damage(stat.get(Stats.maxhp), true);
		if(mp > maxmp())
			tire(stat.get(Stats.maxmp), true);
		updateBoxHP();
	}
	public void removeTempStats(){
		for(int i = 0; i < stats.size; i++)
			if(stats.getKeyAt(i).startsWith("_temp"))
				removeStats(stats.getKeyAt(i--));
	}
	public void unequip(String type){
		if(equippedItems.containsKey(type))
			equippedItems.get(type).run("onUnequip");
	}

	public void addItem(MenuItem.GameItem item){
		items.add(item);
	}
	public void removeItem(MenuItem.GameItem item){
		items.removeValue(item, true);
	}

	public void setSkin(String tileset){
		setSkin(0, tileset);
	}
	public void setSkin(int z, String tilesetName){
		Log.debug("Setting skinset!", z, tilesetName);

		removeSkin(z);

		TiledMapTileSet tileset = game.mappack.loadTileset(tilesetName);
		Skin newSkin = new Skin(z);
		for(TiledMapTile tile : tileset){
			Log.verbose("Objs on new skin!", tilesetName, tile.getObjects().getCount());
			MapProperties prop = tile.getProperties();
			String dirstr = prop.get("direction", String.class);
			if(dirstr != null){
				String[] dirs = dirstr.split("\\s+");
				for(String dir : dirs)
					newSkin.putSkin(prop.get("speed", 0f, Float.class), dir, tile);
			}
		}

		skinList.put(Utils.filename(tilesetName), newSkin);
		if(!tile.isEmpty()){
			MapProperties p = null;
			for(TiledMapTile t : tile)
				if(t != null){
					p = t.getProperties();
					break;
				}
			if(p == null)
				updateSkins();
			else
				updateSkins(p.get("speed", 0f, Float.class), p.get("direction", "down", String.class).split("\\s+")[0]);
		}else
			updateSkins();
	}
	public void setSkin(String skin, float speed, String type){
		Log.verbose2("Setting skin!", skin, speed, type, skinList.get(skin));
		int z = skinList.get(skin).z;
		TiledMapTile t = skinList.get(skin).getSkin(speed, type);
		tile.set(z, t);
		regions.set(z, t.getTextureRegion());
	}
	public void updateSkins(float dx, float dy){
		//if(dx == 0 && dy == 0)//causes running to be played if sudden stop
		//	return;
		String dir = "down";
		if(Math.hypot(dx, dy) > 0.03){
			dir = Math.abs(dx) > Math.abs(dy) ? dx > 0 ? "right" : "left" : dy > 0 ? "up" : "down";
		}else{
			out:
			for(Skin skin : skinList.values())
				for(ArrayMap<String,TiledMapTile> map : skin.tileList.values())
					if(map.containsValue(tile.get(0), true)){
						dir = map.getKey(tile.get(0), true);
						//Log.debug("Found active skin! "+skinList.getKey(skins,true)+" "+dir);
						break out;
					}
		}
		updateSkins((float)Math.hypot(dx, dy), dir);
	}
	public void updateSkins(float speed, String directon){
		for(int i = 0; i < skinList.size; i++)
			setSkin(skinList.getKeyAt(i), speed, directon);
	}
	public void updateSkins(){
		updateSkins(0, "down");
	}
	public String getSkin(int z){
		for(Skin skin : skinList.values())
			if(skin.z == z)
				return skinList.getKey(skin, true);
		return null;
	}
	public String removeSkin(int z){
		//Log.debug("Removing skin! " + z);
		String rem = null;
		for(Skin skin : skinList.values())
			if(skin.z == z)
				rem = skinList.getKey(skin, true);
		if(rem != null){
			tile.removeIndex(z);
			skinList.removeKey(rem);
		}
		return rem;
	}

	@Override
	public Trigger getTrigger(String prop){
		for(TiledMapTile t : tile)
			for(MapObject obj : t.getObjects())
				if(obj instanceof RectangleMapObject && !obj.getProperties().get(prop, "", String.class).isEmpty()){
					game.map.relateRectMapObjToMap((RectangleMapObject)obj, getX(), getY());
					//Log.debug("Got trigger", prop, "from", this, obj);
					return new Trigger(game, ((RectangleMapObject)obj).getRectangle(), obj.getProperties());
				}
		return null;
	}

	public void triggerAction(){
		Log.verbose2("Wanting an action from", this, ", Was", currentAction);
		boolean stop = currentAction == null || currentAction.stop(false);
		if(stop){
			delay = 0;
			currentAction = null;
		}
		if(hasAction())
			Log.debug("Triggerboi", this, actions, stop);
		if(!game.doneLoading || delay != 0 || !stop){
			//Log.debug("..But nobody came.");
			return;
		}else if(!hasAction() && stop){
			if(!doDefaultAction())
				return;
		}
		//Log.debug("Iterate!");
		currentAction = removeAction();
		if(currentAction != null){
			currentAction.run();
			Log.verbose2("Current action!", currentAction, delay, currentAction == null ? null : currentAction.manualOverride);
			//if(delay != 0 || (currentAction != null && currentAction.manualOverride))
			//	map.cover();
			if(currentAction == null)//on loading maps, currentAction gets nulled by loading of new map's scripts // interesting, because they  w e r e n ' t  and I just fixed it
				return;
			else
				triggerAction();
		}else{
			//Log.debug("Trigger is null!");
			triggerAction();
		}
	}
	public boolean doDefaultAction(){
		return false;
	}

	@Override
	public Globals getGlobals(){
		return globals;
	}

	public float str(){
		float rtn = 0;
		for(ArrayMap<Stats,Float> value : stats.values())
			rtn += value.get(Stats.str);
		return rtn;
	}
	public float intl(){
		float rtn = 0;
		for(ArrayMap<Stats,Float> value : stats.values())
			rtn += value.get(Stats.intl);
		return rtn;
	}
	public float def(){
		float rtn = 0;
		for(ArrayMap<Stats,Float> value : stats.values())
			rtn += value.get(Stats.def);
		return rtn;
	}
	public float agl(){
		float rtn = 0;
		for(ArrayMap<Stats,Float> value : stats.values())
			rtn += value.get(Stats.agl);
		return rtn;
	}
	public float maxhp(){
		float rtn = 0;
		for(ArrayMap<Stats,Float> value : stats.values())
			if(value.get(Stats.maxhp) != null)
				rtn += value.get(Stats.maxhp);
		return rtn;
	}
	public float maxmp(){
		float rtn = 0;
		for(ArrayMap<Stats,Float> value : stats.values())
			if(value.get(Stats.maxmp) != null)
				rtn += value.get(Stats.maxmp);
		return rtn;
	}
	public void attack(Character enemy, boolean magic){
		if(enemy == null){
			game.player.addDialog(name + " attacked!");
			game.player.addDialog("...But nobody came.");
			return;
		}
		if(str() >= 0)
			enemy.damage(calcDamage(enemy, magic), false);
		else
			enemy.heal(calcHeal(enemy, magic), false);
		//for(MenuItem.GameItem item : enemy.equippedItems.values())
		//	item.run("onEquippedHit");
		removeTempStats();
	}
	public void damage(float damage, boolean override){
		hp -= damage;
		if(!override && hp < 0)
			hp = 0;
		finishAffect(!override);
	}
	public void heal(float heal, boolean override){
		hp += heal;
		if(!override && hp > maxhp())
			hp = maxhp();
		finishAffect(!override);
	}
	public void tire(float damage, boolean override){
		mp -= damage;
		if(!override && mp < 0)
			mp = 0;
		finishAffect(!override);
	}
	public void invigor(float heal, boolean override){
		mp += heal;
		if(!override && mp > maxmp())
			mp = maxmp();
		finishAffect(!override);
	}
	public void finishAffect(boolean checkDeath){
		updateBoxHP();
		if(checkDeath && isDead())
			die();
	}
	public boolean isDead(){
		return hp <= 0;
	}
	/**
		Returns whether death was handled.
	*/
	public boolean die(){
		LuaValue func = getGlobals().get("onDeath");
		if(!func.isnil())
			return func.call(getGlobals()).toboolean();
		else
			return false;
	}

	public void addAction(Action add){
		Log.verbose2("Adding action", add);
		actions.add(add);
	}
	public boolean hasAction(){
		return !actions.isEmpty();
	}
	public boolean hasAnyAction(){
		return currentAction != null || hasAction();
	}
	public Action getAction(){
		return actions.get(0);
	}
	public Array<Action> getActionArray(){
		return actions;
	}
	public Action removeAction(){
		return actions.removeIndex(0);
	}
	public void addDelay(float delay, LuaFunction funcToRun){
		addAction(new DelayAction(this, delay, false));
		if(funcToRun != null)
			addAction(new FunctionAction(funcToRun));
	}
	
	private static Vector2 coordBuffer = new Vector2(), sizeBuffer = new Vector2();
	public void moveSmolStatBox(){
		localToScreenCoordinates(coordBuffer.set(0, 0));
		localToScreenCoordinates(sizeBuffer.set(getWidth(), getHeight()));
		//Log.debug(coordBuffer, sizeBuffer);
		sizeBuffer.sub(coordBuffer).scl(1, -1); // sizeBuffer gives top-right point
		coordBuffer.y = game.getStage().getViewport().getScreenHeight() - coordBuffer.y - game.getStage().getViewport().getBottomGutterHeight();// + game.getStage().getViewport().getLeftGutterWidth(); // screen is y-down // also gutters are messing stuff up???
		smolStatBox.setPosition(coordBuffer.x + sizeBuffer.x / 2 - smolStatBox.getWidth() / 2, coordBuffer.y + sizeBuffer.y + Tenebrae.MARGIN);
	}
	public void updateBoxHP(){
		Log.debug("Updating HP", hp, maxhp(), mp, maxmp(), this);
		if(box != null)
			box.updateHP();
		if(smolStatBox != null)
			smolStatBox.updateHP();
	}

	@Override
	public void act(float delta){
		super.act(delta);

		if(delay > 0){
			//Log.debug("Delay "+delay+", delta "+delta);
			delay -= delta;
			if(delay < 0)
				delay = 0;
		}
		triggerAction();

		doMovement();
	}
	public void doMovement(){
		if(hasTarget()){
			float px = getX(), py = getY();
			setPosition(targetX, targetY);
			targetX = targetY = -1;
			updateSkins(getX() - px, getY() - py);
		}else{
			updateSkins(0, 0);
		}
	}

	@Override
	public void draw(Batch batch, float parentAlpha){
		moveSmolStatBox();
		super.draw(batch, parentAlpha);
	}

	@Override
	public String toString(){
		return super.toString() + "§" + filename + "," + tile;
	}

	public void endSelf(){
		dispose();
		smolStatBox.remove();
	}

	public class CharacterLib extends TwoArgFunction{
		@Override
		public LuaValue call(LuaValue modname, LuaValue env){
			LuaTable library = tableOf();

			library.set("setSkin", new TwoArgFunction(){
					@Override
					public LuaValue call(LuaValue tileset, LuaValue z){
						if(!z.isnil())
							setSkin(z.checkint(), tileset.checkjstring());
						else
							setSkin(tileset.checkjstring());
						return NONE;
					}
				});
			library.set("removeSkin", new OneArgFunction(){
					@Override
					public LuaValue call(LuaValue z){
						removeSkin(z.checkint());
						return NONE;
					}
				});
			library.set("affect", new VarArgFunction(){ // hp, mp, override
					@Override
					public Varargs invoke(Varargs args){
						float hp = (float)args.checkdouble(1), mp = (float)args.checkdouble(2);
						boolean override = args.optboolean(3, false);
						if(hp < 0)
							damage(-hp, override);
						else if(hp > 0)
							heal(hp, override);
						if(mp < 0)
							tire(-mp, override);
						else if(mp > 0)
							invigor(mp, override);
						return NONE;
					}
				});
			library.set("setStat", new ThreeArgFunction(){
					@Override
					public LuaValue call(LuaValue statLevel, LuaValue stat, LuaValue value){
						String lv;
						if(statLevel.isnil())
							lv = baseStats;
						else
							lv = statLevel.checkjstring();
						setStat(lv, Character.Stats.valueOf(stat.checkjstring()), (float)value.checkdouble());
						return NONE;
					}
				});
			library.set("setStats", new VarArgFunction(){ // statLevel, atk, intl, def, agl, maxhp, maxmp, exp, g
					@Override
					public Varargs invoke(Varargs args){
						String lv;
						if(args.isnil(1))
							lv = baseStats;
						else
							lv = args.checkjstring(1);
						setStats(lv, (float)args.checkdouble(2), (float)args.checkdouble(3), (float)args.checkdouble(4), (float)args.checkdouble(5), (float)args.checkdouble(6), (float)args.checkdouble(7));
						if(!args.isnil(8))
							exp = (float)args.checkdouble(8);
						if(!args.isnil(9))
							g = (float)args.checkdouble(9);
						return NONE;
					}
				});
			library.set("addItem", new OneArgFunction(){
					@Override
					public LuaValue call(LuaValue file){
						MenuItem.GameItem item;
						addItem(item = game.mappack.loadItem(file.checkjstring(), Character.this));
						Log.debug(item);
						return item.getGlobals();
					}
				});
			library.set("hasItem", new OneArgFunction(){
					@Override
					public LuaValue call(LuaValue itemName){
						int amt = 0;
						for(MenuItem.GameItem item : items)
							if(item.id.equals(itemName.checkjstring()))
								amt++;
						return valueOf(amt);
					}
				});
			library.set("getItem", new OneArgFunction(){ // Should only be used for unique items
					@Override
					public LuaValue call(LuaValue itemName){
						for(MenuItem.GameItem item : items)
							if(item.id.equals(itemName.checkjstring()))
								return item.getGlobals();
						return NIL;
					}
				});
			library.set("moveBy", new ThreeArgFunction(){
					@Override
					public LuaValue call(LuaValue x, LuaValue y, LuaValue speed){
						//float dist = (float)Math.hypot(scope.getVal("x"), scope.getVal("y"));
						move((float)x.checkdouble(), (float)y.checkdouble(), (float)speed.optdouble(0), true, false);
						return NONE;
					}
				});
			library.set("moveTo", new ThreeArgFunction(){
					@Override
					public LuaValue call(LuaValue x, LuaValue y, LuaValue speed){
						//float dist = (float)Math.hypot(x - (float)scope.getVal("x"), y - (float)scope.getVal("y"));
						move((float)x.checkdouble(), (float)y.checkdouble(), (float)speed.optdouble(0), false, false);
						return NONE;
					}
				});
			library.set("delay", new TwoArgFunction(){
					@Override
					public LuaValue call(LuaValue delayTime, LuaValue function){
						addDelay((float)delayTime.checkdouble(), function.checkfunction());
						return NONE;
					}
				});
			library.setmetatable(tableOf());
			library.getmetatable().set(INDEX, new TwoArgFunction(){
					@Override
					public LuaValue call(LuaValue self, LuaValue key){
						switch(key.checkjstring()){
							case "name":
								return valueOf(Character.this.name);
							case "str":
								return valueOf(str());
							case "intl":
								return valueOf(intl());
							case "def":
								return valueOf(def());
							case "agl":
								return valueOf(agl());
							case "hp":
								return valueOf(hp);
							case "mp":
								return valueOf(mp);
							case "maxhp":
								return valueOf(maxhp());
							case "maxmp":
								return valueOf(maxmp());
							case "gold":
								return valueOf(g);
							case "exp":
								return valueOf(exp);
							case "__this":
								return CoerceJavaToLua.coerce(Character.this);
							default:
								return NIL;
						}
					}
				});
			library.getmetatable().set(NEWINDEX, new ThreeArgFunction(){
					@Override
					public LuaValue call(LuaValue self, LuaValue key, LuaValue value){
						switch(key.checkjstring()){
							case "name":
								Character.this.name = value.checkjstring();
								break;
							case "str":
								self.error("str is read-only");
								break;
							case "intl":
								self.error("intl is read-only");
								break;
							case "def":
								self.error("def is read-only");
								break;
							case "agl":
								self.error("agl is read-only");
								break;
							case "hp":
								self.error("hp is read-only");
								break;
							case "mp":
								self.error("mp is read-only");
								break;
							case "maxhp":
								self.error("maxhp is read-only");
								break;
							case "maxmp":
								self.error("maxmp is read-only");
								break;
							case "__this":
								self.error("__this is read-only");
								break;
							default:
								return TRUE;
						}
						return NONE;
					}
				});

			ScriptGlob.S.setLibToEnv(library, env);
			env.set("setTexture", NIL);
			return env;
		}
	}

	public static class Skin implements Iterable<TiledMapTile>{
		private ArrayMap<Float,ArrayMap<String, TiledMapTile>> tileList;
		private int z;

		public Skin(int z){
			tileList = new ArrayMap<Float,ArrayMap<String, TiledMapTile>>();
			this.z = z;
		}
		public void putSkin(float speed, String type, TiledMapTile tile){
			Log.verbose2("Putting skin!", speed, type, tile.getId());
			ArrayMap<String,TiledMapTile> map;
			if(tileList.get(speed) != null)
				map = tileList.get(speed);
			else
				map = new ArrayMap<String,TiledMapTile>();
			map.put(type, tile);
			tileList.put(speed, map);
			sortSkinList();
		}
		public TiledMapTile getSkin(float speed, String type){
			float lowspeed = 0;
			for(float newspeed : tileList.keys())
				if(newspeed > speed){
					//Log.debug("Speed stop! "+lowspeed+" "+speed+" "+newspeed);
					break;
				}else{
					//Log.debug("Speed go! "+lowspeed+" "+speed+" "+newspeed);
					lowspeed = newspeed;
				}
			//Log.debug("Speed end! "+lowspeed+" "+speed);
			Log.verbose2("Getting skin", lowspeed, type, tileList.get(lowspeed));
			return tileList.get(lowspeed).get(type);
		}
		public void sortSkinList(){
			ArrayMap<Float,ArrayMap<String, TiledMapTile>> sorted = new ArrayMap<Float,ArrayMap<String, TiledMapTile>>();
			while(tileList.size > 0){
				int lowest = 0;
				for(int i = 0; i < tileList.size; i++)
					if(tileList.getKeyAt(i) < tileList.getKeyAt(lowest))
						lowest = i;
				sorted.put(tileList.getKeyAt(lowest), tileList.getValueAt(lowest));
				tileList.removeIndex(lowest);
			}
			tileList = sorted;
		}

		@Override
		public Iterator<TiledMapTile> iterator(){
			Array<TiledMapTile> tiles = new Array<TiledMapTile>();
			for(ArrayMap<String,TiledMapTile> map : tileList.values())
				for(TiledMapTile tile : map.values())
					if(!tiles.contains(tile, true))
						tiles.add(tile);
			return tiles.iterator();
		}
	}
}
