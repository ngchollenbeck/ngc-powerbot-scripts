package scripts.cmb_nechryaels;

import shared.action_config.CombatConfig;
import shared.templates.AbstractAction;
import shared.tools.GaussianTools;
import org.powerbot.script.Condition;
import org.powerbot.script.Filter;
import org.powerbot.script.Random;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Npc;

import java.util.concurrent.Callable;

import static org.powerbot.script.Condition.sleep;

public class NechryaelCombatAction extends AbstractAction<ClientContext> {
    private CombatConfig config;

    public NechryaelCombatAction(ClientContext ctx, String status, CombatConfig _config) {
        super(ctx, status);
        config = _config;
    }

    @Override
    public boolean activate() {

        boolean hasMinHealth = ctx.combat.healthPercent() >= config.getMinHealthPercent();
        boolean interacting = (ctx.players.local().interacting().valid() && ctx.players.local().interacting().name().equals(config.getNpcName()))
                || ctx.npcs.select().select(new Filter<Npc>() {
            @Override
            public boolean accept(Npc npc) {
                return npc.name().equals(config.getNpcName()) && npc.tile().distanceTo(ctx.players.local()) == 1 && npc.interacting().name().equalsIgnoreCase(ctx.players.local().name());
            }
        }).poll().valid();

        boolean validNpcNearby =
                ctx.npcs.select().select(new Filter<Npc>() {
                    @Override
                    public boolean accept(Npc npc) {
                        return npc.name().equals(config.getNpcName()) && validNpcForCombat(npc);
                    }
                }).nearest().peek().valid();

        // printConditions(noAlchables, hasMinHealth, !interacting, !lootNearby, validNpcNearby, (config.getSafeTile() == null || config.getSafeTile().distanceTo(ctx.players.local()) == 0));


        return hasMinHealth && !interacting && validNpcNearby && (config.getSafeTile() == null || config.getSafeTile().distanceTo(ctx.players.local()) == 0);
    }

    @Override
    public void execute() {
        // Add a touch of AFK
        sleep(Math.abs(GaussianTools.getRandomGaussian(0, 500)));

        // Check for npc interacting with player
        Npc target = ctx.npcs.select().select(new Filter<Npc>() {
            @Override
            public boolean accept(Npc npc) {
                return npc.interacting().valid() && npc.name().equals(config.getNpcName()) && npc.interacting().name().equalsIgnoreCase(ctx.players.local().name()) && npc.healthPercent() > 5;
            }
        }).poll();


        if( !target.valid() ) {
            // None Found. Find nearest valid target
            target = ctx.npcs.select().select(new Filter<Npc>() {
                @Override
                public boolean accept(Npc npc) {
                    return (npc.name().equals(config.getNpcName()) && validNpcForCombat(npc));
                }
            }).nearest().poll();
        }

        Npc npc = target;


        // Target Npc
        if( npc.inViewport() && (!npc.interacting().valid() || npc.interacting().name().equals(ctx.players.local().name())) ) {
            if( npc.interact("Attack", npc.name()) ) {
                // Chill for a sec
                sleep(500);

                // Wait for the first hit
                waitForCombat();

                // Triple check we've got a target
                if( ctx.players.local().interacting().valid() ) {
                    if( config.getNpcDeathAnimation() > 0 ) {
                        // Wait for drop like a good human
                        Condition.wait(new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                return !npc.valid() || validNpcForCombat(npc);
                            }
                        }, Random.nextInt(250, 500), 60);
                    }
                }
            }
        } else {
            if( config.getSafeTile() == null ) {
                ctx.camera.turnTo(npc);

                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return npc.inViewport();
                    }
                }, 200, 20);

                if( !npc.inViewport() ) {
                    ctx.movement.step(npc);
                    sleep(Random.nextInt(500, 750)); //Allows target switching if needed
                }
            }
        }

    }

    private boolean validNpcForCombat(Npc npc) {
        boolean approved = (npc.healthPercent() > 10) &&
                npc.tile().matrix(ctx).reachable() &&
                (config.isMultiCombatArea() || !npc.interacting().valid())
                && (config.getMinDistanceToTarget() <= 0 || npc.tile().distanceTo(ctx.players.local()) >= config.getMinDistanceToTarget());
        return approved;
        //        return (config.isMultiCombatArea() ||  || (config.getSafeTile() != null && config.getSafeTile().distanceTo(ctx.players.local()) == 0))) && npc.healthPercent() > 1;
    }

    private void waitForCombat() {
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return ctx.players.local().interacting().valid();
            }
        }, 200, 10);
    }

    private void printConditions(boolean alchs, boolean minHealth, boolean noInteracting, boolean noLootNearby, boolean validNPC, boolean safetile) {
        System.out.println(("---------Combat Checks-----------"));
        System.out.println("Alch: " + alchs);
        System.out.println("Health: " + minHealth);
        System.out.println("Interacting: " + noInteracting);
        System.out.println("Loot: " + noLootNearby);
        System.out.println("Target: " + validNPC);
        System.out.println("Safetile: " + safetile);
    }

}
