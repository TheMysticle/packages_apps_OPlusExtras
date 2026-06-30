package org.evolution.oplus.OPlusExtras.autohbm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import androidx.preference.PreferenceManager;

import org.evolution.oplus.OPlusExtras.R;

public class AutoHbmTileService extends TileService {

    private static final String KEY_AUTO_HBM = "auto_hbm";

    @Override
    public void onStartListening() {
        super.onStartListening();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        updateTile(sharedPrefs.getBoolean(KEY_AUTO_HBM, false));
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }

    @Override
    public void onClick() {
        super.onClick();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean enabled = !(sharedPrefs.getBoolean(KEY_AUTO_HBM, false));
        sharedPrefs.edit().putBoolean(KEY_AUTO_HBM, enabled).commit();
        
        if (enabled) {
            startService(new Intent(this, AutoHbmService.class));
        } else {
            stopService(new Intent(this, AutoHbmService.class));
        }
        updateTile(enabled);
    }

    private void updateTile(boolean enabled) {
        final Tile tile = getQsTile();
        tile.setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel(getString(R.string.auto_hbm_title));
        tile.setSubtitle(enabled ? getString(R.string.tile_on) : getString(R.string.tile_off));
        tile.updateTile();
    }
}
