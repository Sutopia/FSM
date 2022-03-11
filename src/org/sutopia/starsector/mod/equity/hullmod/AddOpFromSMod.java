package org.sutopia.starsector.mod.equity.hullmod;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.sutopia.starsector.mod.concord.Codex;
import org.sutopia.starsector.mod.concord.api.SelectiveTransientHullmod;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.util.Misc;

public final class AddOpFromSMod extends BaseHullMod implements SelectiveTransientHullmod {
    
    public static final float COMPENSATION_MULT = 0.75f;
    public static final float COMPENSATION_MULT_REDUCE_FOR_BDSM = 0.5f;
    public static final float COMPENSTATION_MULT_REDUCE_FOR_PSMOD = 0.5f;
    
    public static float getCompensationMult() {
        float mult = COMPENSATION_MULT;
        if (Global.getSettings().getModManager().isModEnabled("better_deserving_smods")) {
            mult -= COMPENSATION_MULT_REDUCE_FOR_BDSM;
        }
        if (Global.getSettings().getModManager().isModEnabled("progressiveSMods")) {
            mult -= COMPENSTATION_MULT_REDUCE_FOR_PSMOD;
        }
        return Math.max(0f, mult);
    }
    
    @SuppressWarnings("serial")
    public static final Map<HullSize,Integer> TARGET_OP = new HashMap<HullSize,Integer>() {{
        put(HullSize.FRIGATE, 5);
        put(HullSize.DESTROYER, 10);
        put(HullSize.CRUISER, 20);
        put(HullSize.CAPITAL_SHIP, 30);
    }};

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        float compensation = 0f;
        for (String smod : stats.getVariant().getSMods()) {
            int cost = Global.getSettings().getHullModSpec(smod).getCostFor(hullSize);
            int target = TARGET_OP.get(hullSize);
            if (cost < target) {
                compensation += target - cost;
            }
        }
        compensation *= getCompensationMult();
        
        stats.getDynamic().getMod(Codex.ORDNANCE_POINT_MOD).modifyFlat(id, (int) Math.floor(compensation));
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        switch (index) {
        case 4:
            return String.format("%d%%", Math.round(getCompensationMult() * 100f));
        case 0:
            return String.format("%d", TARGET_OP.get(HullSize.FRIGATE));
        case 1:
            return String.format("%d", TARGET_OP.get(HullSize.DESTROYER));
        case 2:
            return String.format("%d", TARGET_OP.get(HullSize.CRUISER));
        case 3:
            return String.format("%d", TARGET_OP.get(HullSize.CAPITAL_SHIP));
        }
        return null;
    }

    @Override
    public Color getNameColor() {
        return Misc.getHighlightedOptionColor();
    }

    @Override
    public boolean shouldApplyToSpec(ShipHullSpecAPI spec) {
        return false;
    }

    @Override
    public boolean shouldApplyToVariant(ShipVariantAPI variant) {
        return variant.getSMods().size() > 0 && getCompensationMult() > 0;
    }
}
