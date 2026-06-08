package com.mine.autoshine;

import android.content.Intent;
import android.service.quicksettings.TileService;

public class QTileService extends TileService {

	@Override
	public void onClick() {
		super.onClick();

		Intent tapIntent = new Intent();
		tapIntent.putExtra(Constants.SERVICE_INTENT_EXTRA_TAP, 0);
		tapIntent.setAction(Constants.SERVICE_INTENT_ACTION);
		sendBroadcast(tapIntent);
	}

}