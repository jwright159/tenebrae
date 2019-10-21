package com.github.jwright159.tenebrae;

import com.github.jwright159.gdx.*;
import com.github.jwright159.gdx.actor.*;
import com.github.jwright159.tenebrae.EntityBox.*;
import com.github.jwright159.tenebrae.MenuItem.*;
import com.github.jwright159.tenebrae.Action.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.maps.*;
import com.badlogic.gdx.maps.objects.*;
import com.badlogic.gdx.maps.tiled.objects.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.maps.tiled.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.*;
import com.badlogic.gdx.graphics.glutils.*;
import com.badlogic.gdx.*;

public class Player extends Character{
	public TileMap map;
	private boolean firstFrame;// True for first frame after map load, for skipping exittriggers (just call them manually if you need them instead of auto detection)
	public float speedMult = 1;
	public int lv = -1;
	private ArrayMap<Float, ArrayMap<Stats, Float>> statTable;
	public final static int actstep = 1;
	public ButtonBox buttonBox;
	public Label dialogBox;
	private Container<Stack> mapRect;
	private Table uiTable;
	public String trueName;
	public Rectangle activeDeadzone;
	private Rectangle bounds;
	private PatchActor boundsActor;
	private Array<Trigger> ptriggers;
	public Vector3 lastCameraPos;
	public static Rectangle dzRect, bigdzRect;
	public Table table;
	public float deadzone = Tenebrae.DEADZONE_DEFAULT;
	public boolean collide = true;

	public Player(){
		super("player");
		Log.verbose("Making Player");
		lastCameraPos = new Vector3();
		Tenebrae.mp.charas.add(this);

		com.badlogic.gdx.scenes.scene2d.ui.Skin skin = Tenebrae.t.getSkin();
		table = Tenebrae.t.getTable();
		table.setDebug(Tenebrae.tableDebug);

		Stack stack = new Stack();
		Container<Table> uiRect = new Container<Table>();
		uiTable = new Table(skin);
		uiTable.setDebug(Tenebrae.tableDebug);
		uiRect.setActor(uiTable);
		uiRect.pad(Tenebrae.MARGIN).fill();
		stack.add(uiRect);
		Table dialogTable = new Table(skin);
		dialogTable.setDebug(Tenebrae.tableDebug);
		stack.add(dialogTable);
		dialogTable.setFillParent(true);
		table.add(stack).grow();

		Table buttonPane = new Table(skin);
		buttonPane.setDebug(Tenebrae.tableDebug);
		buttonPane.background("window");
		buttonPane.add(buttonBox = new ButtonBox(this, skin)).pad(Tenebrae.MARGIN).grow();
		uiTable.add(buttonPane).grow().uniform();

		mapRect = new Container<Stack>().fill();
		mapRect.setDebug(Tenebrae.tableDebug);
		uiTable.add(mapRect).padLeft(Tenebrae.MARGIN).growY().width(Value.percentHeight(1f, mapRect));
		Stack menuStack = new Stack();
		mapRect.setActor(menuStack);
		menuStack.setFillParent(true);
		menuStack.add(buttonBox.menu.box);
		menuStack.add(buttonBox.items.box);

		Table playerPane = new Table(skin);
		playerPane.setDebug(Tenebrae.tableDebug);
		playerPane.background("window");
		box.remove();
		playerPane.add(box = new PlayerBox(this, skin)).pad(Tenebrae.MARGIN).grow();
		uiTable.add(playerPane).padLeft(Tenebrae.MARGIN).grow().uniform();

		StatBox prevStatBox = smolStatBox;
		prevStatBox.remove();
		Tenebrae.t.getUiStage().addActor(smolStatBox = new FadingStatBox(box.healthBar, box.manaBar, skin));
		smolStatBox.setSize(prevStatBox.getWidth(), prevStatBox.getHeight());
		
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
		dialogBoxBox.setDebug(Tenebrae.tableDebug);
		dialogBoxBox.background("window");
		dialogBoxBox.add(dialogBox).pad(Tenebrae.MARGIN).grow();
		dialogTable.row();
		dialogTable.add(dialogBoxBox).pad(Tenebrae.MARGIN).expandX().fill().height(Value.percentHeight(0.3f, dialogTable));

		/*Table zoomt = new Table();
		 Tenebrae.t.getUiStage().addActor(zoomt);
		 zoomt.setFillParent(true);
		 zoomt.setDebug(true);
		 for(int i = 0; i < 12; i++){
		 for(int j = 0; j < 16; j++)
		 zoomt.add().grow();
		 zoomt.row();
		 }
		 ((OrthographicCamera)Tenebrae.t.getUiStage().getCamera()).zoom = 0.25f;*/
		// What have we learned? zoom=1/4 means width and height are 1/4th

		table.validate();

		statTable = new ArrayMap<Float, ArrayMap<Stats, Float>>();
		setPlayerName("Player");
		name = "You";
		setStats(baseStats, 0, 0, 0, 0, 0, 0);
		exp = 0;
		g = 0;
		bounds = new Rectangle();
		boundsActor = new PatchActor(bounds, skin.getPatch("wrect"), 1);
		boundsActor.setBorderAlignment(Align.left);
		//boundsActor.setDebug(true);
		setBound(-1, -1, -1, -1);
		getGlobals().load(new PlayerLib());

		setExpanded(false);
	}
	@Override
	public void changeMap(TileMap map){
		Log.debug("Changing map!", this.map, map);
		firstFrame = true;
		if(this.map != null){
			this.map.dispose();
			this.map = null;
		}
		this.map = map;
		Tenebrae.t.getStage().addActor(map);
		map.call();
		
		boundsActor.setScale(map.tileWidth, map.tileHeight);
		if(boundsActor.getStage() == null)
			map.getStage().addActor(boundsActor);

		super.changeMap(map);
		for(Character c : Tenebrae.mp.charas)
			if(c != this)
				c.changeMap(map);

		//Log.debug("Changing player! " + toString());

		LuaValue onCreate = Tenebrae.mp.getGlobals().get("onCreate");
		if(!onCreate.isnil())
			onCreate.call();
		
		if(bounds.getWidth() > 0 && bounds.getHeight() > 0)
			setTilePosition(bounds.getWidth() / 2 - getTileWidth() / 2, bounds.getHeight() / 2 - getTileHeight() / 2);
	}

	public void setPlayerName(String name){
		trueName = name;
		box.name.setText(trueName);
		Log.debug("SetName", name, box.name.getText());
	}

	public boolean isColliding(){
		if(!collide)
			return false;
		for(int i = (int)getTileX(); i <= getTileX(Align.right); i++)
			for(int j = (int)getTileY(); j <= getTileY(Align.top); j++)
				if(isColliding(i, j))
					return true;
		/*if((bounds.getWidth() > 0 &&
			(getTileX() < bounds.getX() || bounds.getX() + bounds.getWidth() < getTileX(Align.right))) ||
			(bounds.getHeight() > 0 &&
			(getTileY() < bounds.getY() || bounds.getY() + bounds.getHeight() < getTileY(Align.top))))
			return true;*/
		return false;
	}
	public boolean isColliding(int tilex, int tiley){
		MapObjects objs = map.getCollisionObjects(tilex, tiley);
		if(objs != null){
			for(RectangleMapObject obj : objs)
				if(toTileRect().overlaps(obj.getRectangle())){
					Log.verbose2("Collision!", toTileRect(), obj.getRectangle());
					return true;
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
		return map.getCollidingObjects(toRect(), "onTrigger");
	}
	private Array<Trigger> getCollidingEnteranceObjects(){
		return map.getCollidingObjects(toRect(), "onEnter");
	}
	private Trigger getBestTrigger(){
		Array<Trigger> trigs = getCollidingTriggerObjects();
		Trigger rtn = null;
		float best = 0;
		Rectangle inter = new Rectangle();

		for(Trigger trig : trigs){
			boolean disabled = trig.getProperties().get("disabled", false, Boolean.class);
			Rectangle rect = trig.getRectangle();
			Log.verbose("Obj", rect);
			Log.verbose("Player", toRect());
			Intersector.intersectRectangles(rect, toRect(), inter);
			Log.verbose("Intersect", inter);
			//Log.verbose("Intersecting triggers! " + obj.getName() + " " + rect + " " + player + " " + inter + " " + disabled);
			//rect.set(rect.getX() * map.tilebasewidth, rect.getY() * map.tilebaseheight, rect.getWidth() * map.tilebasewidth, rect.getHeight() * map.tilebaseheight);
			Log.verbose("Rescaled", rect);

			if(inter.area() > best && !disabled){
				best = inter.area();
				rtn = trig;
			}
		}

		return rtn;
	}
	public void triggerBestTrigger(){
		Trigger trig = getBestTrigger();
		Log.verbose("Requesting trigger!", trig);
		if(trig != null){
			trig.trigger("onTrigger");
		}
	}

	public void addDialog(String dialog){
		addDialog(dialog, 0, true, false);
	}
	public void addDialog(String dialog, float delay){
		addDialog(dialog, delay, false, false);
	}
	public void addDialog(String dialog, float delay, boolean tap, boolean tapDelay){
		Log.verbose2("New dialog!", '"' + dialog + '"', delay, tap, tapDelay);
		addAction(new DialogAction(this, dialog, delay, tap, tapDelay));
	}
	@Override
	public void triggerAction(){
		triggerAction(false);
	}
	public void triggerAction(boolean touched){
		Log.verbose2("Wanting an action! Was", currentAction, "Might be", hasAction() ? getAction() : null);
		if(currentAction != null && currentAction.stop(touched)){
			delay = 0;
			currentAction = null;
		}
		if(!hasAction() || !Tenebrae.doneLoading || delay != 0 || (currentAction != null && !currentAction.stop(touched))){
			//Log.debug("..But nobody came.");
			return;
		}
		//Log.debug("Iterate!");
		currentAction = removeAction();
		if(currentAction != null){
			currentAction.run();
			Log.verbose2("Current action!", currentAction, delay, currentAction == null ? null : currentAction.manualOverride);
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
			if(buttonBox.getActiveBox() == null)
				setUiDisabled(false);
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
		Log.verbose("Checking exp lv!");
		int newlv = 0;
		for(int i = 0; i < statTable.size; i++){
			Log.verbose("StatTable exp! " + i + " " + statTable.getKeyAt(i));
			if(statTable.getKeyAt(i) > exp){
				newlv = i - 1;
				break;
			}
		}
		Log.debug("Levels: " + lv + " -> " + newlv);
		for(int i = lv + 1; i <= newlv; i++){
			if(!silent)
				addDialog("You have gone up a level! (" + (lv + 1) + " -> " + (i + 1) + ")");
			setStats(i);
		}
		finishAffect();
	}
	public void setStats(int newlv){
		Log.debug("Setting a lv! " + newlv);
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
		heal(maxhp() - maxhp, true, true);
		invigor(maxmp() - maxmp, true, true);
		Log.debug("setStats", maxhp, maxhp());
	}
	public void setStatLv(float exp, float str, float intl, float def, float agl, float maxhp, float maxmp){
		Log.verbose("Got a new statlv! " + maxhp);
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
	public void die(){
		Log.debug("Dead.");
		collide = false;
		addDialog("You have died! :(");
		addAction(new Action(){public void run(){
					Tenebrae.t.loadSave();
				}});
	}
	
	public void setBound(float x, float y, float width, float height){
		bounds.set(x, y, width, height);
		boundsActor.setBounds(x, y, width, height);
		if(width <= 0 || height <= 0)
			boundsActor.setVisible(false);
		else
			boundsActor.setVisible(true);
		clamp();
	}
	public Rectangle getBound(){
		return bounds;
	}
	public void clamp(){
		if(bounds.getWidth() > 0)
			setTileX(MathUtils.clamp(getTileX(), bounds.getX(), bounds.getX()+bounds.getWidth()-getTileWidth()));
		if(bounds.getHeight() > 0)
			setTileY(MathUtils.clamp(getTileY(), bounds.getY(), bounds.getY()+bounds.getHeight()-getTileHeight()));
	}

	@Override
	public void addItem(GameItem item){
		super.addItem(item);
		EntityBox.addItemToBox(item, buttonBox.items.box);
	}
	@Override
	public void removeItem(GameItem item){
		super.removeItem(item);
		buttonBox.items.box.removeOption(item.option);
		removeStats(item.type);
		equippedItems.removeKey(item.type);
	}

	private Rectangle camRect = new Rectangle(), dz = new Rectangle(), dzr = new Rectangle(), b = new Rectangle();
	public void moveCamera(){
		//Log.debug("Moving camera! currentAction", currentAction);
		OrthographicCamera cam = Tenebrae.t.getCamera();
		if(firstFrame){
			cam.position.x = getX(Align.center);
			cam.position.y = getY(Align.center);
		}
		camRect.set(cam.position.x - Tenebrae.screenRect.width * cam.zoom / 2, cam.position.y - Tenebrae.screenRect.height * cam.zoom / 2, Tenebrae.screenRect.width * cam.zoom, Tenebrae.screenRect.height * cam.zoom);
		dz.set(activeDeadzone.x * cam.zoom, activeDeadzone.y * cam.zoom, activeDeadzone.width * cam.zoom, activeDeadzone.height * cam.zoom);
		if(bounds.getWidth() > 0 || bounds.getHeight() > 0)
			b.set(bounds.x * map.tileWidth, bounds.y * map.tileHeight, bounds.width * map.tileWidth, bounds.height * map.tileHeight);
		Log.verbose2("CamRect:", camRect, "Cam:", cam);

		float dzx = (dz.width / 2 - getTrueWidth() / 2) * deadzone, dzy = (dz.height / 2 - getTrueHeight() / 2) * deadzone;
		dzr.set(dz.x + dzx, dz.y + dzy, dz.width - dzx * 2, dz.height - dzy * 2);
		camRect.x = MathUtils.clamp(camRect.x, getX() + getTrueWidth()  - (dzr.x + dzr.width),  getX() - dzr.x);
		camRect.y = MathUtils.clamp(camRect.y, getY() + getTrueHeight() - (dzr.y + dzr.height), getY() - dzr.y);
		Log.verbose2("Deadzone:", deadzone, "DZ:", dz, "DZR:", dzr, "B:", b, "CamRect:", camRect, "Player:", toRect());

		if(bounds.width > 0)
			if(b.width <= dzr.width)
				camRect.x = b.x + b.width / 2 - camRect.width / 2;
			else
				camRect.x = MathUtils.clamp(camRect.x, b.x - dz.x, b.x + b.width - (dz.x + dz.width));
		if(bounds.height > 0)
			if(b.height <= dzr.height)
				camRect.y = b.y + b.height / 2 - camRect.height / 2;
			else
				camRect.y = MathUtils.clamp(camRect.y, b.y - dz.y, b.y + b.height - (dz.y + dz.height));
		Log.verbose2("CamRect:", camRect);

		cam.position.x = camRect.x + camRect.width / 2;
		cam.position.y = camRect.y + camRect.height / 2;

		lastCameraPos.set(cam.position.x, cam.position.y, cam.zoom);
		cam.update();
	}

	@Override
	public void doMovement(){
		if(hasTarget()){
			float ppx = getTileX(), ppy = getTileY();
			float adx = (targetX - ppx) / actstep, ady = (targetY - ppy) / actstep;
			targetX = targetY = -1;
			for(int i = 0; i < actstep; i++){
				float px = getTileX(), py = getTileY();
				moveTileBy(adx, 0);
				if(isColliding() && currentAction == null)
					setTileX(px);
				moveTileBy(0, ady);
				if(isColliding() && currentAction == null)
					setTileY(py);
			}
			clamp();
			updateSkins(getTileX() - ppx, getTileY() - ppy);
		}else{
			updateSkins(0, 0);
		}
		
		moveSmolStatBox();
	}

	@Override
	public void act(float delta){
		boolean moved = hasTarget(); // back here bc triggerAction() kills them and moves us
		super.act(delta);

		if(dzRect == null){
			Vector2 mapCoords = new Vector2();
			mapRect.localToStageCoordinates(mapCoords);
			dzRect = new Rectangle(mapCoords.x, mapCoords.y, mapRect.getWidth(), mapRect.getHeight());
			//bigdzRect = new Rectangle(uiTable.getX(), uiTable.getY(), uiTable.getWidth(), uiTable.getHeight());
			bigdzRect = new Rectangle(0, 0, Tenebrae.screenRect.getWidth(), Tenebrae.screenRect.getHeight());
			Log.debug("MapRect!", dzRect, bigdzRect);
			setExpanded(isExpanded());
			Tenebrae.t.getCamera().zoom = map.tileHeight * Tenebrae.TILES / dzRect.getHeight();
			Tenebrae.t.updateZoom();
			Log.debug("Zoom", Tenebrae.t.zoom, map.tileHeight, Tenebrae.TILES, dzRect.getHeight());
		}

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
			Log.verbose2("In:", triggersIn.size == 0 ? 0 : triggersIn.get(0), "Out:", ptriggers == null ? null : ptriggers.size == 0 ? 0 : ptriggers.get(0));
			for(Trigger trig : triggersIn){
				Log.verbose2("Found an enter/exit object inside!", trig);
				if((ptriggers == null || !ptriggers.contains(trig, false)) && trig.hasProperty("onEnter")){
					Log.verbose2("It was an enter object!");
					trig.trigger("onEnter");
				}
			}
			if(!firstFrame && ptriggers != null)
				for(Trigger trig : ptriggers){
					Log.verbose2("Found an enter/exit object outside!", trig);
					if(!triggersIn.contains(trig, false) && trig.hasProperty("onExit")){
						Log.verbose2("It was an exit object!");
						trig.trigger("onExit");
					}
				}
			ptriggers = triggersIn;
			
			if(firstFrame || currentAction == null)
				moveCamera();
		}

		firstFrame = false;
	}

	@Override
	public void endSelf(){
		super.endSelf();
		map.dispose();
		table.clear();
	}

	public class PlayerLib extends TwoArgFunction{
		@Override
		public LuaValue call(LuaValue modname, LuaValue env){
			LuaTable library = tableOf();

			library.set("setStatLv", new VarArgFunction(){ // exp, str, intl, def, agl, maxhp, maxmp
					@Override
					public Varargs invoke(Varargs args){
						setStatLv((float)args.checkdouble(1), (float)args.checkdouble(2), (float)args.checkdouble(3), (float)args.checkdouble(4), (float)args.checkdouble(5), (float)args.checkdouble(6), (float)args.checkdouble(7));
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
						Log.debug("Setting", key.checkjstring());
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
