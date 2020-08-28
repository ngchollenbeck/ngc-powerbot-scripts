package scripts.agility_canifs_rooftop.phases;

import org.powerbot.script.rt4.ClientContext;
import scripts.agility_canifs_rooftop.actions.AgilityAction;
import shared.constants.GameObjects;
import shared.templates.StructuredAction;
import shared.templates.StructuredPhase;
import shared.tools.CommonAreas;

public class RunRooftopsPhase extends StructuredPhase {

    public RunRooftopsPhase(ClientContext ctx, String name) {
        super(ctx, name);

//        StructuredAction climbTree = new AgilityAction(ctx, "Climb Tree", "Climb", GameObjects.CANIFS_TALL_TREE_14843, CommonAreas.canifs_start());
        StructuredAction jumpGapOne = new AgilityAction(ctx, "Gap One", "Jump", GameObjects.CANIFS_GAP_1_14844, CommonAreas.canifs_roof_1());
        StructuredAction jumpGapTwo = new AgilityAction(ctx, "Gap Two", "Jump", GameObjects.CANIFS_GAP_2_14845, CommonAreas.canifs_roof_2());
        StructuredAction jumpGapThree = new AgilityAction(ctx, "Gap Three", "Jump", GameObjects.CANIFS_GAP_3_14848, CommonAreas.canifs_roof_3());
        StructuredAction jumpGapFour = new AgilityAction(ctx, "Gap Four", "Jump", GameObjects.CANIFS_GAP_4_14846, CommonAreas.canifs_roof_4());
        StructuredAction jumpGapFive = new AgilityAction(ctx, "Gap Five", "Vault", GameObjects.CANIFS_GAP_5_14894, CommonAreas.canifs_roof_5());
        StructuredAction jumpGapSix = new AgilityAction(ctx, "Gap Six", "Jump", GameObjects.CANIFS_GAP_6_14847, CommonAreas.canifs_roof_6());
        StructuredAction jumpGapSeven = new AgilityAction(ctx, "Gap Seven", "Jump", GameObjects.CANIFS_GAP_7_14897, CommonAreas.canifs_roof_7());

//        climbTree.setNextAction(jumpGapOne);
        jumpGapOne.setNextAction(jumpGapTwo);
        jumpGapTwo.setNextAction(jumpGapThree);
        jumpGapThree.setNextAction(jumpGapFour);
        jumpGapFour.setNextAction(jumpGapFive);
        jumpGapFive.setNextAction(jumpGapSix);
        jumpGapSix.setNextAction(jumpGapSeven);
        jumpGapSeven.setNextAction(jumpGapOne);

        this.setInitialAction(jumpGapOne);
    }

    @Override
    public boolean moveToNextPhase() {
        return false;
    }
}
