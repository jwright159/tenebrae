package wrightway.gdx.tnb;

import wrightway.gdx.*;
import wrightway.gdx.WActor.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.scenes.scene2d.actions.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.graphics.glutils.*;
import com.badlogic.gdx.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import wrightway.gdx.tnb.Tenebrae.Action;
import wrightway.gdx.JVSValue.Function;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;

public class EntityBox extends Table{
	Image icon;
	HealthBar healthBar, manaBar;
	Label name;
	Table bars;
	Character parent;
	public EntityBox(Character parent, Skin skin){
		super(skin);
		this.parent = parent;
		this.setDebug(Tenebrae.tableDebug);
		Table pic = new Table(skin);
		pic.setDebug(Tenebrae.tableDebug);
		pic.add(icon = new Image(new LayeredTextureRegionDrawable(parent.region))).size(128, 128);
		pic.row();
		pic.add(name = new Label(parent.name, skin));
		this.add(pic).padRight(Tenebrae.margin);
		bars = new Table(skin);
		bars.setDebug(Tenebrae.tableDebug);
		bars.add(healthBar = new HealthBar("HP", skin, "health")).grow();
		bars.row();
		bars.add(manaBar = new HealthBar("MP", skin, "mana")).grow();
		this.add(bars).grow();
	}
	public void updateHP(){
		healthBar.setHealth(parent.hp, parent.maxhp());
		manaBar.setHealth(parent.mp, parent.maxmp());
	}

	@SuppressWarnings("unchecked")
	public static ArrayMap sortMap(ArrayMap map){
		Array keys = new Array();
		for(int i = 0; i < map.size; i++)
			keys.add(map.getKeyAt(i));
		keys.sort();
		ArrayMap rtn = new ArrayMap();
		for(int i = 0; i < keys.size; i++)
			rtn.put(keys.get(i), map.get(keys.get(i)));
		return rtn;
	}

	public static class PlayerBox extends EntityBox{
		LevelBar levelBar;
		PlayerBox(Player player, Skin skin){
			super(player, skin);
			name.setText(player.trueName);
			bars.row();
			bars.add(levelBar = new LevelBar(skin)).grow();
		}
		@Override
		public void updateHP(){
			Player p = (Player)parent;
			super.updateHP();
			Tenebrae.verbose("Lv pts! " + p.expNow());
			levelBar.setHealth(p.lv, p.expNow(), p.expAtNextLv());
			p.smolStatBox.updateHP();
		}
	}

	public static class SizableProgressBar extends ProgressBar{
		private boolean round;
		private float position;
		public SizableProgressBar(float min, float max, float step, boolean vertical, Skin skin){
			super(min, max, step, vertical, skin);
		}
		@Override
		public float getPrefWidth(){
			return 0;
		}
		@Override
		public float getPrefHeight(){
			return 0;
		}
		@Override
		public void setRound(boolean round){
			super.setRound(round);
			this.round = round;
		}
		@Override
		protected float getKnobPosition(){
			return position;
		}
		@Override
		public void draw(Batch batch, float parentAlpha){
			ProgressBarStyle style = getStyle();
			boolean disabled = isDisabled();
			final Drawable knob = getKnobDrawable();
			final Drawable bg = (disabled && style.disabledBackground != null) ? style.disabledBackground : style.background;
			final Drawable knobBefore = (disabled && style.disabledKnobBefore != null) ? style.disabledKnobBefore : style.knobBefore;
			final Drawable knobAfter = (disabled && style.disabledKnobAfter != null) ? style.disabledKnobAfter : style.knobAfter;

			Color color = getColor();
			float x = getX();
			float y = getY();
			float width = getWidth();
			float height = getHeight();
			float knobHeight = knob == null ? 0 : knob.getMinHeight();
			float knobWidth = knob == null ? 0 : knob.getMinWidth();
			float percent = getVisualPercent();

			batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);

			if(isVertical()){
				float positionHeight = height;

				float bgTopHeight = 0, bgBottomHeight = 0;
				if(bg != null){
					if(round)
						bg.draw(batch, Math.round(x + (width - bg.getMinWidth()) * 0.5f), y, Math.round(bg.getMinWidth()), height);
					else
						bg.draw(batch, x + width - bg.getMinWidth() * 0.5f, y, bg.getMinWidth(), height);
					bgTopHeight = bg.getTopHeight();
					bgBottomHeight = bg.getBottomHeight();
					positionHeight -= bgTopHeight + bgBottomHeight;
				}

				float knobHeightHalf = 0;
				if(knob == null){
					knobHeightHalf = knobBefore == null ? 0 : knobBefore.getMinHeight() * 0.5f;
					position = (positionHeight - knobHeightHalf) * percent;
					position = Math.min(positionHeight - knobHeightHalf, position);
				}else{
					knobHeightHalf = knobHeight * 0.5f;
					position = (positionHeight - knobHeight) * percent;
					position = Math.min(positionHeight - knobHeight, position) + bgBottomHeight;
				}
				position = Math.max(Math.min(0, bgBottomHeight), position);

				if(knobBefore != null){
					if(round){
						knobBefore.draw(batch, Math.round(x + (width - knobBefore.getMinWidth()) * 0.5f), Math.round(y + bgTopHeight),
							Math.round(knobBefore.getMinWidth()), Math.round(position + knobHeightHalf));
					}else{
						knobBefore.draw(batch, x + (width - knobBefore.getMinWidth()) * 0.5f, y + bgTopHeight, knobBefore.getMinWidth(),
							position + knobHeightHalf);
					}
				}
				if(knobAfter != null){
					if(round){
						knobAfter.draw(batch, Math.round(x + (width - knobAfter.getMinWidth()) * 0.5f),
							Math.round(y + position + knobHeightHalf), Math.round(knobAfter.getMinWidth()),
							Math.round(height - position - knobHeightHalf));
					}else{
						knobAfter.draw(batch, x + (width - knobAfter.getMinWidth()) * 0.5f, y + position + knobHeightHalf,
							knobAfter.getMinWidth(), height - position - knobHeightHalf);
					}
				}
				if(knob != null){
					if(round){
						knob.draw(batch, Math.round(x + (width - knobWidth) * 0.5f), Math.round(y + position), Math.round(knobWidth),
							Math.round(knobHeight));
					}else
						knob.draw(batch, x + (width - knobWidth) * 0.5f, y + position, knobWidth, knobHeight);
				}
			}else{
				float positionWidth = width;

				float bgLeftWidth = 0, bgRightWidth = 0;
				if(bg != null){
					bg.draw(batch, x, y, width, height);
					bgLeftWidth = bg.getLeftWidth();
					bgRightWidth = bg.getRightWidth();
					positionWidth -= bgLeftWidth + bgRightWidth;
				}

				float knobWidthHalf = 0;
				if(knob == null){
					knobWidthHalf = knobBefore == null ? 0 : knobBefore.getMinWidth() * 0.5f;
					position = (positionWidth - knobWidthHalf) * percent;
					position = Math.min(positionWidth - knobWidthHalf, position);
				}else{
					knobWidthHalf = knobWidth * 0.5f;
					position = (positionWidth - knobWidth) * percent;
					position = Math.min(positionWidth - knobWidth, position) + bgLeftWidth;
				}
				position = Math.max(Math.min(0, bgLeftWidth), position);

				if(knobBefore != null){
					knobBefore.draw(batch, x + bgLeftWidth, y, position + knobWidthHalf, height);
				}
				if(knobAfter != null){
					knobAfter.draw(batch, x + position + knobWidthHalf, y, width - position - knobWidthHalf, height);
				}
				if(knob != null){
					knob.draw(batch, x + position, y, knobWidth, height);
				}
			}
		}
	}

	public static class TextBar extends Stack{
		ProgressBar bar;
		Label text;
		TextBar(String text, float min, float max, float step, Skin skin, String skinName){
			this(text, min, max, step, skin);
			if(skinName != null && skin.has(skinName, ProgressBar.ProgressBarStyle.class)){
				bar.setStyle(skin.get(skinName, ProgressBar.ProgressBarStyle.class));
				//if(bar.getStyle().knobBefore instanceof TextureRegionDrawable)
				//bar.getStyle().knobBefore = new TiledDrawable((TextureRegionDrawable)bar.getStyle().knobBefore);
			}
		}
		TextBar(String text, float min, float max, float step, Skin skin){
			setDebug(Tenebrae.tableDebug);
			this.add(bar = new SizableProgressBar(min, max, step, false, skin));
			this.add(new Container<Label>(this.text = new Label(text, skin)).padLeft(Tenebrae.margin / 2f).padRight(Tenebrae.margin / 2f).fill());
			this.text.setAlignment(Align.left);
		}
	}
	public static class HealthBar extends TextBar{
		String name;
		float value, max;//cant change max in bar
		HealthBar(String name, Skin skin, String skinName){
			super(name, 0, 1, 0.01f, skin, skinName);
			this.name = name;
		}
		public void setHealth(float value, float max){
			this.value = value;
			this.max = max;
			bar.setValue(value / max);
			text.setText(toString());
		}
		@Override
		public String toString(){//bc why not
			if(!bar.isVertical())
				return name + ": " + f(value) + "/" + f(max);
			else
				return name + "\n" + f(value) + "\n--\n" + f(max);
		}
		public static String f(float val){
			return String.format("%.0f", val);
		}
	}
	public static class LevelBar extends HealthBar{
		int lv;
		LevelBar(Skin skin){
			super("LV", skin, "level");
		}
		public void setHealth(int lv, float value, float max){
			this.lv = lv;
			setHealth(value, max);
		}
		@Override
		public String toString(){
			if(!bar.isVertical())
				return "LV: " + (lv + 1) + "; EXP: " + f(value) + "/" + f(max);
			else
				return "LV\n" + (lv + 1) + "\n\nEXP" + f(value) + "\n--\n" + f(max);
		}
	}
	public static class MiniHealthBar extends HealthBar{
		HealthBar hbar;
		MiniHealthBar(HealthBar bar, Skin skin){
			super("", skin, skin.find(bar.bar.getStyle()));
			this.hbar = bar;
			this.text.remove();
		}
		public void setHealth(){
			setHealth(hbar.value, hbar.max);
		}
	}

	public static class MenuOption extends Button implements Comparable<MenuOption>{//Not TextButton bc the padding widens the button
		MenuBox box;
		Label text;
		MenuOption(String textStr, MenuBox box, int align, boolean enabled, Skin skin){
			super(skin);
			this.box = box;
			if(box != null)
				box.setVisible(false);
			text = new Label(textStr, skin);
			text.setAlignment(align);
			text.setWrap(true);
			setClip(true);
			addActor(text);
			//text.setEllipsis(true);
			setDisabled(!enabled);
			addListener(new ChangeListener(){
					@Override
					public void changed(ChangeEvent event, Actor a){
						open();
					}
				});
		}
		MenuOption(String text, MenuBox box, Skin skin){
			this(text, box, Align.topLeft, true, skin);
		}
		public boolean isEmpty(){
			Tenebrae.verbose2("Is " + this + " empty? " + (box != null ? box.list.size : -1));
			if(box == null)
				return false;
			for(MenuOption opt : box.list)
				if(opt.isOpt())
					return false;
			return true;
		}
		public boolean isOpt(){
			return !(MenuBox.isEmpty(this) || MenuBox.isNextPage(this) || MenuBox.isPrevPage(this) || isEmpty());
		}
		public void open(){
			if(box != null)
				box.setVisible(true);
		}
		public String getText(){
			return text.getText().toString();
		}
		@Override
		public float getPrefWidth(){
			return 0;
		}
		@Override
		public float getPrefHeight(){
			return 0;
		}
		@Override
		public void layout(){
			super.layout();
			Drawable bg = getBackground();
			text.setBounds(
				bg.getLeftWidth()+Tenebrae.margin*0.5f,
				bg.getBottomHeight()+Tenebrae.margin*0.25f,
				getWidth()-bg.getLeftWidth()-bg.getRightWidth()-Tenebrae.margin,
				getHeight()-bg.getBottomHeight()-bg.getTopHeight()-Tenebrae.margin*0.5f
			);
		}
		@Override
		public int compareTo(MenuOption p1){
			return getText().compareToIgnoreCase(p1.getText());
		}
		@Override
		public String toString(){
			return super.toString() + "§" + getText();
		}
	}
	/*public static void clipWidth(final Actor bounds, final Container cell, float pad){
		cell.setClip(true);
		if(pad != -1)cell.padLeft(pad).padRight(pad);
		cell.width(new Value(){
				@Override
				public float get(Actor a){
					return bounds.getWidth() - cell.getPadX();
				}
			});
	}
	public static void clipHeight(final Actor bounds, final Container cell, float pad){
		cell.setClip(true);
		if(pad != -1)cell.padTop(pad).padBottom(pad);
		cell.height(new Value(){
				@Override
				public float get(Actor a){
					return bounds.getHeight() - cell.getPadY();
				}
			});
	}
	public static void clipWidth(final Actor bounds, final Cell cell, float pad){
		if(pad != -1)cell.padLeft(pad).padRight(pad);
		cell.width(new Value(){
				@Override
				public float get(Actor a){
					return bounds.getWidth() - cell.getPadX();
				}
			});
	}*/
	public static class MenuBox extends Stack{
		static float padding = 30f;
		static int itemsPerHeight = 5;
		int activePage;
		String id;
		Array<MenuOption> list;
		Skin skin;
		Array<Table> pages;
		MenuBox(String id, Skin skin){
			this.skin = skin;
			list = new Array<MenuOption>();
			pages = new Array<Table>();
			this.id = id;
		}

		public void addOption(MenuOption opt){
			Tenebrae.verbose2("Adding option " + opt + " to " + this + "!");
			if(list.contains(opt, true))
				return;
			list.add(opt);
			reorganize();
		}
		public boolean removeOption(MenuOption opt){
			boolean removed = false;
			for(int i = 0; i < list.size; i++){
				if(list.get(i) == opt || (list.get(i).box != null && list.get(i).box.removeOption(opt))){
					removed = true;
					opt.remove();
					break;
				}
			}
			if(removed)
				reorganize();
			return removed;
		}
		public void removeOptions(){
			for(MenuOption opt : list)
				opt.remove();
		}

		public void reorganize(){
			Tenebrae.verbose2("Reorganizing " + this + "!");
			if(list.size == 0)
				return;

			for(int i = 0; i < list.size; i++){
				Tenebrae.verbose2("Found " + (isPrevPage(list.get(i)) ?"prevPage": isNextPage(list.get(i)) ?"nextPage": isEmpty(list.get(i)) ?"empty": "an option") + "!");
				Tenebrae.verbose2("Found " + list.get(i) + "! " + (list.get(i).box != null ?"" + list.get(i).box.list.size: "no box"));
				if(!list.get(i).isOpt()){
					Tenebrae.verbose2("Removing " + list.get(i) + "!");
					list.removeIndex(i--).remove();
				}
			}

			sortList();
			int listLengthAfter = list.size == 0 ? itemsPerHeight * 2 : ((list.size - 1) / ((itemsPerHeight - 1) * 2) + 1) * itemsPerHeight * 2;
			Tenebrae.verbose("List Lengths! Current: " + list.size + ", After: Got " + listLengthAfter + ", expected a multiple of " + itemsPerHeight * 2 + "!");
			for(int i = 0; i < listLengthAfter; i++){
				String debug = "an option";
				if(i % (itemsPerHeight * 2) == itemsPerHeight * 2 - 2){
					list.insert(i, prevPage(i, listLengthAfter));
					debug = "prevPage";
				}else if(i % (itemsPerHeight * 2) == itemsPerHeight * 2 - 1){
					list.insert(i, nextPage(i, listLengthAfter));
					debug = "nextPage";
				}else if(i >= list.size){
					list.insert(i, empty());
					debug = "empty";
				}
				Tenebrae.verbose2("Adding " + debug + " at " + i + "! Size now! " + list.size);
			}

			activePage = 0;
			double pagefloat = (float)list.size / (float)(itemsPerHeight * 2);
			int pages = list.size == 0 ? 1 : (int)Math.ceil(pagefloat);
			Tenebrae.verbose2("Pages! " + list.size + ", " + (itemsPerHeight * 2) + ", " + pagefloat + " = " + pages);

			clearChildren();
			this.pages.clear();
			for(int i = 0; i < pages; i++){
				final Table page = new Table(skin);
				page.setDebug(Tenebrae.tableDebug);
				page.background("background");
				Drawable bg = page.getBackground();
				page.setTouchable(Touchable.enabled);
				this.add(page);
				this.pages.add(page);
				for(int j = 0; j < itemsPerHeight; j++){
					for(int k = 0; k < 2; k++){
						MenuOption opt = list.get(i * itemsPerHeight * 2 + j * 2 + k);
						final Cell c = page.add(opt).pad(
							j == 0 ? Tenebrae.margin - bg.getTopHeight() : 0,
							k == 0 ? Tenebrae.margin - bg.getLeftWidth() : 0,
							Tenebrae.margin - (j == itemsPerHeight - 1 ? bg.getBottomHeight() : 0),
							Tenebrae.margin - (k == 1 ? bg.getRightWidth() : 0)
						).grow();
						/*c.width(new Value(){
								@Override
								public float get(Actor a){
									return (page.getWidth() - Tenebrae.margin) * 0.5f - Tenebrae.margin;// (total - middle)/2 - (drawpad+optpad)
								}
							});
						c.height(new Value(){
								@Override
								public float get(Actor a){
									return (page.getHeight() - Tenebrae.margin * (itemsPerHeight + 1)) / (float)itemsPerHeight;
								}
							});*/
						if(opt.box != null)
							this.add(opt.box);
					}
					page.row();
				}
				page.setVisible(false);
			}

			changePage(activePage);
		}

		public void sortList(){
			//Tenebrae.debug("Sorting list of "+this+"!");
			if(list.size == 0)
				return;

			list.sort();
		}

		public MenuBox findActiveLeaf(){
			MenuBox active = null;
			for(MenuOption opt : list)
				if(opt.box != null && active == null)
					active = opt.box.findActiveLeaf();
			active = active == null && isVisible() ? this : active;
			//Tenebrae.debug("Finding active leaf! "+active+" from "+this);
			return active;
		}

		public static final String prevText = "<", nextText = ">", emptyText = Tenebrae.showEmpty ? "[empty]" : "";
		public MenuOption empty(){
			return new MenuOption(emptyText, null, Align.center, false, skin);
		}
		public MenuOption prevPage(int i, int size){
			return new MenuOption(prevText, null, Align.left, i != itemsPerHeight * 2 - 2, skin){
				@Override
				public void open(){
					changePage(activePage - 1);
				}
			};
		}
		public MenuOption nextPage(int i, int size){
			return new MenuOption(nextText, null, Align.right, i != size - 1, skin){
				@Override
				public void open(){
					changePage(activePage + 1);
				}
			};
		}
		public static boolean isEmpty(MenuOption opt){
			return opt.getText().equals(emptyText) && opt.box == null;
		}
		public static boolean isPrevPage(MenuOption opt){
			return opt.getText().equals(prevText) && opt.box == null;
		}
		public static boolean isNextPage(MenuOption opt){
			return opt.getText().equals(nextText) && opt.box == null;
		}

		public void changePage(int page){
			Tenebrae.verbose2("Changing page on " + this + " of size " + list.size + " from " + activePage + " to " + page + " of " + pages + "!");
			if(list.size == 0)
				return;
			if(page < 0 || page >= pages.size){
				changePage(page < 0 ? pages.size - (Math.abs(page) % pages.size) : page % pages.size);
				return;
			}
			pages.get(activePage).setVisible(false);
			activePage = page;
			pages.get(activePage).setVisible(true);
		}

		@Override
		public String toString(){
			return super.toString() + "§" + (id.isEmpty() ? "[noid]" : id);
		}
	}
	public static void addItemToBox(final MenuItem item, MenuBox box){
		MenuOption cat = null;
		for(MenuOption opt : box.list)
			if(opt.getText().equals(item.catagory)){
				cat = opt;
				break;
			}
		if(cat == null){
			MenuBox b = new MenuBox(item.catagory, box.skin);
			cat = new MenuOption(item.catagory, b, box.skin);
		}

		if(!(item instanceof MenuItem.GameItem)){
			cat.box.addOption(new MenuOption(item.name, null, box.skin){
					@Override
					public void open(){
						item.run("onUse");
					}
				});
		}else{
			MenuItem.GameItem gitem = (MenuItem.GameItem)item;
			MenuBox b = new MenuBox(gitem.name, box.skin);
			MenuOption opt = new MenuOption(gitem.name, b, box.skin);
			Tenebrae.verbose2("Putting item stuff in box!");
			while(gitem.toPutInBox.size > 0){
				Tenebrae.verbose("Got " + gitem.toPutInBox.get(0));
				opt.box.addOption(gitem.toPutInBox.removeIndex(0));
			}
			cat.box.addOption(opt);
		}

		box.addOption(cat);//down here bc box will detect as empty otherwise and get removed
	}

	public static class StatBox extends Button{
		MiniHealthBar health,mana;
		StatBox(HealthBar health, HealthBar mana, Skin skin){
			super(skin);
			setDebug(Tenebrae.tableDebug);
			//setClip(true);
			this.health = new MiniHealthBar(health, skin);
			this.mana = new MiniHealthBar(mana, skin);
			addListener(new ChangeListener(){
					@Override
					public void changed(ChangeEvent event, Actor a){
						Tenebrae.player.setExpanded(!Tenebrae.player.isExpanded());
					}
				});

			Stack stack = new Stack();
			Table bars = new Table(skin);
			bars.add(this.health).grow();
			bars.row();
			bars.add(this.mana).grow();
			stack.add(bars);
			Label label;
			stack.add(label = new Label("MENU", skin));
			label.setAlignment(Align.center);
			this.add(stack).grow();
		}

		public void updateHP(){
			health.setHealth();
			mana.setHealth();
		}
	}

	public static class ButtonBox extends Table{//holds item and menu buttons
		MenuOption menu, items;
		ButtonBox(final Player player, Skin skin){
			super(skin);

			MenuBox menuBox = new MenuBox("MenuBox", skin);
			//might want to delete these at some point
			addItemToBox(new MenuItem("Player", "Kill", killScript, player), menuBox);
			addItemToBox(new MenuItem("Player", "Heal", healScript, player), menuBox);
			addItemToBox(new MenuItem("Player", "Tire", tireScript, player), menuBox);
			addItemToBox(new MenuItem("Player", "Invigor", invigorScript, player), menuBox);
			menu = new MenuOption("Menu", menuBox, Align.center, true, skin);
			ChangeListener click = new ChangeListener(){
				@Override
				public void changed(ChangeEvent event, Actor a){
					player.setUiDisabled(true);
				}
			};
			menu.addListener(click);
			this.add(menu).grow()/*.width(new Value(){
					@Override
					public float get(Actor a){
						return (ButtonBox.this.getWidth() - Tenebrae.margin) * 0.5f;
					}
				})*/;

			MenuBox itemsBox = new MenuBox("ItemsBox", skin);
			items = new MenuOption("Items", itemsBox, Align.center, true, skin);
			items.addListener(click);
			this.add(items).padLeft(Tenebrae.margin).grow()/*.width(new Value(){
					@Override
					public float get(Actor a){
						return (ButtonBox.this.getWidth() - Tenebrae.margin) * 0.5f;
					}
				})*/;
		}
		public static final Function killScript = JVSParser.parseCode("player.affect(-player.hp,0)", null);
		public static final Function healScript = JVSParser.parseCode("player.affect(player.maxhp-player.hp,0)", null);
		public static final Function tireScript = JVSParser.parseCode("player.affect(0,-player.mp)", null);
		public static final Function invigorScript = JVSParser.parseCode("player.affect(0,player.maxmp-player.mp)", null);

		public MenuBox getActiveBox(){
			if(menu.box.isVisible())
				return menu.box;
			if(items.box.isVisible())
				return items.box;
			return null;
		}
	}
}
