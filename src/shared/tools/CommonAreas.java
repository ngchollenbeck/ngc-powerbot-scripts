package shared.tools;

import org.powerbot.script.Area;
import org.powerbot.script.Tile;

/**
 * Area values for common places
 */
public class CommonAreas {
    public static Area getBarbFishingArea() {
        return new Area(new Tile(3102, 3436), new Tile(3112, 3418));
    }

    public static Area getBarbFishingAreaSm() {
        return new Area(new Tile(3102, 3435), new Tile(3109, 3432));
    }

    public static Area getGeArea() {
        return new Area(new Tile(3169, 3483), new Tile(3158, 3488));
    }

    public static Area getGeAreaEast() {
        return new Area(new Tile(3167, 3487), new Tile(3170, 3491));
    }

    public static Area getHillyCabinAreaOutside() {
        return new Area(new Tile(3119, 3449), new Tile(3113, 3446));
    }

    public static Area getHillyCabinAreaInside() {
        return new Area(new Tile(3117, 3450), new Tile(3113, 3453));
    }

    public static Area getHillyLair() {
        return new Area(new Tile(3111, 9831), new Tile(3116, 9834));
    }

    public static Area getDraynorBank() {
        return new Area(new Tile(3092, 3247), new Tile(3095, 3242));
    }

    public static Area getKaramjaFishingDock() {
        return new Area(new Tile(2915, 3181), new Tile(2926, 3172));
    }

    public static Area getPortSarimDock() {
        return new Area(new Tile(3052, 3246), new Tile(3019, 3208));
    }

    public static Area getKaramjaShipDock() {
        return new Area(new Tile(2946, 3146), new Tile(2958, 3145));
    }

    public static Area getPortSarimDepositBox() {
        return new Area(new Tile(3040, 3233), new Tile(3046, 3238));
    }

    public static Area getVarrockOakTrees() {return new Area(new Tile(3189, 3465), new Tile(3195, 3456));}

    public static Area getVarrockFountain() {return new Area(new Tile(3190, 3472), new Tile(3194, 3468));}

    public static Area getPortSarimWillows() {return new Area(new Tile(3056, 3256), new Tile(3063, 3251));}

    public static Area getAuburyRuneShop() {return new Area(new Tile(3252, 3403), new Tile(3254, 3401));}

    public static Area getVarrockBankEast() {return new Area(new Tile(3250, 3420), new Tile(3257, 3424));}

    public static Area getVarrockBankWest() {return new Area(new Tile(3184, 3444), new Tile(3181, 3439));}

    public static Area getVarrockWizards() {return new Area(new Tile(3221, 3377), new Tile(3233, 3263));}

    public static Area getVarrockEssMinerBounds() {return new Area(new Tile(3250, 3428), new Tile(3263, 3395));}

    public static Area lumbridgeCastleBank() {return new Area(new Tile(3210, 3220), new Tile(3205, 3215));}

    public static Area edgevilleBankNorth() {return new Area(new Tile(3092, 3494), new Tile(3097, 3497));}

    public static Area edgevilleBankSouth() {return new Area(new Tile(3091, 3492), new Tile(3094, 3488));}

    public static Area edgevilleSmelter() {return new Area(new Tile(3108, 3500), new Tile(3105, 3497));}

    public static Area corsairBank() {return new Area(new Tile(2572, 2862), new Tile(2568, 2860));}

    public static Area corsairCoveYews() {return new Area(new Tile(2482, 2885), new Tile(2468, 2895));}

    public static Area fishingGuildPrimaryDock() {return new Area(new Tile(2593, 3419), new Tile(2605, 3426));}

    public static Area fishingGuildBank() {return new Area(new Tile(2588, 3418), new Tile(2591, 3414));}

    public static Area ogressSafeZone() {return new Area(new Tile(2014, 9004), new Tile(2012, 9002));}


    public static Area canifs_start() {return new Area(new Tile(3502, 3490, 0), new Tile(3512, 3488, 0));}

    public static Area canifs_roof_1() {return new Area(new Tile(3505, 3491, 2), new Tile(3509, 3498, 2));}
    public static Area canifs_roof_2() {return new Area(new Tile(3503, 3504, 2), new Tile(3497, 3506, 2));}
    public static Area canifs_roof_3() {return new Area(new Tile(3493, 3505, 2), new Tile(3487, 3499, 2));}
    public static Area canifs_roof_4() {return new Area(new Tile(3480, 3500, 3), new Tile(3475, 3492, 3));}
    public static Area canifs_roof_5() {return new Area(new Tile(3477, 3487, 2), new Tile(3480, 3482, 2));}
    public static Area canifs_roof_6() {return new Area(new Tile(3488, 3477, 3), new Tile(3504, 3471, 3));}
    public static Area canifs_roof_7() {return new Area(new Tile(3508, 3476, 2), new Tile(3512, 3483, 2));}

    public static Area canifs_fall_1() {return new Area(new Tile(3481, 3496, 0), new Tile(3484, 3502, 0));}
    public static Area canifs_fall_2() {return new Area(new Tile(3507, 3474, 0), new Tile(3505, 3480, 0));}


    public static Area motherload_nw() {return new Area(new Tile(3733, 5690, 0), new Tile(3720, 5684, 0));}
    public static Area motherload_main() {return new Area(new Tile(3737, 5677, 0), new Tile(3759, 5658, 0));}

}
