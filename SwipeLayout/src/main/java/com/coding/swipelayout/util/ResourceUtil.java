package com.coding.swipelayout.util;

import android.content.Context;
import android.util.Log;

/**
 * 反射获取资源
 *
 */
public class ResourceUtil {
	
	public static int getViewId(Context paramContext,String id) {
		return getId(paramContext,"id", id);
	}

	public static int getStyle(Context paramContext,String id) {
		return getId(paramContext,"style",id);
	}

	public static int getAnimId(Context paramContext,String id) {
		return getId(paramContext,"anim", id);
	}

	public static int getLayoutId(Context paramContext,String id) {
		return getId(paramContext,"layout", id);
	}

	public static int getDrawableId(Context paramContext,String id) {
		return getId(paramContext,"drawable", id);
	}
	
	public static int getColorId(Context paramContext,String id) {
		return getId(paramContext,"color", id);
	}
	
	public static int getStringId(Context paramContext,String id) {
		return getId(paramContext,"string", id);
	}
	
	private static int getId(Context paramContext, String paramI,String paramII) {
		try {
			return paramContext.getResources().getIdentifier(paramII, paramI, paramContext.getPackageName());
		} catch (Exception localException) {
			Log.w("ResourceUtil","getId error in ResourceUtil"+localException.getMessage());
		}
		return 0;
	}
	
}
