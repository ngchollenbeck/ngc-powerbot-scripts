package scripts.mining_motherload;

import org.powerbot.script.*;
import org.powerbot.script.rt4.*;
import org.powerbot.script.rt4.ClientContext;
import shared.action_config.ScriptConfig;
import shared.constants.Items;
import shared.tools.*;

import java.awt.*;
import java.util.concurrent.Callable;

import static java.lang.Integer.parseInt;
import static org.powerbot.script.Condition.sleep;

/*
TODO:
- Improve pathing for running back to hopper
- Allow for breaking landslides to reach nearest ore vein
- Build routines for NE and SW corners
 */

@Script.Manifest(name = "Mining - Motherload", description = "Mines pay dirt, fixes water wheel, and banks ores", properties = "client=4; topic=051515; author=Bowman")
public class MotherloadMiner extends PollingScript<ClientContext> implements MessageListener, PaintListener {

    // Config
    private final ScriptConfig scriptConfig = new ScriptConfig(ctx);
    private Tile veinTile;
    private Tile lastVeinTile;
    private boolean isPlayerMining;
    private int payDirtSackCount;

    private int lastVeinTileX;
    private int lastVeinTileY;

    private final int[] droppables = new int[]{Items.UNCUT_DIAMOND_1617, Items.UNCUT_RUBY_1619, Items.UNCUT_EMERALD_1621, Items.UNCUT_SAPPHIRE_1623};
    private final int[] ores = new int[]{Items.GOLD_ORE_444, Items.ADAMANTITE_ORE_449, Items.COAL_453, Items.MITHRIL_ORE_447, Items.RUNITE_ORE_451};

    private final Tile[] pathFromNWCorner = {new Tile(3728, 5685, 0), new Tile(3730, 5684, 0), new Tile(3732, 5680, 0), new Tile(3736, 5678, 0), new Tile(3738, 5675, 0), new Tile(3741, 5673, 0), new Tile(3744, 5673, 0), new Tile(3747, 5673, 0), new Tile(3750, 5672, 0), new Tile(3750, 5669, 0), new Tile(3750, 5666, 0)};
    private final Tile[] pathToNWCorner = {new Tile(3750, 5666, 0), new Tile(3750, 5669, 0), new Tile(3750, 5672, 0), new Tile(3747, 5673, 0), new Tile(3744, 5673, 0), new Tile(3741, 5673, 0), new Tile(3738, 5675, 0), new Tile(3736, 5678, 0), new Tile(3733, 5680, 0), new Tile(3730, 5683, 0)};

    private int actionsToLevel;

    private final Tile nwRockfallTile1 = new Tile(3731, 5683, 0);
    private final Tile nwRockfallTile2 = new Tile(3733, 5680, 0);
    private final Tile nwRockfallTile3 = new Tile(3727, 5683, 0);
    private final Tile nwRockfallTile4 = new Tile(3745, 5689, 0);

    private enum MiningArea {
        NW,
        SW
    }

    //region Antiban
    private long nextBreak;
    //endregion

    //region Phase
    private enum Phase {
        Start, Mining, ToHopper, ToOreVeins, Processing, Banking
    }

    private Phase currentPhase;
    //endregion

    //region start
    @Override
    public void start() {
        this.scriptConfig.setPhase("Mining");

        this.setOreVein();
        this.isPlayerMining = ctx.players.local().animation() == 6752;

        this.veinTile = new Tile(1, 1, 0);
        this.lastVeinTile = new Tile(1, 1, 0);
        this.lastVeinTileX = ctx.players.local().tile().x();
        this.lastVeinTileY = ctx.players.local().tile().y();

        this.updatePayDirtCount();

        nextBreak = (long) AntibanTools.getRandomInRange(4, 15) * 60000;

        if (ctx.inventory.isFull() && ctx.inventory.select().id(ores).count() == 0) {
            this.currentPhase = Phase.ToHopper;
        } else if (isHopperInView()) {
            this.currentPhase = Phase.Processing;
        } else if (ctx.inventory.select().id(ores).count() > 0)
            this.currentPhase = Phase.Banking;
        else
            this.currentPhase = Phase.Mining;
    }
    //endregion

    //region messaged
    @Override
    public void messaged(MessageEvent e) {
        String msg = e.text();

        if (msg.contains("swing your pick at the rock.")) {
            this.isPlayerMining = true;
        }

        this.updateXPRates();
    }
    //endregion

    //region repaint
    @Override
    public void repaint(Graphics g) {
        if (!ctx.controller.isSuspended()) {
            this.scriptConfig.paint(g);

            g.drawString("Phase  : " + (this.scriptConfig.getPhase()), this.scriptConfig.paintLineX(), this.scriptConfig.paintLineY(1));
            g.drawString("Runtime: " + GuiHelper.getReadableRuntime(getRuntime()), this.scriptConfig.paintLineX(), this.scriptConfig.paintLineY(2));
            g.drawString("Level  : " + ctx.skills.realLevel(Constants.SKILLS_MINING), this.scriptConfig.paintLineX(), this.scriptConfig.paintLineY(4));
            g.drawString("To Lvl : " + actionsToLevel, this.scriptConfig.paintLineX(), this.scriptConfig.paintLineY(5));
        }
    }
    //endregion

    //region poll
    @Override
    public void poll() {
        // Pre Phase Check Action
        this.scriptConfig.prePollAction();

        // Antiban Break
        if (this.activateAntiban()) {
            this.executeAntiban();
        } else {
            executePhase();
        }
    }
    //endregion

    //region Phase
    private Phase checkNextPhase() {
        switch (this.currentPhase) {
            case Start:
                return Phase.Mining;

            case Mining:
                // Inventory full and no gems
                if (ctx.inventory.isFull() && !ctx.inventory.select().select(new Filter<Item>() {
                    @Override
                    public boolean accept(Item item) {
                        return item.name().contains("Uncut");
                    }
                }).poll().valid()) {
                    this.isPlayerMining = false;
                    if (GaussianTools.takeActionLikely()) {
                        ctx.camera.pitch(Random.nextInt(50, 90));
                        ctx.camera.angle(Random.nextInt(150, 220));
                    }
                    return Phase.ToHopper;
                }
                break;

            case ToHopper:
                // Invent
                if (this.isHopperInView()) {
                    return Phase.Processing;
                }
                break;

            case Processing:
                this.updatePayDirtCount();
                if (this.payDirtSackCount > 50 && !ctx.inventory.isFull()) {
                    return Phase.Banking;
                }

                if (!ctx.inventory.isFull() && ctx.inventory.select().id(ores).count() == 0) {
                    return Phase.ToOreVeins;
                }
                break;

            case Banking:
                this.updatePayDirtCount();
                if (this.payDirtSackCount == 0 && ctx.inventory.select().id(ores).count() == 0) {
                    return Phase.ToOreVeins;
                }
                break;

            case ToOreVeins:
                if (CommonAreas.motherload_nw().contains(ctx.players.local()) || isPlayerMining) {
                    if (GaussianTools.takeActionLikely()) ctx.camera.pitch(Random.nextInt(40, 80));
                    return Phase.Mining;
                }
                break;
        }

        return this.currentPhase;
    }

    private void executePhase() {
        this.currentPhase = checkNextPhase();

        switch (this.currentPhase) {
            case Start:
                break;

            case Mining:
                this.scriptConfig.setPhase("Mining");

                if (ctx.inventory.select().id(droppables).count() > 0) {
                    CommonActions.openTab(ctx, Game.Tab.INVENTORY);
                    CommonActions.dropItem(ctx, ctx.inventory.select().id(droppables).first().poll().id());
                    this.isPlayerMining = false;
                }

                if (!ctx.inventory.isFull()) {
                    if (!isOreVeinValid()) {
                        Condition.wait(new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                return ctx.players.local().animation() != -1;
                            }
                        }, 50, 20);

                        this.isPlayerMining = ctx.players.local().animation() != -1;
                        this.setOreVein();
                    }

                    if (!this.isPlayerMining) {
                        this.executeMinePayDirt();
                    }
                }
                break;

            case ToHopper:
                this.scriptConfig.setPhase("To Hopper");

                if (ctx.inventory.isFull()) {
                    this.walkToHopper();
                }
                break;

            case Processing:
                // Deposit pay dirt
                this.scriptConfig.setPhase("Hopper");

                if (isHopperInView() && ctx.inventory.isFull()) {
                    this.loadHopper();

                    if (this.isStrutBroken()) {
                        this.repairBrokenStrut();

                        Condition.wait(new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                return !isStrutBroken();
                            }
                        }, Random.nextInt(250, 400), 20);
                    }

                }
                break;


            case Banking:
                this.scriptConfig.setPhase("Bank");
                // Loot any powermined ores
                GroundItem oreOnGround = ctx.groundItems.select().id(ores).viewable().poll();
                if (oreOnGround.valid() && !ctx.inventory.isFull()) {
                    oreOnGround.interact("Take");
                    Condition.wait(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            return !oreOnGround.valid();
                        }
                    }, Random.nextInt(50, 100), 10);
                } else
                    // withdraw from sack
                    if (ctx.inventory.select().id(ores).count() == 0) {
                        if (payDirtSackCount > 0)
                            emptySack();
                    } else {
                        // deposit into deposit box
                        System.out.println("Banking");
                        depositInventory();
                        sleep();

                    }
                break;

            case ToOreVeins:
                this.scriptConfig.setPhase("To Ore");

                if (!ctx.inventory.isFull()) {
                    this.walkToOreVeins();
                    sleep();
                }
                break;
        }
    }
    //endregion

    //region Antiban
    private void executeAntiban() {
        if (GaussianTools.takeActionNormal()) {
            this.scriptConfig.setPhase("Antiban");
            this.scriptConfig.setStep("Wait");
            AntibanTools.runMiningAntiban(ctx);
        }
        this.nextBreak = getRuntime() + (AntibanTools.getRandomInRange(4, 17) * 60000);
    }

    private boolean activateAntiban() {
        return (getRuntime() > nextBreak);
    }
    //endregion

    //region Mine Pay-Dirt
    private void executeMinePayDirt() {

        GameObject vein = ctx.objects.select().at(this.veinTile).first().poll();

        if (vein.valid()) {

            this.turnCameraToOre(vein.tile());

            // Is this vein visible?
            if (vein.inViewport() && vein.tile().distanceTo(ctx.players.local()) < 6) {

                // Mine the vein
                Point tileCenter = vein.tile().matrix(ctx).centerPoint();

                if (tileCenter.x > -1) {
                    Point ovp = new Point(tileCenter.x + Random.nextInt(-10, 10), tileCenter.y + Random.nextInt(-70, 0));
                    ctx.input.move(ovp);
                    sleep(Random.nextInt(100, 400));
                    String action = ctx.menu.items()[0].split(" ")[0];

                    if (action.equals("Mine")) {
                        ctx.input.click(ovp, true);

                        Condition.wait(new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                return ctx.players.local().inMotion();
                            }
                        }, Random.nextInt(375, 600), 20);

                        Condition.wait(new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                return vein.tile().distanceTo(ctx.players.local()) == 1 || !ctx.players.local().inMotion();
                            }
                        }, Random.nextInt(400, 800), 20);

                        if (isPlayerMining) {

                            if (!GaussianTools.takeActionNormal()) {
                                AntibanTools.moveMouseOffScreen(ctx, true);
                            }

                            // Wait for player to stop mining
                            Condition.wait(new Callable<Boolean>() {
                                @Override
                                public Boolean call() throws Exception {
                                    return !isOreVeinValid() || !isPlayerMining || ctx.inventory.isFull(); // not valid or couldn't reach it due to landslide

                                }
                            }, Random.nextInt(1000, 2000), 65);
                        }
                    }
                }
            } else {
                // Vein not visible, walk to it
                ctx.movement.step(vein);

                // Check for rockfall deposit blocking path
                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return vein.tile().distanceTo(ctx.players.local()) == 1 || !ctx.players.local().inMotion();
                    }
                }, Random.nextInt(100, 200), 20);
            }
        }
    }

    private void setOreVein() {
        GameObject vein = ctx.objects.select().name("Ore vein").select(new Filter<GameObject>() {
            @Override
            public boolean accept(GameObject gameObject) {
                return objectIsValid(gameObject, false) &&
                        (gameObject.tile().x() < 3726 || gameObject.tile().x() > 3731);
            }
        }).nearest().poll();

        if (vein.inViewport()) { // Prevents stepping to vein from disrupting the camera
            this.lastVeinTile = this.veinTile;
            if (this.veinTile != null) {
                this.lastVeinTileX = this.veinTile.x();
                this.lastVeinTileY = this.veinTile.y();
            }
        }
        this.veinTile = vein.tile();
    }

    private boolean isOreVeinValid() {
        if (veinTile == null) return false;

        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return ctx.players.local().animation() == -1;
            }
        }, 400, 10);

        return (ctx.objects.select().at(veinTile).first().poll().name().equals("Ore vein") && veinTile.distanceTo(ctx.players.local()) == 1);
    }
    //endregion

    //region Hopper & Sack
    private void loadHopper() {
        GameObject hopper = ctx.objects.select().name("Hopper").nearest().poll();
        hopper.interact("Deposit");
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return !ctx.inventory.isFull();
            }
        }, Random.nextInt(250, 600), 20);

        sleep(Random.nextInt(250, 1250));

        //this.lastVeinTile = new Tile(1, 1, 0);

        if (ctx.inventory.select().id(Items.HAMMER_2347).count() == 0) {
            var box = ctx.objects.select().name("Crate").nearest().poll();

            if (box.valid()) {
                box.interact("Search");

                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return ctx.inventory.select().id(Items.HAMMER_2347).count() == 1;
                    }
                }, Random.nextInt(800, 1200), 10);
            }
        }
    }

    private boolean isHopperInView() {
        GameObject hopper = ctx.objects.select().name("Hopper").nearest().poll();

        return hopper.inViewport() && hopper.tile().distanceTo(ctx.players.local()) < 10;
    }

    private boolean isStrutBroken() {
        GameObject strut = ctx.objects.select().name("Broken strut").nearest().poll();

        return strut.valid() && strut.tile().y() > 5665 && ctx.inventory.select().id(Items.HAMMER_2347).poll().valid();
    }

    private void repairBrokenStrut() {
        GameObject strut = ctx.objects.select().name("Broken strut").nearest().poll();

        if (!strut.inViewport()) {
            ctx.camera.turnTo(strut);

            Condition.wait(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return strut.inViewport();
                }
            }, Random.nextInt(300, 400), 10);
        }

        strut.interact("Hammer");
    }

    private void emptySack() {
        GameObject sack = ctx.objects.select().name("Sack").nearest().poll();

        if (!sack.inViewport()) {
            ctx.movement.step(sack);

            Condition.wait(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return sack.inViewport() && sack.tile().distanceTo(ctx.players.local()) < 3;
                }
            }, Random.nextInt(150, 250), 30);

            this.emptySack();
        } else {
            sack.interact("Search");
            Condition.wait(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return ctx.inventory.select().id(ores).count() > 0;
                }
            }, Random.nextInt(250, 350), 40);
        }
    }

    private void updatePayDirtCount() {
        this.payDirtSackCount = parseInt(ctx.widgets.component(382, 4, 2).text());
    }

    private void depositInventory() {

        if (CommonActions.openBank(ctx)) {
            if (CommonActions.depositAllExcept(ctx, new int[]{Items.HAMMER_2347})) {
                CommonActions.closeBank(ctx);
            }
        }
    }
    //endregion

    //region Travel
    private void walkToHopper() {
        // Start Walking
        travelPath(pathFromNWCorner);
    }

    private void walkToOreVeins() {
        pathToNWCorner[pathToNWCorner.length - 1] = CommonAreas.motherload_nw().getRandomTile();
        this.travelPath(pathToNWCorner);
    }

    private void travelPath(Tile[] path) {
        Tile nextTile = ctx.movement.newTilePath(path).next();
        GameObject rockFall = getRockfallsOnNwPath();

        if (rockFall.valid() && !CommonAreas.motherload_main().contains(ctx.players.local())) {
            if (!rockFall.inViewport()) {
                if (rockFall.tile().distanceTo(ctx.players.local()) > 3) {
                    ctx.movement.step(rockFall);
                    Condition.wait(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            return rockFall.inViewport();
                        }
                    }, Random.nextInt(150, 250), 20);
                } else
                    ctx.camera.turnTo(rockFall);
            } else {
                rockFall.interact("Mine");

                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return !rockFall.valid();
                    }
                }, Random.nextInt(200, 321), 20);
            }
        } else if (nextTile != null && nextTile.matrix(ctx).reachable() && nextTile.distanceTo(ctx.players.local()) != 0) {
            ctx.movement.newTilePath(path).traverse();
            Condition.wait(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return rockFall.inViewport() || !ctx.players.local().inMotion() || isHopperInView();
                }
            }, Random.nextInt(250, 300), 10);
        }
    }

    private GameObject getRockfallsOnNwPath() {
        return ctx.objects.select().name("Rockfall").select(new Filter<GameObject>() {
            @Override
            public boolean accept(GameObject gameObject) {
                Tile t = gameObject.tile();

                return (t.distanceTo(nwRockfallTile1) == 0 ||
                        t.distanceTo(nwRockfallTile2) == 0) &&
                        objectIsValid(gameObject, false);
            }
        }).nearest().poll();
    }

    private void turnCameraToOre(Tile oreTile) {

        int xOffset = Math.abs((this.lastVeinTileX - oreTile.x()));
        int yOffset = Math.abs((this.lastVeinTileY - oreTile.y()));
        int validOffset = 2;

//        if (this.lastVeinTile.x() == 1 && this.lastVeinTile.y() == 1) {
//            ctx.camera.turnTo(oreTile);
//        }

        if (xOffset > validOffset && yOffset <= validOffset) {
            if (oreTile.x() > lastVeinTile.x()) {
                ctx.camera.angle('e');
            } else {
                ctx.camera.angle('w');
            }
        }

        if (xOffset <= validOffset && yOffset > validOffset) {
            if (oreTile.y() > lastVeinTile.y()) {
                ctx.camera.angle('n');
            } else {
                ctx.camera.angle('s');
            }

        }
    }

    //endregion

    //region Helpers
    private boolean objectIsValid(GameObject gameObject, boolean ignorePlayers) {
        Tile westTile = new Tile(gameObject.tile().x() + 1, gameObject.tile().y(), gameObject.tile().floor());
        Tile eastTile = new Tile(gameObject.tile().x() - 1, gameObject.tile().y(), gameObject.tile().floor());
        Tile northTile = new Tile(gameObject.tile().x(), gameObject.tile().y() + 1, gameObject.tile().floor());
        Tile southTile = new Tile(gameObject.tile().x(), gameObject.tile().y() - 1, gameObject.tile().floor());

        // Reachable and not occupied
        return (westTile.matrix(ctx).reachable() && (ignorePlayers || !ctx.players.select().at(westTile).poll().valid())) ||
                (eastTile.matrix(ctx).reachable() && (ignorePlayers || !ctx.players.at(eastTile).poll().valid())) ||
                (northTile.matrix(ctx).reachable() && (ignorePlayers || !ctx.players.at(northTile).poll().valid())) ||
                (southTile.matrix(ctx).reachable() && (ignorePlayers || !ctx.players.at(southTile).poll().valid()));
    }

    private void updateXPRates() {
        int currentXP = ctx.skills.experience(Constants.SKILLS_MINING);
        int nextLevelXP = ctx.skills.experienceAt(ctx.skills.realLevel(Constants.SKILLS_MINING) + 1);
        this.actionsToLevel = (nextLevelXP - currentXP) / 60;
    }
    //endregion
}