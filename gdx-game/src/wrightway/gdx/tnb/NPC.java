package wrightway.gdx.tnb;

import wrightway.gdx.*;
import wrightway.gdx.JVSValue.Scope;
import wrightway.gdx.JVSValue.WObject;
import wrightway.gdx.JVSValue.Function;
import com.badlogic.gdx.files.*;
import wrightway.gdx.tnb.Action.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.g2d.*;
import java.util.*;

public class NPC extends Character{
	Array<FunctionAction> idleRoutine;
	boolean enabled = true;
	public static final Function setup = JVSParser.parseCode("moveTo(x, y, 0)  this.trigger = trigger  this.enter = enter  this.exit = exit  if(idleRoutine != null)setIdleRoutine(idleRoutine);", new String[]{"x", "y", "trigger", "enter", "exit", "idleRoutine"});

	public NPC(String name, Function script){
		super(name);

		vars.put("setIdleRoutine", new Function(new String[]{"routine"}, new JVSValue[]{new JVSValue(){
						@Override
						public Object get(Scope scope){
							idleRoutine = new Array<FunctionAction>();
							Array<Object> routine = (scope.getVal("routine", JVSValue.WObjectArr.class, null)).toArray();
							for(Function a : routine)
								idleRoutine.add(new FunctionAction(a, vars));
							return null;
						}
					}}));
		vars.put("enable", new Function(new String[]{}, new JVSValue[]{new JVSValue(){
						@Override
						public Object get(Scope scope){
							enabled = true;
							boolean had = Tenebrae.mp.npcs.contains(NPC.this, true);
							if(!had)
								Tenebrae.mp.npcs.add(NPC.this);
							return !had;
						}
					}}));
		vars.put("disable", new Function(new String[]{}, new JVSValue[]{new JVSValue(){
						@Override
						public Object get(Scope scope){
							enabled = false;
							boolean had = Tenebrae.mp.npcs.contains(NPC.this, true);
							if(had)
								return Tenebrae.mp.npcs.removeValue(NPC.this, true);
							return had;
						}
					}}));
		vars.put("setup", new Function(new String[]{"x", "y", "trigger", "enter", "exit", "idleRoutine"}, new JVSValue[]{new JVSValue(){
						@Override
						public Object get(Scope scope){
							setup.getFromScope(vars, scope);
							return null;
						}
					}}));
		vars.put("trigger", JVSValue.nulll);
		vars.put("enter", JVSValue.nulll);
		vars.put("exit", JVSValue.nulll);

		script.get(vars, null);
		hp = maxhp();
		mp = maxmp();
		//box.updateHP(this);
	}

	@Override
	public boolean doDefaultAction(){
		if(idleRoutine == null || idleRoutine.isEmpty())
			return false;
		FunctionAction a;
		addAction(a = idleRoutine.removeIndex(0));
		Log.debug("Default action!", a, actions, actions.size);
		idleRoutine.add(a);
		return true;
	}

	@Override
	public void die(){
		throw new UnsupportedOperationException("...How?");
	}

	@Override
	public void draw(Batch batch, float parentAlpha){
		if(enabled)
			super.draw(batch, parentAlpha);
	}
}
