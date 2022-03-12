package org.sutopia.starsector.mod.equity.hullmod;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.sutopia.starsector.mod.concord.Codex;
import org.sutopia.starsector.mod.concord.api.SelectiveTransientHullmod;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public final class AddOpFromSMod extends BaseHullMod implements SelectiveTransientHullmod {
    
    private static JSONObject settings;
    public static float COMPENSATION_MULT;
    public static float COMPENSATION_MULT_REDUCE_FOR_BDSM;
    public static float COMPENSTATION_MULT_REDUCE_FOR_PSMOD;
    
    static 
    {
        try {
            settings = Global.getSettings().getJSONObject("fair_s_mod_settings");
            COMPENSATION_MULT = (float) settings.getDouble("base_compensation_mult");
            COMPENSATION_MULT_REDUCE_FOR_BDSM = (float) settings.getDouble("bdsm_penalty");
            COMPENSTATION_MULT_REDUCE_FOR_PSMOD = (float) settings.getDouble("psmod_penalty");
        } catch (JSONException je) {
            throw new RuntimeException(je);
        }
    }
    
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
        stats.getDynamic().getMod(Codex.ORDNANCE_POINT_MOD).modifyFlat(id, getCompensation(hullSize, stats.getVariant()));
    }
    
    private static int getCompensation(HullSize hullSize, ShipVariantAPI variant) {
        float compensation = 0f;
        for (String smod : variant.getSMods()) {
            int cost = Global.getSettings().getHullModSpec(smod).getCostFor(hullSize);
            int target = TARGET_OP.get(hullSize);
            if (cost < target) {
                compensation += target - cost;
            }
        }
        compensation *= getCompensationMult();
        return (int)  Math.floor(compensation);
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

    @Override
    public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return (ship != null && !isForModSpec);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width,
            boolean isForModSpec) {
        super.addPostDescriptionSection(tooltip, hullSize, ship, width, isForModSpec);
        tooltip.addSectionHeading("Current Bonus", Alignment.MID, 6f);
        
        int totalBonus = getCompensation(hullSize, ship.getVariant());
        tooltip.addPara("This ship currently has %s ordnance points added due to special modifications", 6f, Misc.getHighlightColor(), "" +totalBonus);
        tooltip.addSpacer(6f);
        tooltip.setBulletedListMode(BaseIntelPlugin.BULLET);
        Color[] hls = new Color[2];
        hls[0] = Misc.getGrayColor();
        
        for (String hullmodId : ship.getVariant().getSMods()) {
            HullModSpecAPI hullmod = Global.getSettings().getHullModSpec(hullmodId);
            int bonus = 0;
            int cost = hullmod.getCostFor(hullSize);
            int target = TARGET_OP.get(hullSize);
            if (cost < target) {
                bonus = target - cost;
                hls[1] = Misc.getHighlightColor();
            } else {
                hls[1] = Misc.getGrayColor();
            }
            tooltip.addPara(hullmod.getDisplayName() + " %s: %s", 
                    0, 
                    hls, 
                    "(" + hullmod.getCostFor(hullSize) + ")", 
                    "+" + bonus);
        }

        tooltip.setBulletedListMode(BaseIntelPlugin.INDENT + BaseIntelPlugin.INDENT);
        tooltip.addPara("Effectiveness: %s", 
                0, 
                Misc.getHighlightColor(), 
                String.format("x%.2f", getCompensationMult()));

        tooltip.setBulletedListMode(null);
    }
}
