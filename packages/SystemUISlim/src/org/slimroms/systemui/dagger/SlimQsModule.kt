package org.slimroms.systemui.dagger

import org.slimroms.systemui.qs.tiles.CaffeineTile
import org.slimroms.systemui.qs.tiles.HeadsUpTile

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
}
