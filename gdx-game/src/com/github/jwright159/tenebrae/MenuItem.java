package com.github.jwright159.tenebrae;

import com.github.jwright159.gdx.*;
import com.github.jwright159.tenebrae.Character.Stats;
import com.badlogic.gdx.maps.*;
import com.badlogic.gdx.files.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;

public abstract class MenuItem{
	public String name, category;
	
	public MenuItem(String category, String name){
		this.category = category;
		this.name = name;
	}
	
	public abstract void run(String funcName)
	
	@Override
	public String toString(){
		return super.toString() + "§" + category + "." + name;
	}

	public static class ScriptItem extends MenuItem implements ScriptGlob{
		protected Tenebrae game;
		private Globals globals;
		public EntityBox.MenuOption option;
		protected Character owner;
		
		public ScriptItem(Tenebrae game, String category, String name, Character owner, Prototype onUse){
			super(category, name);
			this.game = game;
			this.owner = owner;
			globals = game.new StdEntGlobals();
			globals.load(new ScriptItemLib());
			if(onUse != null)
				globals.set("onUse", new LuaClosure(onUse, globals));
		}
		
		@Override
		public void run(String funcName){
			game.player.closeMenus();
			globals.get(funcName).checkfunction().call();
		}
		
		@Override
		public Globals getGlobals(){
			return globals;
		}
		
		public class ScriptItemLib extends TwoArgFunction{
			@Override
			public LuaValue call(LuaValue modname, LuaValue env){
				LuaTable library = tableOf();

				library.setmetatable(tableOf());
				library.getmetatable().set(INDEX, new TwoArgFunction(){
						@Override
						public LuaValue call(LuaValue self, LuaValue key){
							switch(key.checkjstring()){
								case "owner":
									return owner.getGlobals();
								case "name":
									return valueOf(ScriptItem.this.name);
								case "category":
									return valueOf(category);
								default:
									return NIL;
							}
						}
					});
				library.getmetatable().set(NEWINDEX, new ThreeArgFunction(){
						@Override
						public LuaValue call(LuaValue self, LuaValue key, LuaValue value){
							switch(key.checkjstring()){
								case "owner":
									self.error("owner is read-only");
									break;
								case "name":
									ScriptItem.this.name = value.checkjstring();
									break;
								case "category":
									category = value.checkjstring();
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

	public static class GameItem extends ScriptItem{
		private ArrayMap<Character.Stats,Float> equip;
		private int life;
		public float eChance;
		public String type, id;
		private boolean magic;
		public Array<EntityBox.MenuOption> toPutInBox = new Array<EntityBox.MenuOption>();
		public EntityBox.MenuOption equipOpt, unequipOpt;
		private Skin skin;

		public GameItem(Tenebrae game, String id, String name, Character owner, ArrayMap<Character.Stats,Float> equip, int life, String catagory, String type, float enemyChance, boolean isMagic, Skin skin){
			super(game, catagory, name, owner, null);
			this.id = id;
			this.equip = equip;
			this.life = life;
			this.type = type;
			this.eChance = enemyChance;
			this.magic = isMagic;
			this.skin = skin;
			getGlobals().load(new GameItemLib());

			equipOpt = new EntityBox.MenuOption("Equip", null, skin){
				@Override
				public void open(){
					if(isEquipped())
						return;
					run("onEquip");
				}
			};
			unequipOpt = new EntityBox.MenuOption("Unequip", null, skin){
				@Override
				public void open(){
					if(!isEquipped())
						return;
					run("onUnequip");
				}
			};
		}
		public GameItem(Tenebrae game, String name, Prototype script, Character owner, Skin skin){
			this(game, name, "Item", owner, new ArrayMap<Character.Stats,Float>(), 0, "Items", "Item", 0, false, skin);
			Log.gameplay("Parsing file of item!");
			new LuaClosure(script, getGlobals()).call();
			if(isEquipable()){
				addOption(equipOpt);
				addOption(unequipOpt);
			}
			Log.debug("Item", this);
		}
		public void equip(){
			owner.unequip(type);
			owner.equippedItems.put(type, this);
			owner.setStats(type, equip.get(Character.Stats.str), equip.get(Character.Stats.intl), equip.get(Character.Stats.def), equip.get(Character.Stats.agl), equip.get(Character.Stats.maxhp), equip.get(Character.Stats.maxmp));
		}
		public void unequip(){
			owner.equippedItems.removeKey(type);
			owner.removeStats(type);
		}
		public boolean isEquipable(){
			return !getGlobals().get("onEquip").isnil();
		}
		public boolean isEquipped(){
			return owner.equippedItems.get(type) == this;
		}
		public void addOption(EntityBox.MenuOption opt){
			if(option == null)
				toPutInBox.add(opt);
			else
				option.box.addOption(opt);
		}

		public void setStat(Stats stat, float value){
			equip.put(stat, value);
		}
		public void setStats(float str, float intl, float def, float agl, float maxhp, float maxmp){
			//Tenebrae.debug("Updating stats of type " + type + " to " + toString() + "! " + atk + " " + def + " " + agl + " " + maxhp + " " + maxmp);
			setStat(Stats.str, str);
			setStat(Stats.intl, intl);
			setStat(Stats.def, def);
			setStat(Stats.agl, agl);
			setStat(Stats.maxhp, maxhp);
			setStat(Stats.maxmp, maxmp);
		}
		public void parseStats(String raw){
			Array<Float> loadStats = new Array<Float>();
			String[] rawStats = raw.toString().split("\\s+");
			for(String rawStat : rawStats)
				loadStats.add(Float.parseFloat(rawStat));
			setStats(loadStats.get(0), loadStats.get(1), loadStats.get(2), loadStats.get(3), loadStats.get(4), loadStats.get(5));
		}

		public void decay(){
			//Tenebrae.debug("Decaying "+toString()+"! "+life);
			if(life <= 0)
				return;
			life--;
			if(life <= 0){
				//Tenebrae.debug("Item decayed!");
				run("onDestroy");
			}
		}
		
		public class GameItemLib extends TwoArgFunction{
			@Override
			public LuaValue call(LuaValue modname, LuaValue env){
				LuaTable library = tableOf();

				library.set("decay", new ZeroArgFunction(){
						@Override
						public LuaValue call(){
							decay();
							return NONE;
						}
					});
				library.set("equip", new ZeroArgFunction(){
						@Override
						public LuaValue call(){
							equip();
							return NONE;
						}
					});
				library.set("unequip", new ZeroArgFunction(){
						@Override
						public LuaValue call(){
							unequip();
							return NONE;
						}
					});
				library.set("setStat", new TwoArgFunction(){
						@Override
						public LuaValue call(LuaValue stat, LuaValue value){
							setStat(Character.Stats.valueOf(stat.checkjstring()), (float)value.checkdouble());
							return NONE;
						}
					});
				library.set("setStats", new VarArgFunction(){ // atk, intl, def, agl, maxhp, maxmp
						@Override
						public Varargs invoke(Varargs args){
							setStats((float)args.checkdouble(1), (float)args.checkdouble(2), (float)args.checkdouble(3), (float)args.checkdouble(4), (float)args.checkdouble(5), (float)args.checkdouble(6));
							return NONE;
						}
					});
				library.set("attack", new VarArgFunction(){ // enemy, atk, intl, def, agl, maxhp, maxmp
						@Override
						public Varargs invoke(Varargs args){
							if(args.narg() > 1)
								GameItem.this.owner.setStats("_temp" + owner.stats.size, (float)args.optdouble(2, 0), (float)args.optdouble(3, 0), (float)args.optdouble(4, 0), (float)args.optdouble(5, 0), (float)args.optdouble(6, 0), (float)args.optdouble(7, 0));
							GameItem.this.owner.attack(args.isnil(1) ? null : (Character)args.checktable(1).getmetatable().get(Entity.ENTITY).checkuserdata(Character.class), magic);
							return NONE;
						}
					});
				library.set("addFunc", new TwoArgFunction(){
						@Override
						public LuaValue call(LuaValue name, LuaValue function){
							final LuaFunction func = function.checkfunction();
							EntityBox.MenuOption opt = new EntityBox.MenuOption(name.checkjstring(), null, skin){
								@Override
								public void open(){
									game.player.closeMenus();
									func.call();
								}
							};
							addOption(opt);
							return NONE;
						}
					});
				library.set("remove", new ZeroArgFunction(){
						@Override
						public LuaValue call(){
							GameItem.this.owner.removeItem(GameItem.this);
							return NONE;
						}
					});
				library.setmetatable(tableOf());
				library.getmetatable().set(INDEX, new TwoArgFunction(){
						@Override
						public LuaValue call(LuaValue self, LuaValue key){
							switch(key.checkjstring()){
								case "durability":
									return valueOf(life);
								case "enemyUseChance":
									return valueOf(eChance);
								case "type":
									return valueOf(type);
								case "magic":
									return valueOf(magic);
								default:
									return NIL;
							}
						}
					});
				library.getmetatable().set(NEWINDEX, new ThreeArgFunction(){
						@Override
						public LuaValue call(LuaValue self, LuaValue key, LuaValue value){
							switch(key.checkjstring()){
								case "durability":
									life = value.checkint();
									break;
								case "enemyUseChance":
									eChance = (float)value.checkdouble();
									break;
								case "type":
									type = value.checkjstring();
									break;
								case "magic":
									magic = value.checkboolean();
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
