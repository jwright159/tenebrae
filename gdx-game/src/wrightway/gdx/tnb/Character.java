package wrightway.gdx.tnb;

import wrightway.gdx.*;
import wrightway.gdx.JVSValue.Scope;
import wrightway.gdx.JVSValue.WObject;
import wrightway.gdx.JVSValue.Function;
import com.badlogic.gdx.maps.*;
import com.badlogic.gdx.maps.tiled.objects.*;
import static wrightway.gdx.tnb.EntityBox.HealthBar.f;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.maps.tiled.*;
import com.badlogic.gdx.graphics.*;
import java.util.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.maps.objects.*;
import wrightway.gdx.tnb.Action.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;

abstract public class Character extends WActor.WTexture implements JVSValue{
	String name, filename;
	float x,y,width,height,px,py,targetX=-1,targetY=-1;//in tiles
	float exp, g, hp, mp;
	ArrayMap<String,ArrayMap<Stats,Float>> stats;
	Array<MenuItem.GameItem> items;
	ArrayMap<String,MenuItem.GameItem> equippedItems;
	EntityBox box;
	Character enemy;
	ArrayMap<String,Skin> skinList;
	Scope vars;
	Array<Action> actions;
	Action currentAction;
	float delay;//Using Delay Action keeps me from using only RunnableActions //But now it doesn't matter anyway as I'm not using Actions //Well i was just using Runnables with a boolean property but now i need more properties so i renamed it to Action
	MapObject mapobj;
	LayeredTile tile;

	public static final String baseStats = "_base", atkMod = "_attackMod", itemMod = "_itemMod";
	enum Stats{str,intl,def,agl,maxhp,maxmp}

	public Character(String filename){
		tile = new LayeredTile(){
			@Override
			public void set(int index, TiledMapTile value){
				super.set(index, value);
				getRegions().set(index, value.getTextureRegion());
			}
		};
		stats = new ArrayMap<String,ArrayMap<Stats,Float>>();
		items = new Array<MenuItem.GameItem>();
		equippedItems = new ArrayMap<String,MenuItem.GameItem>();
		skinList = new ArrayMap<String,Skin>();
		vars = new Scope(null, "char"+filename);
		actions = new Array<Action>();
		this.filename = filename;
		box = new EntityBox(this, Tenebrae.t.getSkin());
		setStats(baseStats, 0, 0, 0, 0, 1, 1);
		exp = 0;
		g = 0;
		vars.put("setSkin", new Function(new String[]{"tileset", "z"}, new JVSValue[]{new JVSValue(){
						@Override
						public Object get(Scope scope){
							int z;
							if((z = scope.getVal("z", Integer.class, -1)) != -1)
								setSkin(z, scope.getVal("tileset", String.class, null));
							else
								setSkin(scope.getVal("tileset", String.class, null));
							return null;
						}
					}}));
		vars.put("removeSkin", new Function(new String[]{"z"}, new JVSValue[]{new JVSValue(){
						@Override
						public Object get(Scope scope){
							removeSkin(scope.getVal("z", Integer.class, null));
							return null;
						}
					}}));
		vars.put("addItem", new Function(new String[]{"file"}, new JVSValue[]{new JVSValue(){
						@Override
						public Object get(Scope scope){
							MenuItem.GameItem item;
							addItem(item = Tenebrae.mp.loadItem(scope.getVal("file", String.class, null), Character.this));
							return item;
						}
					}}));
		vars.put("affect", new Function(new String[]{"hp", "mp", "silent", "override"}, new JVSValue[]{new JVSValue(){
						@Override
						public Object get(Scope scope){
							float hp = scope.getVal("hp", Float.class, null), mp = scope.getVal("mp", Float.class, null);
							boolean silent = scope.getVal("silent", java.lang.Boolean.class, false);
							boolean override = scope.getVal("override", java.lang.Boolean.class, false);
							if(hp < 0)
								damage(-hp, override,  silent);
							else if(hp > 0)
								heal(hp, override, silent);
							if(mp < 0)
								tire(-mp, override, silent);
							else if(mp > 0)
								invigor(mp, override, silent);
							return null;
						}
					}}));
		vars.put("setStat", new Function(new String[]{"statLevel", "stat", "value"}, new JVSValue[]{new JVSValue(){
						@Override
						public Object get(Scope scope){
							String lv;
							if((lv = scope.getVal("statLevel", String.class, "")).isEmpty())
								lv = baseStats;
							setStat(lv, Character.Stats.valueOf(scope.getVal("stat", String.class, null)), scope.getVal("value", Float.class, null));
							return null;
						}
					}}));
		vars.put("setStats", new Function(new String[]{"statLevel", "atk", "intl", "def", "agl", "maxhp", "maxmp", "exp", "g"}, new JVSValue[]{new JVSValue(){
						@Override
						public Object get(Scope scope){
							String lv;
							if((lv = scope.getVal("statLevel", String.class, "")).isEmpty())
								lv = baseStats;
							setStats(lv, scope.getVal("atk", Float.class, null), scope.getVal("intl", Float.class, null), scope.getVal("def", Float.class, null), scope.getVal("agl", Float.class, null), scope.getVal("maxhp", Float.class, null), scope.getVal("maxmp", Float.class, null));
							float ex;
							if((ex = scope.getVal("exp", Float.class, -1f)) != -1)
								exp = ex;
							float go;
							if((go = scope.getVal("g", Float.class, -1f)) != -1)
								g = go;
							return null;
						}
					}}));
		vars.put("hasItem", new Function(new String[]{"item"}, new JVSValue[]{new JVSValue(){
						@Override
						public Object get(Scope scope){
							int amt = 0;
							for(MenuItem.GameItem item : items)
								if(item.id.equals(scope.getVal("item", String.class, null)))
									amt++;
							return (float)amt;
						}
					}}));
		vars.put("moveBy", new Function(new String[]{"x", "y", "speed", "noclip"}, new JVSValue[]{new JVSValue(){
						@Override
						public Object get(Scope scope){
							//float dist = (float)Math.hypot(scope.getVal("x"), scope.getVal("y"));
							move(scope.getVal("x", Float.class, null), scope.getVal("y", Float.class, null), scope.getVal("speed", Float.class, 0f), true);
							return null;
						}
					}}));
		vars.put("moveTo", new Function(new String[]{"x", "y", "speed", "noclip"}, new JVSValue[]{new JVSValue(){
						@Override
						public Object get(Scope scope){
							//float dist = (float)Math.hypot(x - (float)scope.getVal("x"), y - (float)scope.getVal("y"));
							move(scope.getVal("x", Float.class, null), scope.getVal("y", Float.class, null), scope.getVal("speed", Float.class, 0f), false);
							return null;
						}
					}}));
		vars.put("delay", new Function(new String[]{"delayTime", "func"}, new JVSValue[]{new JVSValue(){
						@Override
						public Object get(Scope scope){
							addDelay(scope.getVal("delayTime", Float.class, null), scope.getVal("func", Function.class, null), scope);
							return null;
						}
					}}));
		vars.put("this", new JVSValue.WValue(){
				@Override
				public Object get(){
					return vars;
				}
			});
		vars.put("name", new JVSValue.WValue(){
				@Override
				public Object get(){
					return name;
				}
				@Override
				public void put(Object value){
					name = value.toString();
				}
			});
		vars.put("str", new JVSValue.WValue(){
				@Override
				public Object get(){
					return str();
				}
			});
		vars.put("int", new JVSValue.WValue(){
				@Override
				public Object get(){
					return intl();
				}
			});
		vars.put("def", new JVSValue.WValue(){
				@Override
				public Object get(){
					return def();
				}
			});
		vars.put("agl", new JVSValue.WValue(){
				@Override
				public Object get(){
					return agl();
				}
			});
		vars.put("hp", new JVSValue.WValue(){
				@Override
				public Object get(){
					return hp;
				}
			});
		vars.put("maxhp", new JVSValue.WValue(){
				@Override
				public Object get(){
					return maxhp();
				}
			});
		vars.put("mp", new JVSValue.WValue(){
				@Override
				public Object get(){
					return mp;
				}
			});
		vars.put("maxmp", new JVSValue.WValue(){
				@Override
				public Object get(){
					return maxmp();
				}
			});
		vars.put("gold", new JVSValue.WValue(){
				@Override
				public Object get(){
					return g;
				}
				@Override
				public void put(Object value){
					g = (Float)value;
				}
			});
		vars.put("exp", new JVSValue.WValue(){
				@Override
				public Object get(){
					return exp;
				}
				@Override
				public void put(Object value){
					exp = (Float)value;
				}
			});
		vars.put("x", new JVSValue.WValue(){
				@Override
				public Object get(){
					//if(Tenebrae.fight == null)
						return x;
					//else
					//	return Tenebrae.fight.sprite.getX();
				}
				@Override
				public void put(Object value){
					move(value, y, 0, false);
				}
			});
		vars.put("y", new JVSValue.WValue(){
				@Override
				public Object get(){
					//if(Tenebrae.fight == null)
						return y;
					//else
					//	return Tenebrae.fight.sprite.getY();
				}
				@Override
				public void put(Object value){
					move(x, value, 0, false);
				}
			});
		vars.put("map", new JVSValue.WValue(){
				@Override
				public Object get(){
					return Tenebrae.mp.get(null);
				}
			});
		vars.put("enemy", new JVSValue.WValue(){
				@Override
				public Object get(){
					return enemy.get(null);
				}
			});
		vars.put("player", new JVSValue.WValue(){
				@Override
				public Object get(){
					return Tenebrae.player.get(null);
				}
			});
	}
	public void changeMap(TileMap map, float spawnx, float spawny){
		Rectangle rect, oRect = null;
		mapobj = map.getObject(filename);
		if(mapobj == null)
			mapobj = map.getObject("npcs");
		if(mapobj == null)
			mapobj = map.getObject("player");
		if(mapobj instanceof RectangleMapObject){
			RectangleMapObject obj = (RectangleMapObject)mapobj;
			oRect = obj.getRectangle();
			//Log.debug("Entity is a rectangle! "+pRect);
		}else if(mapobj instanceof TiledMapTileMapObject){
			TiledMapTileMapObject obj = (TiledMapTileMapObject)mapobj;
			oRect = new Rectangle(obj.getX(), obj.getY(), obj.getTextureRegion().getRegionWidth() * obj.getScaleX(), obj.getTextureRegion().getRegionHeight() * obj.getScaleY());
			//Log.debug("Entity is a tile! "+pRect);
		}
		rect = new Rectangle(oRect.getX() / map.tilebasewidth, oRect.getY() / map.tilebaseheight, oRect.getWidth() / map.tilebasewidth, oRect.getHeight() / map.tilebaseheight);

		Log.debug("Changing map!", this, spawnx, spawny, rect);
		if(spawnx == -1 || spawny == -1){//for spawnpoint
			x = rect.getX(); y = rect.getY();
		}else{//for doors and things
			x = spawnx; y = spawny;
		}

		width = rect.getWidth(); height = rect.getHeight();
		setSize(rect.getWidth() * map.tilebasewidth, rect.getHeight() * map.tilebaseheight);
		setScale(map.tilewidth / map.tilebasewidth, map.tileheight / map.tilebaseheight);
		Log.debug("scaled", width, height, getWidth(), getHeight(), getScaleX(), getScaleY());
	}

	public void move(final float newX, final float newY, final float speed, final boolean relative){
		addAction(new Action.MoveAction(this, newX, newY, speed, relative, false));
		triggerAction();
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
		if(box != null)
			box.updateHP();
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
			damage(stat.get(Stats.maxhp), true, true);
		if(mp > maxmp())
			tire(stat.get(Stats.maxmp), true, true);
		box.updateHP();
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
		Log.debug("Setting skinset! " + z + " " + tilesetName);

		removeSkin(z);

		TiledMapTileSet tileset = Tenebrae.mp.loadTileset(tilesetName);
		Skin skin = new Skin(z);
		for(TiledMapTile tile : tileset){
			Log.verbose("Objs on new skin!", tilesetName, tile.getObjects().getCount());
			MapProperties prop = tile.getProperties();
			String dirstr = prop.get("direction", String.class);
			if(dirstr != null){
				String[] dirs = dirstr.split("\\s+");
				if(dirs.length != 0)
					for(String dir : dirs)
						skin.putSkin(prop.get("speed", 0f, Float.class), dir, tile);
			}
		}

		Array<Texture> used = new Array<Texture>();
		for(TiledMapTile tile : skin)
			used.add(tile.getTextureRegion().getTexture());
		Array<Texture> disposed = new Array<Texture>();
		for(TiledMapTile tile : tileset){
			Texture tex = tile.getTextureRegion().getTexture();
			if(!used.contains(tex, true) && !disposed.contains(tex, true)){
				tex.dispose();
				disposed.add(tex);
			}
		}

		skinList.put(tilesetName, skin);
		if(tile.size != 0){
			String dir = null;
			float dirspeed = Float.MAX_VALUE;
			for(Skin skins : skinList.values())
				for(ObjectMap.Entry<Float,ArrayMap<String,TiledMapTile>> mapEntry : skins.skinList)
					for(ObjectMap.Entry<String,TiledMapTile> entry : mapEntry.value)
						for(TiledMapTile tile : this.tile)
							if(entry.value == tile && mapEntry.key < dirspeed){
								dirspeed = mapEntry.key;
								dir = entry.key;
								//Log.debug("Found active skin! "+skinList.getKey(skins,true)+" "+dir+" "+dirspeed);
							}
			updateSkins(dirspeed, dir);
		}else{
			updateSkins(0, "down");
		}
	}
	public void setSkin(String skin, float speed, String type){
		//Log.verbose("Setting skin!", skin, speed, type, skinList.get(skin));
		tile.set(skinList.get(skin).z, skinList.get(skin).getSkin(speed, type));
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
				for(ArrayMap<String,TiledMapTile> map : skin.skinList.values())
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
			removeRegion(z);
			skinList.removeKey(rem).dispose();
		}
		return rem;
	}

	public void triggerAction(){
		Log.verbose2("Wanting an action from", this, ", Was", currentAction);
		boolean stop = currentAction == null || currentAction.stop(false);
		if(stop){
			delay = 0;
			currentAction = null;
		}
		if(!actions.isEmpty())
			Log.debug("Triggerboi", actions, stop);
		if(!Tenebrae.doneLoading || delay != 0 || !stop){
			//Log.debug("..But nobody came.");
			return;
		}else if(actions.isEmpty() && stop){
			if(!doDefaultAction())
				return;
		}
		//Log.debug("Iterate!");
		currentAction = actions.removeIndex(0);
		if(currentAction != null){
			currentAction.run();
			Log.verbose2("Current action!", currentAction, delay, currentAction.manualOverride);
			//if(delay != 0 || (currentAction != null && currentAction.manualOverride))
			//	map.cover();
			if(currentAction == null)//on loading maps, currentAction gets nulled by loading of new map's scripts
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
	public void attack(boolean magic){
		if(enemy == null){
			Tenebrae.player.addDialog(name + " attacked!");
			Tenebrae.player.addDialog("...But nobody came.");
			return;
		}
		if(str() >= 0)
			enemy.damage(calcDamage(enemy, magic), false, false);
		else
			enemy.heal(calcHeal(enemy, magic), false, false);
		//for(MenuItem.GameItem item : enemy.equippedItems.values())
		//	item.run("onEquippedHit");
		removeTempStats();
	}
	public void damage(float damage, boolean override, boolean silent){
		hp -= damage;
		if(!override && hp < 0)
			hp = 0;
		finishAffect();
		//if(!silent)
		//	Tenebrae.player.addDialog(name + " got hurt by " + f(damage) + " points!");
	}
	public void heal(float heal, boolean override, boolean silent){
		hp += heal;
		if(!override && hp > maxhp())
			hp = maxhp();
		finishAffect();
		//if(!silent)
		//	Tenebrae.player.addDialog(name + " got healed by " + f(heal) + " points!");
	}
	public void tire(float damage, boolean override, boolean silent){
		mp -= damage;
		if(!override && mp < 0)
			mp = 0;
		finishAffect();
		//if(!silent)
		//	Tenebrae.player.addDialog(name + " got tired by " + f(damage) + " points!");
	}
	public void invigor(float heal, boolean override, boolean silent){
		mp += heal;
		if(!override && mp > maxmp())
			mp = maxmp();
		finishAffect();
		//if(!silent)
		//	Tenebrae.player.addDialog(name + " got invigorated by " + f(heal) + " points!");
	}
	public void failAttack(){
		Tenebrae.player.addDialog(name + " missed!");
	}
	public void finishAffect(){
		box.updateHP();
		if(isDead())
			die();
	}
	public boolean isDead(){
		return hp <= 0;
	}
	abstract public void die()

	public void addAction(Action add){
		actions.add(add);
	}
	public void addDelay(float delay, Function funcToRun, Scope context){
		addAction(new DelayAction(this, delay, false));
		if(funcToRun != null)
			addAction(new FunctionAction(funcToRun, context));
	}

	Rectangle rectBuffer = new Rectangle();
	public Rectangle toTileRect(){
		rectBuffer.set(x, y, width, height);
		return rectBuffer;
	}

	@Override
	public Object get(Scope scope){
		return vars;
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
		if(targetX != -1 || targetY != -1){
			float adx = (targetX - x) / Player.actstep, ady = (targetY - y) / Player.actstep;
			targetX = targetY = -1;
			float ppx = x, ppy = y;
			for(int i = 0; i < Player.actstep; i++){
				//float px = x, py = y;
				x += adx;
				y += ady;
			}
			updateSkins(x - ppx, y - ppy);
		}else{
			updateSkins(0, 0);
		}

		setPosition(x * Tenebrae.player.map.tilewidth, y * Tenebrae.player.map.tilewidth);
	}

	@Override
	public void draw(Batch batch, float parentAlpha){
		if(Tenebrae.player.map.hasOnMap(this))
			super.draw(batch, parentAlpha);
	}

	@Override
	public String toString(){
		return super.toString() + "§" + filename + "{" + x + "x" + y + ", " + width + "x" + height + ", " + getScaleX() + "x" + getScaleY() + ", " + tile + "}";
	}

	public void endSelf(){
		for(Skin skin : skinList.values())
			skin.dispose();
		dispose();
	}

	public static class Skin implements Disposable, Iterable<TiledMapTile>{
		ArrayMap<Float,ArrayMap<String, TiledMapTile>> skinList;
		int z;

		Skin(int z){
			skinList = new ArrayMap<Float,ArrayMap<String, TiledMapTile>>();
			this.z = z;
		}
		public void putSkin(float speed, String type, TiledMapTile tile){
			Log.verbose("Putting skin! " + speed + " " + type + " " + tile.getId());
			ArrayMap<String,TiledMapTile> map;
			if(skinList.get(speed) != null)
				map = skinList.get(speed);
			else
				map = new ArrayMap<String,TiledMapTile>();
			map.put(type, tile);
			skinList.put(speed, map);
			sortSkinList();
		}
		public TiledMapTile getSkin(float speed, String type){
			float lowspeed = 0;
			for(float newspeed : skinList.keys())
				if(newspeed > speed){
					//Log.debug("Speed stop! "+lowspeed+" "+speed+" "+newspeed);
					break;
				}else{
					//Log.debug("Speed go! "+lowspeed+" "+speed+" "+newspeed);
					lowspeed = newspeed;
				}
			//Log.debug("Speed end! "+lowspeed+" "+speed);
			return skinList.get(lowspeed).get(type);
		}
		public void sortSkinList(){
			ArrayMap<Float,ArrayMap<String, TiledMapTile>> sorted = new ArrayMap<Float,ArrayMap<String, TiledMapTile>>();
			while(skinList.size > 0){
				int lowest = 0;
				for(int i = 0; i < skinList.size; i++)
					if(skinList.getKeyAt(i) < skinList.getKeyAt(lowest))
						lowest = i;
				sorted.put(skinList.getKeyAt(lowest), skinList.getValueAt(lowest));
				skinList.removeIndex(lowest);
			}
			skinList = sorted;
		}

		@Override
		public Iterator<TiledMapTile> iterator(){
			Array<TiledMapTile> tiles = new Array<TiledMapTile>();
			for(ArrayMap<String,TiledMapTile> map : skinList.values())
				for(TiledMapTile tile : map.values())
					if(!tiles.contains(tile, true))
						tiles.add(tile);
			return tiles.iterator();
		}

		@Override
		public void dispose(){
			Array<Texture> disposed = new Array<Texture>();
			for(TiledMapTile tile : this){
				Texture texture = tile.getTextureRegion().getTexture();
				if(!disposed.contains(texture, true)){
					texture.dispose();
					disposed.add(texture);
				}
			}
		}
	}
}
