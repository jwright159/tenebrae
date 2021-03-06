package com.github.jwright159.tenebrae;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.maps.*;
import com.badlogic.gdx.maps.objects.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.*;
import com.github.jwright159.gdx.*;
import com.github.jwright159.gdx.actor.*;
import com.github.jwright159.tenebrae.Action.*;
import com.github.jwright159.tenebrae.EntityBox.*;
import com.github.jwright159.tenebrae.MenuItem.*;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;
import com.badlogic.gdx.graphics.g2d.*;

public class Player extends Character{
	private boolean firstFrame;// True for first frame after map load, for skipping exittriggers (just call them manually if you need them instead of auto detection)
	public float speedMult = 1;
	public int lv = -1;
	private ArrayMap<Float, ArrayMap<Stats, Float>> statTable;
	public final static int actstep = 1;
	public ButtonBox buttonBox;
	public Label dialogBox;
	private Container<Stack> mapRect;
	public Table table;
	private Table uiTable;
	public String trueName;
	private Array<Trigger> ptriggers;
	public Vector3 lastCameraPos;
	
	public Rectangle activeDeadzone;
	public Rectangle dzRect, bigdzRect;
	public float deadzone = Tenebrae.DEADZONE_DEFAULT;
	public boolean collide = true;
	
	public static final float JOYSPEED = 2f;
	public static final float SPRINTMULT = 2f;

	public Player(Tenebrae game){
		super(game, "player");
		Log.gameplay("Making Player");
		lastCameraPos = new Vector3();
		game.mappack.charas.add(this);

		com.badlogic.gdx.scenes.scene2d.ui.Skin skin = game.getSkin();
		table = game.getTable();
		table.setDebug(Tenebrae.TABLEDEBUG);

		Stack stack = new Stack();
		Container<Table> uiRect = new Container<Table>();
		uiTable = new FocusTable(skin);
		uiTable.setDebug(Tenebrae.TABLEDEBUG);
		uiRect.setActor(uiTable);
		uiRect.pad(Tenebrae.MARGIN).fill();
		stack.add(uiRect);
		Table dialogTable = new Table(skin);
		dialogTable.setDebug(Tenebrae.TABLEDEBUG);
		stack.add(dialogTable);
		dialogTable.setFillParent(true);
		//Table consoleTable = new Table(skin);
		//stack.add(consoleTable);
		table.add(stack).grow();

		Table buttonPane = new Table(skin);
		buttonPane.setDebug(Tenebrae.TABLEDEBUG);
		buttonPane.background("window");
		buttonPane.add(buttonBox = new ButtonBox(this, skin)).pad(Tenebrae.MARGIN).grow();
		uiTable.add(buttonPane).grow().uniform();

		mapRect = new Container<Stack>().fill();
		mapRect.setDebug(Tenebrae.TABLEDEBUG);
		uiTable.add(mapRect).padLeft(Tenebrae.MARGIN).growY().width(Value.percentHeight(1f, mapRect));
		Stack menuStack = new Stack();
		mapRect.setActor(menuStack);
		menuStack.setFillParent(true);
		menuStack.add(buttonBox.menu.box);
		menuStack.add(buttonBox.items.box);

		Table playerPane = new Table(skin);
		playerPane.setDebug(Tenebrae.TABLEDEBUG);
		playerPane.background("window");
		box.remove();
		playerPane.add(box = new PlayerBox(this, skin)).pad(Tenebrae.MARGIN).grow();
		uiTable.add(playerPane).padLeft(Tenebrae.MARGIN).grow().uniform();

		dialogTable.add().grow();
		
		final Table dialogBoxBox = new Table(skin);
		dialogBox = new Label("", skin, "dialog"){
			@Override
			public void setText(CharSequence text){
				super.setText(text);
				if(text == null || text.length() == 0){
					setVisible(false);//hasDialog
					dialogBoxBox.setVisible(false);
				}else{
					setVisible(true);
					dialogBoxBox.setVisible(true);
				}
			}
		};
		dialogBox.setWrap(true);
		dialogBox.setAlignment(Align.topLeft);
		dialogBox.setText(null);
		dialogBoxBox.setDebug(Tenebrae.TABLEDEBUG);
		dialogBoxBox.background("window");
		dialogBoxBox.add(dialogBox).pad(Tenebrae.MARGIN).grow();
		dialogTable.row();
		dialogTable.add(dialogBoxBox).pad(Tenebrae.MARGIN).expandX().fill().height(Value.percentHeight(0.3f, dialogTable));

		//consoleTable.add(Tenebrae.console).fillX().bottom();
		
		/*Table zoomt = new Table();
		 game.getUiStage().addActor(zoomt);
		 zoomt.setFillParent(true);
		 zoomt.setDebug(true);
		 for(int i = 0; i < 12; i++){
		 for(int j = 0; j < 16; j++)
		 zoomt.add().grow();
		 zoomt.row();
		 }
		 ((OrthographicCamera)game.getUiStage().getCamera()).zoom = 0.25f;*/
		// What have we learned? zoom=1/4 means width and height are 1/4th

		table.validate();

		statTable = new ArrayMap<Float, ArrayMap<Stats, Float>>();
		setPlayerName("Player");
		name = "You";
		setStats(baseStats, 0, 0, 0, 0, 0, 0);
		exp = 0;
		g = 0;
		
		getGlobals().load(new PlayerLib());

		firstFrame = true; // For GameScreen.resize()
		setExpanded(false);
	}
	@Override
	public void changeMap(TileMap map){
		Log.debug("Changing map!", game.map, map);
		firstFrame = true;
		if(game.map != null){
			game.map.dispose();
			game.map = null;
		}
		game.map = map;
		game.getStage().addActor(map);
		map.call();

		super.changeMap(map);
		for(Character c : game.mappack.charas)
			if(c != this)
				c.changeMap(map);

		//Log.debug("Changing player! " + toString());
		
		updateMapRect();

		LuaValue onCreate = game.mappack.getGlobals().get("onCreate");
		if(!onCreate.isnil())
			onCreate.call(game.mappack.getGlobals());
		
		game.setFocusTable(null);
	}

	public void setPlayerName(String name){
		trueName = name;
		box.name.setText(trueName);
	}

	public boolean isColliding(){
		if(!collide)
			return false;
		Log.gameplay("isColliding?", getX(), getY(), getX(Align.right), getY(Align.top));
		for(int i = (int)getX(); i <= getX(Align.right); i++)
			for(int j = (int)getY(); j <= getY(Align.top); j++)
				if(isColliding(i, j))
					return true;
		return false;
	}
	public boolean isColliding(int tileX, int tiley){
		MapObjects objs = game.map.getCollisionObjects(tileX, tiley);
		if(objs != null){
			for(RectangleMapObject obj : objs){
				Log.gameplay("Collision?", toRect(), obj.getRectangle());
				if(toRect().overlaps(obj.getRectangle())){
					Log.gameplay("Collision!");
					return true;
				}
			}
		}
		return false;
	}
	public Vector2 getClosestCell(){
		int rx,ry;
		if(Math.ceil(getX()) - getX() >= getX(Align.right) - Math.floor(getX(Align.right)))
			rx = (int)getX();
		else
			rx = (int)getX(Align.right);
		if(Math.ceil(getY()) - getY() >= getY(Align.top) - Math.floor(getY(Align.top)))
			ry = (int)getY();
		else
			ry = (int)getY(Align.top);
		return new Vector2(rx, ry);
	}
	private Array<Trigger> getCollidingTriggerObjects(){
		return game.map.getCollidingObjects(toRect(), "onTrigger");
	}
	private Array<Trigger> getCollidingEnteranceObjects(){
		return game.map.getCollidingObjects(toRect(), "onEnter");
	}
	private Trigger getBestTrigger(){
		Array<Trigger> trigs = getCollidingTriggerObjects();
		Trigger rtn = null;
		float best = 0;
		Rectangle inter = new Rectangle();

		for(Trigger trig : trigs){
			boolean disabled = trig.getProperties().get("disabled", false, Boolean.class);
			Rectangle rect = trig.getRectangle();
			Log.gameplay("Obj", rect);
			Log.gameplay("Player", toRect());
			Intersector.intersectRectangles(rect, toRect(), inter);
			Log.gameplay("Intersect", inter);
			//Log.verbose("Intersecting triggers! " + obj.getName() + " " + rect + " " + player + " " + inter + " " + disabled);
			//rect.set(rect.getX() * map.tilebasewidth, rect.getY() * map.tilebaseheight, rect.getWidth() * map.tilebasewidth, rect.getHeight() * map.tilebaseheight);
			Log.gameplay("Rescaled", rect);

			if(inter.area() > best && !disabled){
				best = inter.area();
				rtn = trig;
			}
		}

		return rtn;
	}
	public boolean triggerBestTrigger(){
		Trigger trig = getBestTrigger();
		Log.gameplay("Requesting trigger!", trig);
		if(trig != null){
			trig.trigger("onTrigger");
			return true;
		}else
			return false;
	}

	public void addDialog(String dialog){
		addDialog(dialog, 0, true, false);
	}
	public void addDialog(String dialog, float delay){
		addDialog(dialog, delay, false, false);
	}
	public void addDialog(String dialog, float delay, boolean tap, boolean tapDelay){
		Log.gameplay("New dialog!", '"' + dialog + '"', delay, tap, tapDelay);
		addAction(new DialogAction(game, this, dialog, delay, tap, tapDelay));
	}
	@Override
	public void triggerAction(){
		triggerAction(false);
	}
	public void triggerAction(boolean touched){
		Log.gameplay("Wanting an action! Was", currentAction, "Might be", hasAction() ? getAction() : null);
		if(currentAction != null && currentAction.stop(touched)){
			delay = 0;
			currentAction = null;
		}
		if(!hasAction() || !game.doneLoading || delay != 0 || (currentAction != null && !currentAction.stop(touched))){
			//Log.debug("..But nobody came.");
			return;
		}
		//Log.debug("Iterate!");
		currentAction = removeAction();
		if(currentAction != null){
			currentAction.run();
			Log.gameplay("Current action!", currentAction, delay, currentAction == null ? null : currentAction.manualOverride);
			if(delay != 0 || (currentAction != null && currentAction.manualOverride))
				;//map.cover();
			else if(currentAction == null)//on loading maps, currentAction gets nulled by loading of new map's scripts
				return;
			else
				triggerAction(false);
		}else{
			//Log.debug("Trigger is null!");
			triggerAction(touched);
		}
	}
	public boolean performBack(){
		//Log.debug("Performing back!");
		if(buttonBox.getActiveBox() != null){
			MenuBox openedBox = buttonBox.getActiveBox();
			MenuBox active = openedBox.findActiveLeaf();
			//Log.debug("Backing up from "+active+"!");
			active.setVisible(false);
			if(buttonBox.getActiveBox() == null){
				setUiDisabled(false);
				game.setFocusTable(null);
			}else{
				active = openedBox.findActiveLeaf();
				active.setFocusTableToActive();
			}
			return true;
		}else if(currentAction != null){
			triggerAction(true);
			if(currentAction == null)
				return true;
		}else if(isExpanded()){
			setExpanded(false);
			return true;
		}
		//Log.debug("But you are backed against the wall.");
		return false;
	}
	public void closeMenus(){
		while(buttonBox.getActiveBox() != null)
			performBack();
	}

	public boolean isExpanded(){
		return uiTable.isVisible();
	}
	public void setExpanded(boolean expanded){
		activeDeadzone = expanded ? dzRect : bigdzRect;
		uiTable.setVisible(expanded);
		if(dzRect != null)
			moveCamera();
		//Log.debug("Expanding! " + expanded);
	}
	public void setUiDisabled(boolean disabled){
		buttonBox.menu.setDisabled(disabled);
		buttonBox.items.setDisabled(disabled);
	}

	public void addG(float g){
		this.g += g;
	}

	public void addExp(float exp){
		addExp(exp, false);
	}
	public void addExp(float exp, boolean silent){
		this.exp += exp;
		checkExpLv(true);
	}
	public void checkExpLv(boolean silent){
		Log.gameplay("Checking exp lv!");
		int newlv = 0;
		for(int i = 0; i < statTable.size; i++){
			Log.gameplay("StatTable exp! " + i + " " + statTable.getKeyAt(i));
			if(statTable.getKeyAt(i) > exp){
				newlv = i - 1;
				break;
			}
		}
		Log.gameplay("Levels: " + lv + " -> " + newlv);
		for(int i = lv + 1; i <= newlv; i++){
			if(!silent)
				addDialog("You have gone up a level! (" + (lv + 1) + " -> " + (i + 1) + ")");
			setStats(i);
		}
		finishAffect(true);
	}
	public void setStats(int newlv){
		Log.gameplay("Setting a lv! " + newlv);
		/*float n = newlv < lv ? -1 : 1;
		 lv = newlv;
		 ArrayMap<Stats,Float> paststats = stats.get(baseStats);
		 ArrayMap<Stats,Float> stats = statTable.getValueAt(newlv);
		 Log.debug("Past and new stats! " + paststats + " " + stats);
		 setStats(baseStats, n * stats.get(Stats.str) + paststats.get(Stats.str), n * stats.get(Stats.intl) + paststats.get(Stats.intl), n * stats.get(Stats.def) + paststats.get(Stats.def), n * stats.get(Stats.agl) + paststats.get(Stats.agl), n * stats.get(Stats.maxhp) + paststats.get(Stats.maxhp), n * stats.get(Stats.maxmp) + paststats.get(Stats.maxmp));
		 heal(maxhp() - hp, true, true);
		 invigor(maxmp() - mp, true, true);*/
		float maxhp = maxhp(), maxmp = maxmp();
		ArrayMap<Stats,Float> stats = statTable.getValueAt(newlv);
		setStats(baseStats, stats.get(Stats.str), stats.get(Stats.intl), stats.get(Stats.def), stats.get(Stats.agl), stats.get(Stats.maxhp), stats.get(Stats.maxmp));
		heal(maxhp() - maxhp, true);
		invigor(maxmp() - maxmp, true);
		Log.gameplay("setStats", maxhp, maxhp());
	}
	public void setStatLv(float exp, float str, float intl, float def, float agl, float maxhp, float maxmp){
		Log.gameplay("Got a new statlv! " + maxhp);
		ArrayMap<Stats, Float> stats = new ArrayMap<Stats, Float>();
		stats.put(Stats.str, str);
		stats.put(Stats.intl, intl);
		stats.put(Stats.def, def);
		stats.put(Stats.agl, agl);
		stats.put(Stats.maxhp, maxhp);
		stats.put(Stats.maxmp, maxmp);
		statTable.put(exp, stats);
		checkExpLv(true);
	}
	public float expNow(){
		if(statTable.size == 0)
			return 0;

		for(int i = 0; i < statTable.size; i++)
			if(statTable.getKeyAt(i) > exp)
				return exp - (i != 0 ? statTable.getKeyAt(i - 1) : 0);

		return statTable.getKeyAt(statTable.size - 1);
	}
	public float expAtNextLv(){
		if(statTable.size == 0)
			return 0;
		for(int i = 0; i < statTable.size; i++)
			if(statTable.getKeyAt(i) > exp)
				return statTable.getKeyAt(i) - statTable.getKeyAt(i - 1);
		return statTable.getKeyAt(statTable.size - 1);
	}

	/*
	 //NEED SOMETHING TO RUN W/O PRESSING ANYTHING BC THIS HURT TEXT AND GRAPHIC ARE AT DIFF TIMES
	 //ARRRRGH CAN'T USE TRIGGERACTION() BC WE'RE IN THE MIDDLE OF A TRIGGER, FAILS CHECK!
	 //Runnable is a Java class... extend it?
	 //BOI GET REKT!
	 //Hmm... don't think I need the act list to be SeqActs, just RunActs
	 //Wait, I already have a class for this! The Item class, which used to be BoolRun. Going back to that then
	 //Ended up subclassing BoolRun to Item anyway
	 //Almost there
	 //So when superfunc "triggers", just adds to queue, doesn't run thus doesn't get thru
	 //... I could rebuild my triggering system to not use actions and fire rn. Risky, but worth a shot
	 //THAT WORKED. Kinda. Still doing it. But it's not trash! :D
	 //GET. REKT. BOI. I'VE DONE IT BY REWRITING THE ACTIONS TO NOT USE THEM
	 //*really far in the future* wow what is this. i dont even use actions anymore, just run scripts.
	 //okay so i /might/ have rewritten actions again, but its still mostly just run on the fly
	 //News flash: rewriting actions again to run simultaneously for camera movements (not even doing anything with enemies, why is this here)
	 //News flash 2: not doing that, just adding more generalized actions. might still be a good idea tho.
	 //Most of this convo is invalid, not using this turn-based thing anymore. Keeping this for postarity or whatever
	 */

	@Override
	public boolean die(){
		if(!super.die()){
			Log.debug("Dead.");
			collide = false;
			addAction(new Action(){public void run(){
						game.loadSave();
					}});
		}
		return true;
	}

	@Override
	public void addItem(GameItem item){
		super.addItem(item);
		EntityBox.addItemToBox(item, buttonBox.items.box, game);
	}
	@Override
	public void removeItem(GameItem item){
		super.removeItem(item);
		buttonBox.items.box.removeOption(item.option);
		removeStats(item.type);
		equippedItems.removeKey(item.type);
	}
	
	private Rectangle camRect = new Rectangle(), dz = new Rectangle(), dzr = new Rectangle();
	public void moveCamera(){
		//Log.debug("Moving camera! currentAction", currentAction);
		OrthographicCamera cam = game.getCamera();
		if(firstFrame && !game.map.hasCameraInit){
			cam.position.x = getX(Align.center);
			cam.position.y = getY(Align.center);
		}
		camRect.set(cam.position.x - game.screenRect.width * cam.zoom / 2, cam.position.y - game.screenRect.height * cam.zoom / 2, game.screenRect.width * cam.zoom, game.screenRect.height * cam.zoom);
		dz.set(activeDeadzone.x * cam.zoom, activeDeadzone.y * cam.zoom, activeDeadzone.width * cam.zoom, activeDeadzone.height * cam.zoom);
		Log.graphics("CamRect:", camRect, "Cam:", cam);

		float dzx = (dz.width / 2 - getTrueWidth() / 2) * deadzone, dzy = (dz.height / 2 - getTrueHeight() / 2) * deadzone;
		dzr.set(dz.x + dzx, dz.y + dzy, dz.width - dzx * 2, dz.height - dzy * 2);
		camRect.x = MathUtils.clamp(camRect.x, getX() + getTrueWidth()  - (dzr.x + dzr.width),  getX() - dzr.x);
		camRect.y = MathUtils.clamp(camRect.y, getY() + getTrueHeight() - (dzr.y + dzr.height), getY() - dzr.y);
		Log.graphics("Deadzone:", deadzone, "DZ:", dz, "DZR:", dzr, "CamRect:", camRect, "Player:", toRect());

		cam.position.x = camRect.x + camRect.width / 2;
		cam.position.y = camRect.y + camRect.height / 2;

		lastCameraPos.set(cam.position.x, cam.position.y, cam.zoom);
		cam.update();
	}
	
	public void updateMapRect(){
		Vector2 mapCoords = new Vector2();
		mapRect.localToStageCoordinates(mapCoords);
		dzRect = new Rectangle(Math.max(mapCoords.x - game.screenRect.getX(), 0), Math.max(mapCoords.y - game.screenRect.getY(), 0),
							   mapRect.getWidth(), mapRect.getHeight());
		bigdzRect = new Rectangle(0, 0, game.screenRect.getWidth(), game.screenRect.getHeight());
		Log.graphics("MapRect!", dzRect, bigdzRect);

		if(firstFrame){
			game.getCamera().zoom = Tenebrae.TILES / dzRect.getWidth();
			game.updateZoom();
			Log.graphics("Zoom", game.zoom, Tenebrae.TILES, dzRect.getWidth(), getWidth());
		}else{
			game.getCamera().zoom = game.zoom;
		}
		
		if(game.map != null)
			setExpanded(isExpanded());
	}

	public boolean canMove(){
		return buttonBox.getActiveBox() == null && !hasAnyAction();
	}
	public void handleMovementControls(float delta){
		if(!canMove())
			return;

		float dist = JOYSPEED*speedMult*delta;
		float x = 0, y = 0;
		if(Gdx.input.isKeyPressed(Input.Keys.RIGHT))
			x += dist;
		if(Gdx.input.isKeyPressed(Input.Keys.LEFT))
			x -= dist;
		if(Gdx.input.isKeyPressed(Input.Keys.UP))
			y += dist;
		if(Gdx.input.isKeyPressed(Input.Keys.DOWN))
			y -= dist;
		if(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)){
			x *= SPRINTMULT;
			y *= SPRINTMULT;
		}
		
		if(x != 0 && y != 0){
			float sq = (float)Math.sqrt(2);
			x /= sq;
			y /= sq;
		}
		
		if(x != 0 || y != 0)
			move(x, y, 0, true, true);
	}
	
	@Override
	public void moveToTarget(){
		if(hasTarget){
			float adx = (targetX - getX()) / actstep, ady = (targetY - getY()) / actstep;
			for(int i = 0; i < actstep; i++){
				float px = getX(), py = getY();
				moveBy(adx, 0);
				if(isColliding() && currentAction == null)
					setX(px);
				moveBy(0, ady);
				if(isColliding() && currentAction == null)
					setY(py);
			}
			hasTarget = false;
		}
	}

	@Override
	public void act(float delta){
		handleMovementControls(delta);
		
		boolean moved = hasTarget; // back here bc triggerAction() kills them and moves us
		super.act(delta);

		if(moved || firstFrame){
			/*float rand = (float)Math.random();
			 float chance = speed * 1f;
			 if(rand < chance){
			 WScreen.b = 1f;//VISUAL NOTICE FOR DEBUGGING
			 Vector2 cellvec = getClosestCell();
			 Enemy enemy = map.getTileEnemy(cellvec.x, cellvec.y);
			 //Log.debug("Battle! " + rand + ", " + chance+", "+enemy);
			 if(enemy != null){
			 encounter(enemy);
			 }
			 }*/

			Array<Trigger> triggersIn = getCollidingEnteranceObjects();
			Log.gameplay("In:", triggersIn.size == 0 ? 0 : triggersIn.get(0), "Out:", ptriggers == null ? null : ptriggers.size == 0 ? 0 : ptriggers.get(0));
			for(Trigger trig : triggersIn){
				Log.gameplay("Found an enter/exit object inside!", trig);
				if((ptriggers == null || !ptriggers.contains(trig, false)) && trig.hasProperty("onEnter")){
					Log.gameplay("It was an enter object!");
					trig.trigger("onEnter");
				}
			}
			if(!firstFrame && ptriggers != null)
				for(Trigger trig : ptriggers){
					Log.gameplay("Found an enter/exit object outside!", trig);
					if(!triggersIn.contains(trig, false) && trig.hasProperty("onExit")){
						Log.gameplay("It was an exit object!");
						trig.trigger("onExit");
					}
				}
			ptriggers = triggersIn;
		}
		
		if(firstFrame){
			px = getX();
			py = getY();
			firstFrame = false;
		}
	}

	public void endSelf(){
		dispose();
		table.clear();
	}

	public class PlayerLib extends TwoArgFunction{
		@Override
		public LuaValue call(LuaValue modname, LuaValue env){
			LuaTable library = tableOf();

			library.set("setStatLv", new VarArgFunction(){ // self, exp, str, intl, def, agl, maxhp, maxmp
					@Override
					public Varargs invoke(Varargs args){
						setStatLv((float)args.checkdouble(2), (float)args.checkdouble(3), (float)args.checkdouble(4), (float)args.checkdouble(5), (float)args.checkdouble(6), (float)args.checkdouble(7), (float)args.checkdouble(8));
						return NONE;
					}
				});
			library.setmetatable(tableOf());
			library.getmetatable().set(INDEX, new TwoArgFunction(){
					@Override
					public LuaValue call(LuaValue self, LuaValue key){
						//Log.debug("Getting", key.checkjstring());
						switch(key.checkjstring()){
							case "name":
								return valueOf(trueName);
							case "speed":
								return valueOf(speedMult);
							case "deadzone":
								return valueOf(deadzone);
							default:
								return NIL;
						}
					}
				});
			library.getmetatable().set(NEWINDEX, new ThreeArgFunction(){
					@Override
					public LuaValue call(LuaValue self, LuaValue key, LuaValue value){
						//Log.debug("Setting", key.checkjstring());
						switch(key.checkjstring()){
							case "name":
								setPlayerName(value.checkjstring());
								break;
							case "speed":
								speedMult = (float)value.checkdouble();
								break;
							case "deadzone":
								deadzone = (float)value.checkdouble();
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
