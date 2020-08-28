package shared.tools;

import org.powerbot.script.rt4.ClientContext;

import java.awt.*;

/**
 * GUI helper coordinates & tools
 */
public class GuiHelper {
    private static final int baseX = 547;
    private static final int baseY = 205;
    private static final int widthX = 190;
    private static final int widthY = 261;

    private static final int dialogX = 2;
    private static final int dialogY = 345 - 135;
    private static final int dialogWidth = 514;
    private static final int dialogHeight = 128;

    private static final Color baseColor = new Color(0, 0, 0, 120);
    private static final Color baseColorDark = new Color(0, 0, 0);

    private static final Color textColorWhite = Color.WHITE;
    private static final Color textColorImportant = Color.GREEN;
    private static final Color textColorInformation = Color.CYAN;
    private static final Color textColorWarning = Color.YELLOW;
    private static final Color textColorDanger = Color.RED;

    public static String getReadableRuntime(Long runtimeMillis) {
        return runtimeMillis / 1000 / 60 + "m " + runtimeMillis / 1000 % 60 + "s";
    }

    public static Long getRuntimeMinutes(Long runtime) {
        return runtime / 1000 / 60;
    }

    public static int getBaseX() {
        return baseX;
    }

    public static int getBaseY() {
        return baseY;
    }

    public static int getWidthX() {
        return widthX;
    }

    public static int getWidthY() {
        return widthY;
    }

    public static int getDialogX() {
        return dialogX;
    }

    public static int getDialogY() {
        return dialogY;
    }

    public static int getDialogWidth() {
        return dialogWidth;
    }

    public static int getDialogHeight() {
        return dialogHeight;
    }

    public static int getStartX() {
        return baseX + 5;
    }

    public static int getDialogStartX() {
        return dialogX + 5;
    }

    public static int getDialogMiddleX() {
        return (int)(dialogX + .65 * dialogWidth);
    }

    public static int getStartY(int line) {
        return baseY + 25 * line;
    }

    public static int getDialogStartY(int line) {
        return dialogY + 20 * line;
    }

    public static Color getBaseColor() {
        return baseColor;
    }

    public static Color getBaseColorDark() {
        return baseColorDark;
    }

    public static Color getTextColorWhite() {
        return textColorWhite;
    }

    public static Color getTextColorImportant() {
        return textColorImportant;
    }

    public static Color getTextColorInformation() {
        return textColorInformation;
    }

    public static Color getTextColorWarning() {
        return textColorWarning;
    }

    public static Color getTextColorDanger() {
        return textColorDanger;
    }

    /**
     * Return the XP per hour based on skill, startXP, and runtime
     * @param ctx Context
     * @param combatSkill SKill ID
     * @param startXP Script start XP
     * @param runtime runtime from getRuntime()
     * @return xp per hour
     */
    public static double xpPerHour(ClientContext ctx, int combatSkill, double startXP, long runtime) {
        // Pull XP for combat style
        int currentXP = ctx.skills.experience(combatSkill);
        double xpGained = currentXP - startXP;
        return 3600000 / runtime * xpGained;
    }
}
