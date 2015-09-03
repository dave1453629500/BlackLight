/* 
 * Copyright (C) 2015 Peter Cai
 *
 * This file is part of BlackLight
 *
 * BlackLight is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BlackLight is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BlackLight.  If not, see <http://www.gnu.org/licenses/>.
 */

package info.papdt.blacklight.support;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

import info.papdt.blacklight.R;
import static info.papdt.blacklight.BuildConfig.DEBUG;

public class LongPostUtility {
	private static final String TAG = LongPostUtility.class.getSimpleName();
	
	private static final int PADDING = 40;
	
	private static final int TYPE_BOLD = 0,
							TYPE_ITALIC = 1,
							TYPE_DELETED = 2,
							TYPE_INDENT = 3,
							TYPE_TITLE = 4;

	public static Bitmap parseLongPost(Context context, String text, Bitmap pic) {
		if (DEBUG) {
			Log.d(TAG, "parseLongPost");
			Log.d(TAG, "text = " + text);
		}
		
		// Generated By BlackLight
		// By adding this, we can skip many overflow checks.
		String from = context.getResources().getString(R.string.long_from);
		text += "\n\n" + from;
		
		// Get width and height
		int width = 720;
		int height = -1; // We will calculate this later
		int picWidth = width - 20, picHeight = 0; // We will calculate this later
		int textWidth = width - PADDING * 2; // For padding

		// Create the paint first to measue text
		TextPaint paint = new TextPaint();
		paint.setAntiAlias(true);
		paint.setTextSize(20.0f);

		// Parse the tags and trip the string
		ArrayList<HashMap<String, Integer>> format = new ArrayList<HashMap<String, Integer>>();
		String tmp = text;

		String stripped = "";

		boolean ignore = false;
		boolean indent = false;
		boolean title = false;
		
		int rank = 1;

		while (tmp.length() > 0) {
			String str = tmp.substring(0, 1);

			// The escape character is "\"
			if (str.equals("\\") && !ignore) {
				// \*This is not Italic text \*
				tmp = tmp.substring(1, tmp.length());
				ignore = true;
				continue;
			}

			// Simple text formatting
			// Thanks to Markdown
			if (str.equals("_") && tmp.length() > 1 && tmp.substring(1, 2).equals("_") && !ignore) {
				// __This is bold text__
				tmp = tmp.substring(2, tmp.length());
				HashMap<String, Integer> map = new HashMap<String, Integer>();
				map.put("pos", stripped.length());
				map.put("type", TYPE_BOLD);
				format.add(map);
				continue;
			} else if (str.equals("*") && !ignore) {
				// *This is Italic text*
				tmp = tmp.substring(1, tmp.length());
				HashMap<String, Integer> map = new HashMap<String, Integer>();
				map.put("pos", stripped.length());
				map.put("type", TYPE_ITALIC);
				format.add(map);
				continue;
			} else if (str.equals("~") && tmp.length() > 1 && tmp.substring(1, 2).equals("~") && !ignore) {
				// ~~This is deleted text~~
				tmp = tmp.substring(2, tmp.length());
				HashMap<String, Integer> map = new HashMap<String, Integer>();
				map.put("pos", stripped.length());
				map.put("type", TYPE_DELETED);
				format.add(map);
				continue;
			} else if (str.equals("[") && tmp.length() > 1 && !ignore) {
				// Inspired from shell's coloring
				// [rRed Text[d
				// [gGreen Text[d
				// [bBlue Text[d
				// [yYellow Text[d
				// [cCyan Text[d
				// [mMagenta Text[d
				// [dDefault Color[d
				String color = tmp.substring(1, 2);
				int type = Integer.MIN_VALUE;
				if (color.equals("r")) {
					type = Color.RED;
				} else if (color.equals("g")) {
					type = Color.GREEN;
				} else if (color.equals("b")) {
					type = Color.BLUE;
				} else if (color.equals("y")) {
					type = Color.YELLOW;
				} else if (color.equals("c")) {
					type = Color.CYAN;
				} else if (color.equals("m")) {
					type = Color.MAGENTA;
				} else if (color.equals("d")) {
					type = -1;
				} else if (color.equals("#")) {
					color = tmp.substring(1, 8);
					type = Color.parseColor(color);
				}

				if (type > Integer.MIN_VALUE) {
					HashMap<String, Integer> map = new HashMap<String, Integer>();
					map.put("pos", stripped.length());
					map.put("type", type);
					format.add(map);
					tmp = tmp.substring(color.length() + 1, tmp.length());
					continue;
				}
			} else if (str.equals("\n")) {
				// We have much to do with line breaks
				String c = tmp.substring(1, 2);
				
				if (DEBUG) {
					LogF.d(TAG, "character after break: %s", c);
				}
				
				// Indent
				if (c.equals(">") || c.equals("-") || (isNumber(c) && tmp.substring(2, 3).equals("."))) {
					stripped += str;
					if (!indent) {
						if (DEBUG) {
							Log.d(TAG, "indent!");
						}
						
						// Indent quotes
						HashMap<String, Integer> map = new HashMap<String, Integer>();
						map.put("pos", stripped.length());
						map.put("type", TYPE_INDENT);
						format.add(map);
						indent = true;
					}
					
					if (!isNumber(c)) {
						if (tmp.substring(2, 3).equals(" ")) {
							tmp = tmp.substring(3, tmp.length());
						} else {
							tmp = tmp.substring(2, tmp.length());
						}
						
						if (c.equals("-")) {
							// Unsorted list
							stripped += "・ ";
						}
					} else {
						stripped += rank + ". ";
						if (tmp.substring(3, 4).equals(" ")) {
							tmp = tmp.substring(4, tmp.length());
						} else {
							tmp = tmp.substring(3, tmp.length());
						}
						rank += 1;
					}
					continue;
				} else if (indent) {
					// Unindent quotes
					HashMap<String, Integer> map = new HashMap<String, Integer>();
					map.put("pos", stripped.length());
					map.put("type", TYPE_INDENT);
					format.add(map);
					indent = false;
					rank = 1;
				}
				
				// Title
				if (c.equals("#")) {
					stripped += str;
					if (!title) {
						HashMap<String, Integer> map = new HashMap<String, Integer>();
						map.put("pos", stripped.length());
						map.put("type", TYPE_TITLE);
						format.add(map);
						title = true;
					}
					
					if (tmp.substring(2, 3).equals(" ")) {
						tmp = tmp.substring(3, tmp.length());
					} else {
						tmp = tmp.substring(2, tmp.length());
					}
					
					continue;
				} else if (title) {
					HashMap<String, Integer> map = new HashMap<String, Integer>();
					map.put("pos", stripped.length() + 1);
					map.put("type", TYPE_TITLE);
					format.add(map);
					title = false;
				}
			}

			ignore = false;

			stripped += str;
			tmp = tmp.substring(1, tmp.length());
		}
		
		if (DEBUG) {
			Log.d(TAG, "text = " + stripped);
		}
		
		// Build the layout
		StaticLayout layout = new StaticLayout(stripped, paint, textWidth,
				Layout.Alignment.ALIGN_NORMAL, 1.0f, 1.0f, false);

		// Calculate height
		height = layout.getLineTop(layout.getLineCount()) + PADDING * 2;

		if (pic != null) {
			picHeight = (int) (((float) picWidth / (float) pic.getWidth()) * pic.getHeight());
			height += picHeight + 20;

			if (DEBUG) {
				Log.d(TAG, "picHeight = " + picHeight + "; height = " + height
					  + "; pic.getHeight() = " + pic.getHeight());
				Log.d(TAG, "picWidth = " + picWidth + "; pic.getWidth() = " + pic.getWidth());
			}
		}

		// Create the bitmap and draw
		Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bmp);

		paint.setColor(context.getResources().getColor(android.R.color.background_light));
		canvas.drawRect(0, 0, width, height, paint);

		int defColor = context.getResources().getColor(R.color.black);
		paint.setColor(defColor);
		
		int indentStart = -1;
		boolean isTitle = false;
		
		for (int i = 0; i < layout.getLineCount(); i++) {
			float y = PADDING + layout.getLineTop(i);
			float x = PADDING;
			
			if (indentStart > -1) {
				x += PADDING / 2;
			}
			
			if (DEBUG) {
				Log.d(TAG, "i = " + i + "; x = " + x + "; y = " + y);
			}
			
			int lastPos = layout.getLineStart(i);
			int max = layout.getLineEnd(i);
			
			// The last line is the generated-by string.
			if (i == layout.getLineCount() - 1) {
				paint.setColor(context.getResources().getColor(R.color.gray));
			}
			
			while (format.size() > 0) {
				HashMap<String, Integer> f = format.get(0);
				int pos = f.get("pos");
				int type = f.get("type");
				
				// If we have gone to the last character of this line
				// Just finish this line.
				if (pos >= max) {
					break;
				}
				
				String str = stripped.substring(lastPos, pos);
				canvas.drawText(str, x, y, paint);
				x += paint.measureText(str);
				lastPos = pos;
				
				switch (type) {
					case TYPE_TITLE:
						isTitle = !isTitle;
						
						if (DEBUG && isTitle) {
							LogF.d(TAG, "line %d is title", i);
						}
						
					case TYPE_BOLD:
						paint.setFakeBoldText(!paint.isFakeBoldText());
						break;
					case TYPE_ITALIC:
						if (paint.getTextSkewX() >= 0.0f)
							paint.setTextSkewX(-0.25f);
						else
							paint.setTextSkewX(0.0f);
						break;
					case TYPE_DELETED:
						paint.setStrikeThruText(!paint.isStrikeThruText());
						break;
					case TYPE_INDENT:
						if (indentStart == -1) {
							indentStart = i;
							x += PADDING / 2;
						} else {
							
							if (DEBUG) {
								LogF.d(TAG, "indentStart = %d; indentEnd = %d", indentStart, i + 1);
							}
							
							// Draw a indent line
							int color = paint.getColor();
							paint.setColor(context.getResources().getColor(R.color.gray_alpha));
							canvas.drawRect(PADDING * 1.2f,
								layout.getLineTop(indentStart) - layout.getLineAscent(indentStart) + PADDING, PADDING * 1.2f + 2,
								layout.getLineTop(i) - layout.getLineDescent(i) + PADDING, paint);
							paint.setColor(color);
							
							indentStart = -1;
							x -= PADDING / 2;
						}
						break;
					case -1:
						paint.setColor(defColor);
						break;
					default:
						paint.setColor(type);
						break;
				}
				
				format.remove(0);
			}
			
			// Not drawn? Just draw it!
			if (lastPos < max) {
				String str = stripped.substring(lastPos, max);
				canvas.drawText(str, x, y, paint);
				x += paint.measureText(str);
			}
			
			if (isTitle) {
				// Draw underline for title
				if (DEBUG) {
					LogF.d(TAG, "i = %d, x = %f, width = %f", i, x, x - PADDING);
				}
				
				int color = paint.getColor();
				paint.setColor(context.getResources().getColor(R.color.gray_alpha));
				canvas.drawRect(PADDING,
					layout.getLineTop(i) + PADDING + 4, x - PADDING / 2,
					layout.getLineTop(i) + PADDING + 6, paint);
				paint.setColor(color);
			}
			
		}

		// Draw the picture
		if (pic != null) {
			int y = layout.getLineTop(layout.getLineCount());
			canvas.drawBitmap(pic, new Rect(0, 0, pic.getWidth(), pic.getHeight()),
							  new Rect(10, y + 10, picWidth + 10, picHeight + y + 10), paint);
		}

		// Finished, return
		return bmp;
	}

	public static String parseLongContent(Context context, String content) {
		if (DEBUG) {
			Log.d(TAG, "parseLongContent");
		}

		String[] strs = content.split("\n");
		String str = "";

		if (strs.length > 0) {
			str = strs[0];
		}

		if (str.length() < 140) {
			if (TextUtils.isEmpty(str)) {
				str = context.getResources().getString(R.string.long_post);
			}

			return str;
		} else {
			return str.substring(0, 137) + "...";
		}
	}
	
	private static boolean isNumber(String s) {
		char c = s.charAt(0);
		return c > '0' && c < '9';
	}

}
