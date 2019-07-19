package local.media;



/** Interface for classes that start a media application (e.g. audio or video fullduplex streaming). */
public interface MediaApp
{
    /** Starts media application */
    boolean startApp();

    /** Stops media application */
    boolean stopApp();

}