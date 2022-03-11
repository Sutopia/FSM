package org.sutopia.starsector.mod.equity.hullmod;

import org.sutopia.starsector.mod.concord.Codex;
import org.sutopia.starsector.mod.concord.api.GlobalTransientHullmod;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

public class AddOpFromSModListener extends BaseHullMod implements GlobalTransientHullmod {
    public static final String EFFECT_HULLMOD_SPEC = "su_global_s_mod_op_bonus";

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        if (stats.getVariant().getSMods().size() > 0) {
            if (!stats.getVariant().hasHullMod(EFFECT_HULLMOD_SPEC) && AddOpFromSMod.getCompensationMult() > 0) {
                stats.getVariant().addPermaMod(EFFECT_HULLMOD_SPEC, false);
            }
        } else {
            if (stats.getVariant().hasHullMod(EFFECT_HULLMOD_SPEC)) {
                stats.getVariant().removePermaMod(EFFECT_HULLMOD_SPEC);
            }
            stats.getDynamic().getMod(Codex.ORDNANCE_POINT_MOD).unmodifyFlat(EFFECT_HULLMOD_SPEC);
        }
    }

}
