package com.asb.analytics.logs;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class will print the log accross the all project through static methods.
 *
 * Created by Claudio Paolicelli
 */
public class Logger {

    private static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");

    private final String dateTime;

    private final boolean active;

    //-------- CONSTRUCTORS

    /**
     * Public static constructor.
     *
     * @return instance of {@link Logger}
     */
    public static Logger log(boolean active) {
        return new Logger(false, active);
    }

    /**
     * Public static constructor.
     *
     * @return instance of {@link Logger}
     */
    public static Logger log() {
        return new Logger(false, true);
    }


    /**
     * Public static constructor.
     * Set dateTime to empty string for log with no dateTime.
     *
     * @return instance of {@link Logger}
     */
    public static Logger planeLog() {
        return new Logger(false, true);
    }

    /**
     * Public static constructor.
     * Set dateTime to empty string for log with no dateTime.
     *
     * @return instance of {@link Logger}
     */
    public static Logger planeLog(boolean active) {
        return new Logger(false, active);
    }

    /*
     * Private constructor. Create new instance of the class and set the dateTime.
     */
    private Logger(boolean plane, boolean active) {

        this.active = active;

        if (plane) this.dateTime = "";
        else this.dateTime = "[" + dateTimeFormat.format(new Date()) + "]";
    }

    /*
     * Colour description
     */
    private void coloursDescription() {
        System.out.println("\u25A0 Info log");
        System.out.println(Color.GREEN + "\u25A0 Operation success log");
        System.out.println(Color.YELLOW + "\u25A0 Warning/Error log");
        System.out.println(Color.RED + "\u25A0 Fatal error log\n\n");
        System.out.println(Color.RESET);
    }

    public void mainStart() {

        coloursDescription();

        System.out.println(String.format("%s Starting BetStore software...", this.dateTime));
    }

    public void fatalError(String message) {

        if (!active) return;

        System.out.println(Color.RED + String.format("%s FATAL ERROR ACCURRED", this.dateTime));
        System.out.println(String.format("%s", message) + Color.RESET);
    }

    public void info(String message) {

        if (!active) return;

        System.out.println(String.format("%s %s", this.dateTime, message));
    }

    public void error(String message) {

        if (!active) return;

        System.out.println(Color.YELLOW + String.format("%s %s", this.dateTime, message) + Color.RESET);
    }
}
