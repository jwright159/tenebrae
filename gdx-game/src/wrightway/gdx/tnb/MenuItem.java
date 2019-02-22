package wrightway.gdx.tnb;

import wrightway.gdx.*;
import wrightway.gdx.JVSValue.Scope;
import wrightway.gdx.JVSValue.WObject;
import wrightway.gdx.JVSValue.Function;
import com.badlogic.gdx.maps.*;
import com.badlogic.gdx.files.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.*;
import wrightway.gdx.tnb.Character.Stats;
import com.badlogic.gdx.scenes.scene2d.ui.*;

public class MenuItem implements JVSValue{//implements scoped so menuitems can be run by code (ex: the kill command)
	Scope vars;
	String name, catagory;
	EntityBox.MenuOption option;
	final Character owner;
	MenuItem(String catagory, String name, Function script, Character owner){
		this.catagory = catagory;
		this.name = name;
		vars = new Scope(null, "mi"+name);
		this.owner = owner;
		vars.put("onUse", script != null ? script : JVSValue.nulll);//for customfuncs in the java code
		vars.put("this", new JVSValue.WValue(){
				@Override
				public Object get(){
					return vars;
				}
			});
		vars.put("player", new JVSValue.WValue(){
				@Override
				public Object get(){
					return Tenebrae.player.get(null);
				}
			});
		vars.put("map", new JVSValue.WValue(){
				@Override
				public Object get(){
					return Tenebrae.mp.get(null);
				}
			});
	}
	public boolean contains(String name){
		return vars.containsKey(name);
	}
	public void run(String funcName){
		Tenebrae.player.closeMenus();
		vars.run(funcName);
	}
	@Override
	public Object get(Scope scope){
		return vars;
	}
	@Override
	public String toString(){
		return super.toString() + "§" + catagory + "." + name;
	}

	public static class GameItem extends MenuItem{
		ArrayMap<Character.Stats,Float> equip;
		int life;
		float eChance;
		String type, id;
		boolean magic;
		Array<EntityBox.MenuOption> toPutInBox = new Array<EntityBox.MenuOption>();
		EntityBox.MenuOption equipOpt, unequipOpt;

		GameItem(String id, String name, Character owner, ArrayMap<Character.Stats,Float> equip, int life, String catagory, String type, float enemyChance, boolean isMagic, final Skin skin){
			super(catagory, name, null, owner);
			this.id = id;
			this.equip = equip;
			this.life = life;
			this.type = type;
			this.eChance = enemyChance;
			this.magic = isMagic;

			vars.put("decay", new Function(new String[]{}, new JVSValue[]{new JVSValue(){
							@Override
							public Object get(Scope scope){
								decay();
								return null;
							}
						}}));
			vars.put("equip", new Function(new String[]{}, new JVSValue[]{new JVSValue(){
							@Override
							public Object get(Scope scope){
								equip();
								return null;
							}
						}}));
			vars.put("unequip", new Function(new String[]{}, new JVSValue[]{new JVSValue(){
							@Override
							public Object get(Scope scope){
								unequip();
								return null;
							}
						}}));
			vars.put("setStat", new Function(new String[]{"stat", "value"}, new JVSValue[]{new JVSValue(){
							@Override
							public Object get(Scope scope){
								setStat(Character.Stats.valueOf(scope.getVal("stat", String.class, null)), scope.getVal("value", Float.class, null));
								return null;
							}
						}}));
			vars.put("setStats", new Function(new String[]{"atk", "intl", "def", "agl", "maxhp", "maxmp"}, new JVSValue[]{new JVSValue(){
							@Override
							public Object get(Scope scope){
								setStats(scope.getVal("atk", Float.class, null), scope.getVal("intl", Float.class, null), scope.getVal("def", Float.class, null), scope.getVal("agl", Float.class, null), scope.getVal("maxhp", Float.class, null), scope.getVal("maxmp", Float.class, null));
								return null;
							}
						}}));
			vars.put("attack", new Function(new String[]{"atk", "intl", "def", "agl", "maxhp", "maxmp"}, new JVSValue[]{new JVSValue(){
							@Override
							public Object get(Scope scope){
								GameItem.this.owner.setStats("_temp" + GameItem.this.owner.stats.size, scope.getVal("atk", Float.class, 0f), scope.getVal("intl", Float.class, 0f), scope.getVal("def", Float.class, 0f), scope.getVal("agl", Float.class, 0f), scope.getVal("maxhp", Float.class, 0f), scope.getVal("maxmp", Float.class, 0f));
								GameItem.this.owner.attack(magic);
								return null;
							}
						}}));
			vars.put("addFunc", new Function(new String[]{"name", "func"}, new JVSValue[]{new JVSValue(){
							@Override
							public Object get(final Scope scope){
								EntityBox.MenuOption opt = new EntityBox.MenuOption(scope.getVal("name", String.class, null), null, skin){
									@Override
									public void open(){
										scope.run("func");
									}
								};
								addOption(opt);
								return null;
							}
						}}));
			vars.put("remove", new Function(new String[]{}, new JVSValue[]{new JVSValue(){
							@Override
							public Object get(Scope scope){
								GameItem.this.owner.removeItem(GameItem.this);
								return null;
							}
						}}));
			vars.put("name", new JVSValue.WValue(){
					@Override
					public Object get(){
						return GameItem.this.name;
					}
					@Override
					public void put(Object value){
						GameItem.this.name = value.toString();
					}
				});
			vars.put("catagory", new JVSValue.WValue(){
					@Override
					public Object get(){
						return GameItem.this.catagory;
					}
					@Override
					public void put(Object value){
						GameItem.this.catagory = value.toString();
					}
				});
			vars.put("durability", new JVSValue.WValue(){
					@Override
					public Object get(){
						return GameItem.this.life;
					}
					@Override
					public void put(Object value){
						GameItem.this.life = (int)(float)value;
					}
				});
			vars.put("enemyUseChance", new JVSValue.WValue(){
					@Override
					public Object get(){
						return eChance;
					}
					@Override
					public void put(Object value){
						eChance = value;
					}
				});
			vars.put("type", new JVSValue.WValue(){
					@Override
					public Object get(){
						return GameItem.this.type;
					}
					@Override
					public void put(Object value){
						GameItem.this.type = value.toString();
					}
				});
			vars.put("magic", new JVSValue.WValue(){
					@Override
					public Object get(){
						return magic;
					}
					@Override
					public void put(Object value){
						magic = value;
					}
				});

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
		GameItem(String name, Function script, Character owner, Skin skin){
			this(name, "Item", owner, new ArrayMap<Character.Stats,Float>(), 0, "Items", "Item", 0, false, skin);
			Log.verbose("Parsing file of item!");
			script.get(vars, null);
			if(isEquipable()){
				addOption(equipOpt);
				addOption(unequipOpt);
			}
			Log.debug("Item " + name);
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
			return contains("onEquip");
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
	}
}
