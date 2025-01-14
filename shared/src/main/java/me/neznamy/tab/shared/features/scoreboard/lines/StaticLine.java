package me.neznamy.tab.shared.features.scoreboard.lines;

import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.shared.TAB;
import me.neznamy.tab.shared.features.scoreboard.Scoreboard;
import me.neznamy.tab.shared.rgb.RGBUtils;

public abstract class StaticLine extends ScoreboardLine {

	//text for 1.13+
	protected String text;
	
	//values for 1.7 clients with 16 character limit for player name
	protected String prefix1_7;
	protected String name1_7;
	protected String suffix1_7;

	//values for 1.8-1.12 clients with 40 character limit for player name
	protected String prefix;
	protected String name;
	protected String suffix;
	
	public StaticLine(Scoreboard parent, int lineNumber, String text, String forcedNameStart) {
		super(parent, lineNumber);
		this.text = TAB.getInstance().getPlaceholderManager().color(text);
		String legacy = RGBUtils.getInstance().convertRGBtoLegacy(this.text);
		//1.8+
		String[] v1_8 = splitText(forcedNameStart, legacy, 40);
		prefix = v1_8[0];
		name = v1_8[1];
		suffix = v1_8[2];
		//1.7-
		String[] v1_7 = splitText(forcedNameStart, legacy, 16);
		prefix1_7 = v1_7[0];
		name1_7 = v1_7[1];
		suffix1_7 = v1_7[2];
	}
	
	private String[] splitText(String playerNameStart, String text, int maxNameLength) {
		String prefix;
		String name;
		String suffix;
		if (text.length() <= (maxNameLength - playerNameStart.length())) {
			prefix = "";
			name = playerNameStart + text;
			suffix = "";
		} else {
			String[] prefix_other = split(text, 16);
			prefix = prefix_other[0];
			String other = prefix_other[1];
			if (playerNameStart.length() > 0) {
				other = playerNameStart + TAB.getInstance().getPlaceholderManager().getLastColors(prefix) + other;
			}
			String[] name_suffix = split(other, maxNameLength);
			name = name_suffix[0];
			suffix = name_suffix[1];
		}
		return new String[]{prefix, name, suffix};
	}
	
	@Override
	public void refresh(TabPlayer refreshed, boolean force) {
		//nothing to refresh
	}

	@Override
	public void refreshUsedPlaceholders() {
		//no placeholders
	}
}