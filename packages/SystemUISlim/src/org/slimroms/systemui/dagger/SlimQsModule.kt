package org.slimroms.systemui.dagger

import org.slimroms.systemui.qs.tiles.AmbientDisplayTile
import org.slimroms.systemui.qs.tiles.CaffeineTile
import org.slimroms.systemui.qs.tiles.HeadsUpTile
import org.slimroms.systemui.qs.tiles.SyncTile

import com.android.systemui.qs.tileimpl.QSTileImpl

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

@Module
interface SlimQsModule {

    @Binds
    @IntoMap
    @StringKey(CaffeineTile.TILE_SPEC)
    fun bindCaffeineTile(caffeineTile: CaffeineTile): QSTileImpl<*>

    @Binds
    @IntoMap
    @StringKey(HeadsUpTile.TILE_SPEC)
    fun bindHeadsUpTile(headsUpTile: HeadsUpTile): QSTileImpl<*>

     @Binds
     @IntoMap
     @StringKey(SyncTile.TILE_SPEC)
     fun bindSyncTile(syncTile: SyncTile): QSTileImpl<*>

    @Binds
    @IntoMap
    @StringKey(AmbientDisplayTile.TILE_SPEC)
    fun bindAmbientDisplayTile(ambientDisplayTile: AmbientDisplayTile): QSTileImpl<*>
}
