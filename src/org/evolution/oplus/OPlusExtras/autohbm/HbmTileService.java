package org.evolution.oplus.OPlusExtras.autohbm;

import android.content.SharedPreferences;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import androidx.preference.PreferenceManager;

import org.evolution.oplus.OPlusExtras.R;
import org.evolution.oplus.OPlusExtras.Nodes;
import org.evolution.oplus.OPlusExtras.utils.Utils;

public class HbmTileService extends TileService {

    private static final String KEY_HBM = "hbm";
    private static final String KEY_AUTO_HBM = "auto_hbm";

    @Override
    public void onStartListening() {
        super.onStartListening();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isAutoHbmEnabled = sharedPrefs.getBoolean(KEY_AUTO_HBM, false);
        boolean isHbmEnabled = sharedPrefs.getBoolean(KEY_HBM, false);
        updateTile(isAutoHbmEnabled, isHbmEnabled);
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }

    @Override
    public void onClick() {
        super.onClick();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isAutoHbmEnabled = sharedPrefs.getBoolean(KEY_AUTO_HBM, false);

        if (isAutoHbmEnabled) {
            // Do nothing if Auto HBM is enabled
            return;
        }

        final boolean enabled = !(sharedPrefs.getBoolean(KEY_HBM, false));
        sharedPrefs.edit().putBoolean(KEY_HBM, enabled).commit();
        Utils.writeValue(Nodes.nodeHBM(this), enabled ? "1" : "0");
        
        updateTile(isAutoHbmEnabled, enabled);
    }

    private void updateTile(boolean isAutoHbmEnabled, boolean isHbmEnabled) {
        final Tile tile = getQsTile();
        if (isAutoHbmEnabled) {
            tile.setState(Tile.STATE_UNAVAILABLE);
            tile.setLabel(getString(R.string.hbm_title));
            tile.setSubtitle(getString(R.string.auto_hbm_title)); // Indicate that Auto HBM is controlling it
        } else {
            tile.setState(isHbmEnabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            tile.setLabel(getString(R.string.hbm_title));
            tile.setSubtitle(isHbmEnabled ? getString(R.string.tile_on) : getString(R.string.tile_off));
        }
        tile.updateTile();
    }
}
