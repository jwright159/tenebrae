package wrightway.gdx.tnb;

import wrightway.gdx.*;
import com.badlogic.gdx.maps.tiled.*;
import com.badlogic.gdx.maps.*;
import com.badlogic.gdx.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.files.*;
import com.badlogic.gdx.utils.*;
import wrightway.gdx.tnb.MenuItem.GameItem;
import com.leff.midi.util.*;
import com.leff.midi.event.*;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;

public class Enemy extends Character{
	String enctrText;
	boolean fleeable;
	int turns;
	MidiWavSync music;
	ArrayMap<Integer,EnemyWeapon> weapons;

	Enemy(Prototype script){
		super("die");
		getGlobals().load(new EnemyLib());
		turns = 0;
		enemy = Tenebrae.player;

		name = "Enemy";
		box = new EntityBox(this, Tenebrae.t.getSkin());
		setStats(baseStats, 0, 0, 0, 0, 1, 1);
		exp = 0;
		g = 0;
		enctrText = "An enemy approaches!";
		fleeable = true;
		weapons = new ArrayMap<>();

		new LuaClosure(script, getGlobals()).call();

		hp = maxhp();
		mp = maxmp();
		box.updateHP();

		//Tenebrae.debug("Enemy made! "+this);
	}

	public String battleText(){
		if(turns == 0)
			return enctrText;
		return enctrText;
	}
	@Override
	public void die(){
		getGlobals().get("onDie").checkfunction().call();
		Tenebrae.player.endEncounter();
	}

	public void useItem(){
		float totChance = 0;
		for(GameItem item : items)
			totChance += item.eChance;
		float random = (float)Math.random() * totChance;
		totChance = 0;
		for(GameItem item : items){
			totChance += item.eChance;
			if(totChance > random){
				item.run("onUse");
				break;
			}
		}
	}

	@Override
	public void dispose(){
		Log.debug("Enemy disposed");
		super.dispose();
		music.dispose();
		for(EnemyWeapon weapon : weapons.values())
			weapon.dispose();
	}
	
	public class EnemyLib extends TwoArgFunction{
		@Override
		public LuaValue call(LuaValue modname, LuaValue env){
			LuaTable library = tableOf();

			library.set("setMusic", new OneArgFunction(){
							@Override
							public LuaValue call(LuaValue filename){
								music = Tenebrae.mp.loadMidiWav(filename.checkjstring(), new MidiEventListener(){
										@Override
										public void onStart(boolean fromBeginning){
											// TODO: Implement this method
										}
										@Override
										public void onEvent(MidiEvent event, long ms){
											EnemyWeapon weapon = weapons.get(((NoteOn)event).getChannel());
											if(weapon != null)
												weapon.spawn();
										}
										@Override
										public void onStop(boolean finished){
											// TODO: Implement this method
										}
									});
								//return music;
								return NONE;
							}
						});
			library.set("channelWeapon", new TwoArgFunction(){
							@Override
							public LuaValue call(LuaValue channel, LuaValue filename){
								EnemyWeapon weapon = Tenebrae.mp.loadEnemyWeapon(filename.checkjstring(), Enemy.this);
								weapons.put(channel.checkint(), weapon);
								//return weapon;
								return NONE;
							}
						});
			library.setmetatable(tableOf());
			library.getmetatable().set(INDEX, new TwoArgFunction(){
					@Override
					public LuaValue call(LuaValue self, LuaValue key){
						switch(key.checkjstring()){
							case "encounter":
								return valueOf(enctrText);
							case "canFlee":
								return valueOf(fleeable);
							default:
								return NIL;
						}
					}
				});
			library.getmetatable().set(NEWINDEX, new ThreeArgFunction(){
					@Override
					public LuaValue call(LuaValue self, LuaValue key, LuaValue value){
						switch(key.checkjstring()){
							case "encounter":
								enctrText = value.checkjstring();
								break;
							case "canFlee":
								fleeable = value.checkboolean();
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
